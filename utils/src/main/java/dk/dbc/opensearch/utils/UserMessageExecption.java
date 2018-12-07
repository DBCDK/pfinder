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

import java.util.Map;

/**
 * This is intended to be thrown after an incident has been logged, and output
 * to the user is needed.
 * <p>
 * The message is intended to be printed to the user. And all logging of an
 * incident is done before this is is thrown
 * <p>
 * Messages of this type isn't
 *
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class UserMessageExecption extends RuntimeException {

    private static final long serialVersionUID = 8466072039399258842L;

    private final UserMessage userMessage;

    public UserMessageExecption(UserMessage userMessage) {
        this.userMessage = userMessage;
    }

    public String getUserMessage(Map<UserMessage, String> messages) {
        return messages.getOrDefault(userMessage, "Unknown error: " + userMessage);
    }

}
