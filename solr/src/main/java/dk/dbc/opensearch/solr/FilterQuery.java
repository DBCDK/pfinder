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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.solr;

import dk.dbc.opensearch.solr.flatquery.FlatQuery;
import dk.dbc.opensearch.solr.flatquery.FlatQueryAndNot;
import dk.dbc.opensearch.solr.flatquery.FlatQueryNested;
import dk.dbc.opensearch.solr.flatquery.FlatQueryOr;
import dk.dbc.opensearch.solr.flatquery.FlatQuerySearch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class FilterQuery {

    public static List<FlatQuery> from(FlatQuery query) {
        return new FilterQuery().extractFrom(query).getFilterQueries();
    }

    private final Map<String, FlatQueryAndNot> filterQueriesByName;
    private final List<FlatQuery> filterQueries;

    private FilterQuery() {
        this.filterQueriesByName = new HashMap<>();
        this.filterQueries = new ArrayList<>();
    }

    private FlatQueryAndNot filterQueryFor(String filterQueryName) {
        return filterQueriesByName.computeIfAbsent(filterQueryName, n ->
                                           {
                                               FlatQueryAndNot replace = new FlatQueryAndNot();
                                               filterQueries.add(replace);
                                               return replace;
                                           });
    }

    private FilterQuery extractFrom(FlatQuery query) {
        if (query instanceof FlatQueryAndNot) {
            // extract only from top level and nodes
            extract((FlatQueryAndNot) query);
        } else if (query instanceof FlatQueryOr) {
            extractOrOnTopNode((FlatQueryOr) query);
        }
        return this;
    }

    public List<FlatQuery> getFilterQueries() {
        return filterQueries;
    }

    private void extract(FlatQueryAndNot flatQuery) {
        for (Iterator<FlatQuery> iterator = flatQuery.ands().iterator() ; iterator.hasNext() ;) {
            FlatQuery element = iterator.next();
            String filterQueryName = findFilterQueryName(element);
            if (filterQueryName != null) {
                FlatQueryAndNot replace = filterQueryFor(filterQueryName);
                iterator.remove();
                replace.addAnd(element);
            }
        }
    }

    /**
     * Clear top or node, and move all to new filterQuery or node
     *
     * @param flatQuery top node
     */
    private void extractOrOnTopNode(FlatQueryOr flatQuery) {
        String filterQueryName = findFilterQueryName(flatQuery);
        if (filterQueryName != null) {
            FlatQueryOr filterQuery = new FlatQueryOr();
            filterQueries.add(filterQuery);
            filterQuery.ors().addAll(flatQuery.ors());
            flatQuery.ors().clear();
        }
    }

    private static String findFilterQueryName(FlatQuery query) {
        if (query instanceof FlatQueryNested) {
            return ( (FlatQueryNested) query ).getFilterQueryName();
        } else if (query instanceof FlatQuerySearch) {
            return ( (FlatQuerySearch) query ).getFilterQueryName();
        } else if (query instanceof FlatQueryOr) {
            Iterator<FlatQuery> i = ( (FlatQueryOr) query ).ors().iterator();
            return allTheSameFilter(i);
        } else if (query instanceof FlatQueryAndNot) {
            Iterator<FlatQuery> i = ( (FlatQueryAndNot) query ).ands().iterator();
            if (i.hasNext()) {
                String andName = allTheSameFilter(i);
                if (andName == null) {
                    return null;
                }
                i = ( (FlatQueryAndNot) query ).nots().iterator();
                if (!i.hasNext()) {
                    return andName;
                }
                String notName = allTheSameFilter(i);
                if (andName.equals(notName)) {
                    return andName;
                }
            } else {
                i = ( (FlatQueryAndNot) query ).nots().iterator();
                return allTheSameFilter(i);
            }
            return null;
        }

        return null;
    }

    private static String allTheSameFilter(Iterator<FlatQuery> i) {
        if (!i.hasNext()) {
            return null;
        }
        String name = findFilterQueryName(i.next());
        if (name == null) {
            return null;
        }
        while (i.hasNext()) {
            if (!name.equals(findFilterQueryName(i.next()))) {
                return null;
            }
        }
        return name;
    }
}
