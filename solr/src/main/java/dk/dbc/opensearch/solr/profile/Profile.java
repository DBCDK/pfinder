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
package dk.dbc.opensearch.solr.profile;

import dk.dbc.opensearch.solr.flatquery.FlatQueryOr;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A (stacked) profile collection.
 * <p>
 * This contains the union of one or more profiles, as stored in
 * {@link Profiles}.
 * <p>
 * This is mapped into 2 queries (used as filter queries, since they don't
 * change, and caching of this group is wanted for performance. The 2 queries
 * are:
 * Search and Relation. the search filter is: what is allowed to be searched for
 * results, the Relation is all that is allowed to fetch relations from.
 * <p>
 * Also for each collection is is possible to check is a given relation type is
 * allowed to be shown.
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Profile implements Serializable {

    private static final long serialVersionUID = -7462331936710472030L;

    private final FlatQueryOr searchFilterQuery;
    private final FlatQueryOr relationFilterQuery;
    private final Map<String, Set<String>> relations;

    /**
     * Store the values
     *
     * @param searchFilterQuery   Query for search profile
     * @param relationFilterQuery Query for relation profile
     * @param relations           A group of relations by collectionIdentifier
     */
    Profile(FlatQueryOr searchFilterQuery, FlatQueryOr relationFilterQuery, Map<String, Set<String>> relations) {
        this.searchFilterQuery = searchFilterQuery;
        this.relationFilterQuery = relationFilterQuery;
        this.relations = relations;
    }

    public FlatQueryOr getSearchFilterQuery() {
        return searchFilterQuery;
    }

    public FlatQueryOr getRelationFilterQuery() {
        return relationFilterQuery;
    }

    /**
     * If an inbound relation is allowed that matches this relation
     *
     * @param collectionIdentifier the sourceIdentifier from open agency
     * @param relationName         the name of the outbound relation
     * @return does this exist?
     */
    public boolean allowsRelation(String collectionIdentifier, String relationName) {
        return relations.getOrDefault(collectionIdentifier, Collections.EMPTY_SET)
                .contains(relationName);
    }

    @Override
    public String toString() {
        return "{" + searchFilterQuery + ", " + relationFilterQuery + ", " + relations + '}';
    }

}
