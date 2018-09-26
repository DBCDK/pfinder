/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-cql
 *
 * opensearch-cql is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-cql is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.cql;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class CQLException extends RuntimeException {

    private static final long serialVersionUID = -1737736039680126129L;

    public static class Position {

        private final String query;
        private final int pos;

        public Position(String query, int pos) {
            this.query = query;
            this.pos = pos;
        }

        @Override
        public String toString() {
            return query.substring(0, pos) + "--->" + query.substring(pos);
        }
    }

    protected final Position position;

    public CQLException(Position position, String message) {
        super(message);
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " at: " + position;
    }

}
