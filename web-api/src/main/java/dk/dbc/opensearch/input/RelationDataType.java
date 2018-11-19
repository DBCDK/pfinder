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

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public enum RelationDataType {
    TYPE, URI, FULL;

    public static RelationDataType from(String value) {
        switch (value) {
            case "type":
                return TYPE;
            case "uri":
                return URI;
            case "full":
                return FULL;
            default:
                throw new AssertionError();
        }
    }

}
