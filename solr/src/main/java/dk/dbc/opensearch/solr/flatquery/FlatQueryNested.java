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

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class FlatQueryNested extends FlatQuery {

    private final String query;
    private final FlatQuery nested;
    private final String filterQuery;

    public FlatQueryNested(String query, FlatQuery nested, String filterQuery) {
        this.query = query;
        this.nested = nested;
        this.filterQuery = filterQuery;
    }

    /**
     * Get the literal SolR query for this nested query
     *
     * @return the verbatim solr stmt for this nested query
     */
    public String getQuery() {
        return query;
    }

    public String getFilterQueryName() {
        return filterQuery;
    }

    @Override
    public boolean allAreFilterQuery(String filterQueryName) {
        return filterQueryName.equals(filterQuery);
    }

    @Override
    public boolean allAreNestedOfType(FieldSpec name) {
        return false;
    }

    @Override
    public boolean anyAreNestedOfType(FieldSpec name) {
        return false;
    }

    @Override
    public String toString() {
        return query;
    }
}
