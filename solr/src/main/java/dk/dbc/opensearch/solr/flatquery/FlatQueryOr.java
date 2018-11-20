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
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class FlatQueryOr extends FlatQuery {

    private static final long serialVersionUID = -2673604413757864295L;

    private final List<FlatQuery> or;

    public FlatQueryOr() {
        this.or = new ArrayList<>();
    }

    /**
     * Add a sub query to this OR node
     *
     * @param query sub query
     */
    public void addOr(FlatQuery query) {
        or.add(query);
    }

    /**
     * The list of sub-queries in the OR construction
     *
     * @return list
     */
    public List<FlatQuery> ors() {
        return or;
    }

    @Override
    public boolean allAreFilterQuery(String filterQueryName) {
        return or.stream().allMatch(q -> allAreFilterQuery(filterQueryName));
    }

    @Override
    public boolean allAreNestedOfType(FieldSpec nested) {
        return or.stream().allMatch(q -> q.allAreNestedOfType(nested));
    }

    @Override
    public boolean anyAreNestedOfType(FieldSpec nested) {
        return or.stream().anyMatch(q -> q.allAreNestedOfType(nested));
    }

    @Override
    public String toString() {
        return "{OR " + or + "}";
    }

}
