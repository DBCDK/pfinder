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
package dk.dbc.opensearch.setup;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class HttpClient {

    private String connectTimeout;
    private String readTimeout;

    public String getConnectTimeout() {
        return connectTimeout;
    }

    public long connectTimeoutMS() {
        return toMs(connectTimeout);
    }

    public void setConnectTimeout(String connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getReadTimeout() {
        return readTimeout;
    }

    public long readTimeoutMS() {
        return toMs(readTimeout);
    }

    public void setReadTimeout(String readTimeout) {
        this.readTimeout = readTimeout;
    }

    private static long toMs(String spec) {
        String[] split = spec.split("(?<=\\d)(?=\\D)");
        if (split.length == 2) {
            long units = Long.parseUnsignedLong(split[0], 10);
            switch (split[1].toLowerCase(Locale.ROOT)) {
                case "ms":
                    return units;
                case "s":
                    return TimeUnit.SECONDS.toMillis(units);
                case "m":
                    return TimeUnit.MINUTES.toMillis(units);
                case "h":
                    return TimeUnit.HOURS.toMillis(units);
                default:
                    break;
            }
        }
        throw new IllegalArgumentException("Invalid time spec: " + spec);
    }

}
