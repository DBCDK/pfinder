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

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class UserException extends Exception {

    private static final long serialVersionUID = 8466072039399258842L;

    private final String userMessage;

    public UserException(String userMessage, Exception ex) {
        super(ex.getMessage(), ex.getCause());
        this.userMessage = userMessage;
    }

    public UserException(String userMessage) {
        this(userMessage, new Exception(userMessage));
    }

    public String getUserMessage() {
        return userMessage;
    }

}
