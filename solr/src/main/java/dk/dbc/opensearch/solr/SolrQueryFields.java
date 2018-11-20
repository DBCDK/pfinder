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

import dk.dbc.opensearch.cql.CQLError;
import dk.dbc.opensearch.cql.CQLParser;
import dk.dbc.opensearch.cql.ModifierCollection;
import dk.dbc.opensearch.cql.QueryNode;
import dk.dbc.opensearch.cql.token.BooleanOpName;
import dk.dbc.opensearch.cql.token.TokenList;
import dk.dbc.opensearch.solr.flatquery.FlatQuery;
import dk.dbc.opensearch.solr.profile.Profile;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.SolrQuery;

/**
 *
 * Class for caching of parsed queries
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class SolrQueryFields implements Serializable {

    private static final long serialVersionUID = 5194647199009610759L;

    private final String query;
    private final List<String> nestedQueries;
    private final List<String> filterQueries;

    public static SolrQueryFields fromCQL(SolrRules rules, String cql, Profile profile) {
        return fromCQL(rules, cql, profile, CQLParser.DEFAULT_RELATIONS, CQLParser.DEFAULT_BOOLEANS, TokenList.BOOLEAN_NAMES);
    }

    public static SolrQueryFields fromCQL(SolrRules rules, String cql, Profile profile,
                                          Map<String, Function<ModifierCollection, CQLError>> relations,
                                          Map<String, Function<ModifierCollection, CQLError>> booleans,
                                          Map<String, BooleanOpName> booleanNames) {
        QueryNode cqlTree = CQLParser.parse(cql, relations, booleans, booleanNames);
        FlatQuery query = FlatQuery.from(rules, cqlTree);
        List<FlatQuery> nestedQueries = NestedQueries.from(query);
        List<FlatQuery> filterQueries = FilterQuery.from(query);
        filterQueries.add(profile.getSearchFilterQuery());
        return new SolrQueryFields(query, nestedQueries, filterQueries);
    }

    public SolrQueryFields(FlatQuery query, List<FlatQuery> nestedQueries, List<FlatQuery> filterQueries) {
        this.query = QueryBuilder.queryFrom(query);
        this.nestedQueries = nestedQueries.stream().map(QueryBuilder::queryFrom).collect(Collectors.toList());
        this.filterQueries = filterQueries.stream().map(QueryBuilder::queryFrom).collect(Collectors.toList());
    }

    /**
     * Convert a query into a SolR query with nested queries and filter queries
     *
     * @return SolR query
     */
    public SolrQuery asSolrQuery() {
        SolrQuery solrQuery = new SolrQuery(query);
        int i = 1;
        for (String nestedQuery : nestedQueries) {
            solrQuery.add("q" + i++, nestedQuery);
        }
        filterQueries.forEach(solrQuery::addFilterQuery);
        return solrQuery;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + Objects.hashCode(this.query);
        hash = 17 * hash + Objects.hashCode(this.nestedQueries);
        hash = 17 * hash + Objects.hashCode(this.filterQueries);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SolrQueryFields other = (SolrQueryFields) obj;
        return ( Objects.equals(this.query, other.query) &&
                 Objects.equals(this.nestedQueries, other.nestedQueries) &&
                 Objects.equals(this.filterQueries, other.filterQueries) );
    }

    @Override
    public String toString() {
        return "SolrQueryFields{" + "query=" + query + ", nestedQueries=" + nestedQueries + ", filterQueries=" + filterQueries + '}';
    }

}
