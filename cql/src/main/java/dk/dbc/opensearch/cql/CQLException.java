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

import java.io.Serializable;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class CQLException extends RuntimeException {

    private static final long serialVersionUID = -1737736039680126129L;

    public static class Position implements Serializable {

        private static final long serialVersionUID = -5255304122052942508L;

        private final String query;
        private final int pos;

        public Position(String query, int pos) {
            this.query = query;
            this.pos = pos;
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            if (pos > 55) {
                buffer.append("...").append(query.substring(pos - 50, pos));
            } else {
                buffer.append(query.substring(0, pos));
            }
            buffer.append("--->");
            if (query.length() - pos > 55) {
                buffer.append(query.substring(pos, pos + 50)).append("...");
            } else {
                buffer.append(query.substring(pos));
            }
            return buffer.toString();
        }
    }

    protected final Position position;
    protected final CQLError cqlError;

    public CQLException(Position position, String message) {
        this(CQLError.GENERAL_SYSTEM_ERROR, position, message);
    }

    public CQLException(CQLError cqlError, Position position) {
        this(cqlError, position, cqlError.getMsg());
    }

    public CQLException(CQLError cqlError, Position position, String message) {
        super(message);
        this.cqlError = cqlError;
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " at: " + position;
    }

    public CQLError getCqlError() {
        return cqlError;
    }

}
