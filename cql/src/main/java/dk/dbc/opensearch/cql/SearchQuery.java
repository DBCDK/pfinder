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
public class SearchQuery implements QueryNode {

    private static final long serialVersionUID = -8203560611906805497L;

    private final String index;
    private final String relation;
    private final ModifierCollection modifiers;
    private final String searchTerm;

    transient private final Position pos;

    public SearchQuery(Position pos, String searchTerm) {
        this(pos, "default", "=", ModifierCollection.EMPTY, searchTerm);
    }

    public SearchQuery(Position pos, String index, String relation, ModifierCollection modifiers, String searchTerm) {
        this.pos = pos;
        this.index = index;
        this.relation = relation;
        this.modifiers = modifiers;
        this.searchTerm = searchTerm;
    }

    public String getIndex() {
        return index;
    }

    public String getRelation() {
        return relation;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public ModifierCollection getModifiers() {
        return modifiers;
    }

    public Position getPos() {
        return pos;
    }

    @Override
    public String toString() {
        String modText = modifiers.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList()).toString();
        return "{" + relation + modText + ":" + index + ", " + searchTerm + "}";
    }

}
