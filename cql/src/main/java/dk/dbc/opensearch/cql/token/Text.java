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
package dk.dbc.opensearch.cql.token;

import java.util.EnumSet;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Text extends Token {

    private static final EnumSet<Type> MATCH_TYPES = EnumSet.of(Type.TEXT);

    public Text(String content, int at) {
        super(content, at);
    }

    @Override
    protected EnumSet<Token.Type> matchTypes() {
        return MATCH_TYPES;
    }

    public String getText() {
        return content;
    }

    @Override
    public String toString() {
        return "Token.Text{" + content + '}';
    }
}
