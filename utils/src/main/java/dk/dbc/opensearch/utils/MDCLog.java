/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-utils
 *
 * opensearch-utils is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-utils is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;

/**
 * This is somewhat along the lines of
 * {@link MDC#putCloseable(java.lang.String, java.lang.String)} however, with
 * the MDC version you need a closable for each property set leaving bulky
 * code like this
 * <pre>
 *        try (MDC.MDCCloseable k1 = MDC.putCloseable("key1", "value1") ;
 *             MDC.MDCCloseable k2 = MDC.putCloseable("key2", String.valueOf(value2)) ;
 *             MDC.MDCCloseable k2 = MDC.putCloseable("key3", String.valueOf(value3))) {
 *            ...
 *        }
 * </pre>
 * instead of:
 * <pre>
 *        try (MDCLog mdc = mdc().withKey1("value1")
 *                .withKey2(value2)
 *                .withKey3(value3)) {
 *            ...
 *        }
 * </pre>
 * Which also ensures that no key typos occurs (or at least they're consistent)
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class MDCLog implements AutoCloseable {

    private static final String NULL = "NULL";

    private final HashMap<String, String> restore;

    public static MDCLog mdc() {
        return new MDCLog();
    }

    public MDCLog() {
        this.restore = new HashMap<>();
    }

    @Override
    public void close() {
        restore.forEach(this::setOrRemove);
    }

    public MDCLog withAction(String action) {
        return with("action", action);
    }

    public MDCLog withAgencyId(Integer agencyId) {
        return with("agencyId", String.valueOf(agencyId));
    }

    public MDCLog withOutputType(String outputType) {
        return with("outputType", outputType);
    }

    public MDCLog withPeer(String peer) {
        return with("peer", String.valueOf(peer));
    }

    public MDCLog withTrackingId(String trackingId) {
        return with("trackingId", trackingId);
    }

    public MDCLog withProfiles(List<String> profiles) {
        return with("profiles", String.valueOf(profiles));
    }

    private MDCLog with(String key, String value) {
        restore.computeIfAbsent(key, this::makeRestoreValue);
        setOrRemove(key, value);
        return this;
    }

    void timing(Map.Entry<String, Long> e) {
        setOrRemove(e.getKey(), e.getValue().toString());
    }

    private String makeRestoreValue(String k) {
        String value = MDC.get(k);
        if (value == null)
            value = NULL;
        return value;
    }

    private void setOrRemove(String key, String value) {
        if (value == null || value == (Object) NULL) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

}
