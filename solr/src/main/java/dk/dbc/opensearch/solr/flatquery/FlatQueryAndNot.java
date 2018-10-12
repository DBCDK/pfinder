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

import dk.dbc.opensearch.solr.config.FieldSpec;
import java.util.ArrayList;
import java.util.List;

/**
 * Representing a flat query of numerous AND or NOT query parts
 *
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class FlatQueryAndNot extends FlatQuery {

    private final List<FlatQuery> and;
    private final List<FlatQuery> not;

    public FlatQueryAndNot() {
        this.not = new ArrayList<>();
        this.and = new ArrayList<>();
    }

    public FlatQueryAndNot(FlatQuery and) {
        this();
        this.and.add(and);
    }

    /**
     * Add an AND type subtree
     *
     * @param flatQuery query that is right side of an AND
     */
    public void addAnd(FlatQuery flatQuery) {
        and.add(flatQuery);
    }

    /**
     * Add an NOT type subtree
     *
     * @param flatQuery query that is right side of a NOT
     */
    public void addNot(FlatQuery flatQuery) {
        not.add(flatQuery);
    }

    /**
     * List of all added AND type subtrees
     *
     * @return list
     */
    public List<FlatQuery> ands() {
        return and;
    }

    /**
     * List of all added NOT type subtrees
     *
     * @return list
     */
    public List<FlatQuery> nots() {
        return not;
    }

    @Override
    public boolean allAreFilterQuery(String filterQueryName) {
        return and.stream().allMatch(q -> q.allAreFilterQuery(filterQueryName)) &&
               not.stream().allMatch(q -> q.allAreFilterQuery(filterQueryName));
    }

    @Override
    public boolean allAreNestedOfType(FieldSpec nested) {
        return and.stream().allMatch(q -> q.allAreNestedOfType(nested)) &&
               not.stream().allMatch(q -> q.allAreNestedOfType(nested));
    }

    @Override
    public boolean anyAreNestedOfType(FieldSpec nested) {
        return and.stream().anyMatch(q -> q.allAreNestedOfType(nested)) ||
               not.stream().anyMatch(q -> q.allAreNestedOfType(nested));
    }

    @Override
    public String toString() {
        return "{AND " + and + ", NOT " + not + "}";
    }

}
