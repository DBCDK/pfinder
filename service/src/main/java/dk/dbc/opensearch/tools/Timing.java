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

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Timing implements AutoCloseable {

    private final HashMap<String, Long> timings;
    private final String name;
    private final long start;

    Timing(HashMap<String, Long> timings, String name) {
        this.timings = timings;
        this.name = name;
        this.start = System.nanoTime();
    }

    @Override
    public void close() {
        timings.put(name, ( System.nanoTime() - start ) / 1_000_000L);
    }

}
