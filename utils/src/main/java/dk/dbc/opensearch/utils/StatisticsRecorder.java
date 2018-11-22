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

import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dbc.opensearch.utils.MDCLog.mdc;

/**
 * A millisecond timing accumulator
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class StatisticsRecorder {

    private static final Logger log = LoggerFactory.getLogger(StatisticsRecorder.class);

    private final ConcurrentHashMap<String, Long> timings;
    private final long start;

    public StatisticsRecorder() {
        this.timings = new ConcurrentHashMap<>();
        this.start = System.nanoTime();
    }

    /**
     * This log all the timings accumulated including "total" which is from this
     * object was created to now.
     */
    public void log() {
        timings.put("total", ms(start));
        try (MDCLog mdc = mdc()) {
            timings.entrySet().forEach(mdc::timing);
            log.info("timings");
        }
    }

    /**
     * Make a timer
     * <p>
     * When it is closed (AutoClosable), accumulate the time spent under this
     * name
     *
     * @param name name of the timing
     * @return AutoClosable context
     */
    public Timing timer(String name) {
        long timingStart = System.nanoTime();
        return () -> timings.compute(name, (k, v) -> ms(timingStart) +
                                                         longNullIs0(v));
    }

    private static long longNullIs0(Long v) {
        return v == null ? 0 : v;
    }

    private long ms(long timingStart) {
        return ( System.nanoTime() - timingStart ) / 1_000_000L;
    }

    @Override
    public String toString() {
        return "StatisticsRecorder{" + "timings=" + timings + '}';
    }

}
