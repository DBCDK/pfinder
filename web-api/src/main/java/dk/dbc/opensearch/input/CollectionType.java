/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-web-api
 *
 * opensearch-web-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-web-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.input;

import java.util.Locale;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public enum CollectionType {
    WORK, WORK1, MANIFESTATION;

    public static CollectionType from(String value) {
        switch (value.toLowerCase(Locale.ROOT)) {
            case "work":
                return WORK;
            case "work-1":
                return WORK1;
            case "manifestation":
                return MANIFESTATION;
            default:
                throw new AssertionError();
        }
    }

}
