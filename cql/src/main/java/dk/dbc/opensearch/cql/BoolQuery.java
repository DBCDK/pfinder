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

import dk.dbc.opensearch.cql.CQLException.Position;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class BoolQuery implements QueryNode {

    private QueryNode left;
    private final String operator;
    private final Position pos;
    private final ModifierCollection modifiers;
    private QueryNode right;

    public BoolQuery(QueryNode left, Position pos, String operator, ModifierCollection modifiers, QueryNode right) {
        this.left = left;
        this.pos = pos;
        this.operator = operator;
        this.modifiers = modifiers;
        this.right = right;
    }

    public QueryNode getLeft() {
        return left;
    }

    public void setLeft(QueryNode left) {
        this.left = left;
    }

    public Position getPos() {
        return pos;
    }

    public String getOperator() {
        return operator;
    }

    public QueryNode getRight() {
        return right;
    }

    public void setRight(QueryNode right) {
        this.right = right;
    }

    public ModifierCollection getModifiers() {
        return modifiers;
    }

    @Override
    public String toString() {
        String modText = modifiers.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList()).toString();
        return "{" + operator + modText + ":" + left + ", " + right + "}";
    }

}
