/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-solr
 *
 * opensearch-solr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-solr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a modifiableCopy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.solr.flatquery;

import dk.dbc.opensearch.cql.CQLException.Position;
import dk.dbc.opensearch.cql.Modifier;
import dk.dbc.opensearch.cql.SearchQuery;
import dk.dbc.opensearch.solr.config.FieldSpec;
import dk.dbc.opensearch.solr.SolrRules;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.util.Collections.EMPTY_MAP;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class FlatQuerySearch extends FlatQuery {

    private final Position pos;
    private final String index;
    private final String relation;
    private final Map<String, Modifier> modifiers;
    private final String term;
    private final FieldSpec fieldSpec;

    public FlatQuerySearch(SolrRules rules, SearchQuery query) {
        this.pos = query.getPos();
        this.index = rules.indexName(query.getIndex(), pos);
        this.relation = query.getRelation();
        this.modifiers = query.getModifiers();
        this.term = query.getSearchTerm();
        this.fieldSpec = rules.fieldSpec(query.getIndex(), pos);
    }

    public FlatQuerySearch(Position pos, String index, String term, FieldSpec fieldSpec) {
        this.pos = pos;
        this.index = index;
        this.relation = "=";
        this.modifiers = EMPTY_MAP;
        this.term = term;
        this.fieldSpec = fieldSpec;
    }

    /**
     * The SolR index name
     * <p>
     * Mapped version of the cql index
     *
     * @return index name
     */
    public String getIndex() {
        return index;
    }

    /**
     * The relation type from the original cql query
     *
     * @return search relation type
     */
    public String getRelation() {
        return relation;
    }

    /**
     * The modifiers for this query relation
     *
     * @return map of lowercase modifier name to modifier structure
     */
    public Map<String, Modifier> getModifiers() {
        return modifiers;
    }

    /**
     * What is searched for
     *
     * @return the query term
     */
    public String getTerm() {
        return term;
    }

    /**
     * The position of the search token (index or term)
     *
     * @return position object for exception
     */
    public Position getPos() {
        return pos;
    }

    /**
     * Get the field spec for the index in question
     *
     * @return a field spec
     */
    public FieldSpec getFieldSpec() {
        return fieldSpec;
    }

    public String getFilterQueryName() {
        return fieldSpec.getFilterQueryName();
    }

    @Override
    public boolean allAreFilterQuery(String filterQueryName) {
        return filterQueryName.equals(fieldSpec.getFilterQueryName());
    }

    @Override
    public boolean allAreNestedOfType(FieldSpec that) {
        return isNestedOfType(that);
    }

    @Override
    public boolean anyAreNestedOfType(FieldSpec that) {
        return isNestedOfType(that);
    }

    private boolean isNestedOfType(FieldSpec that) {
        return fieldSpec != null && that.sameNestedGroup(fieldSpec);
    }

    @Override
    public String toString() {
        if (modifiers.isEmpty() && index.isEmpty()) {
            return term;
        } else if (modifiers.isEmpty()) {
            return relation + ';' + index + ':' + term;
        } else {
            return relation + '[' + modifiers.entrySet().stream().sorted(Entry.comparingByKey()).map(Entry::getValue).map(Object::toString).collect(Collectors.joining(",")) + ']' + ';' + index + ':' + term;
        }
    }
}
