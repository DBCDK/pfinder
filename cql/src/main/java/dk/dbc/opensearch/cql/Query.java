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

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.util.Collections.EMPTY_MAP;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public interface Query {

    enum Type {
        SEARCH,
        BOOLEAN
    }

    Type getType();

    public static class Search implements Query {

        private final String index;
        private final String operator;
        private final Map<String, Modifier> modifiers;
        private final SearchTerm searchTerm;

        public Search(String searchTerm) {
            this("default", "=", EMPTY_MAP, searchTerm);
        }

        public Search(String index, String operator, Map<String, Modifier> modifiers, String searchTerm) {
            this.index = index;
            this.operator = operator;
            this.modifiers = modifiers;
            this.searchTerm = new SearchTerm(searchTerm);
        }

        public String getIndex() {
            return index;
        }

        public String getOperator() {
            return operator;
        }

        public SearchTerm getSearchTerm() {
            return searchTerm;
        }

        public Map<String, Modifier> getModifiers() {
            return modifiers;
        }

        @Override
        public Type getType() {
            return Type.SEARCH;
        }

        @Override
        public String toString() {
            String modText = modifiers.entrySet().stream()
                    .sorted(Entry.comparingByKey())
                    .map(Entry::getValue)
                    .collect(Collectors.toList()).toString();
            return "{" + operator + modText + ":" + index + ", " + searchTerm + "}";
        }
    }

    public static class BooleanOp implements Query {

        private final Query left;
        private final String operator;
        private final Map<String, Modifier> modifiers;
        private final Query right;

        public BooleanOp(Query left, String operator, Map<String, Modifier> modifiers, Query right) {
            this.left = left;
            this.operator = operator;
            this.modifiers = modifiers;
            this.right = right;
        }

        public Query getLeft() {
            return left;
        }

        public String getOperator() {
            return operator;
        }

        public Query getRight() {
            return right;
        }

        public Map<String, Modifier> getModifiers() {
            return modifiers;
        }

        @Override
        public Type getType() {
            return Type.BOOLEAN;
        }

        @Override
        public String toString() {
            String modText = modifiers.entrySet().stream()
                    .sorted(Entry.comparingByKey())
                    .map(Entry::getValue)
                    .collect(Collectors.toList()).toString();
            return "{" + operator + modText + ":" + left + ", " + right + "}";
        }
    }
}