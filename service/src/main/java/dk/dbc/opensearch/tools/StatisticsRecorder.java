/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-service
 *
 * opensearch-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.tools;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dbc.opensearch.tools.MDCLog.mdc;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class StatisticsRecorder {

    private static final Logger log = LoggerFactory.getLogger(StatisticsRecorder.class);

    private final HashMap<String, Long> timings;
    private final Timing total;

    public StatisticsRecorder() {
        this.timings = new HashMap<>();
        this.total = new Timing(timings, "total");
    }

    public void clear() {
        timings.clear();
    }

    public void log() {
        total.close();
        try (MDCLog mdc = mdc()) {
            timings.entrySet().forEach(mdc::timing);
            log.info("timings");
        }
    }

    public Timing timer(String name) {
        return new Timing(timings, name);
    }

    @Override
    public String toString() {
        return "StatisticsRecorder{" + "timings=" + timings + '}';
    }

}
