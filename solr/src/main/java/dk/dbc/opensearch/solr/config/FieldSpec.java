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
package dk.dbc.opensearch.solr.config;

import dk.dbc.opensearch.solr.flatquery.FlatQueryOr;
import dk.dbc.opensearch.solr.flatquery.FlatQuery;
import dk.dbc.opensearch.solr.flatquery.FlatQueryNested;
import dk.dbc.opensearch.solr.flatquery.FlatQueryAndNot;
import dk.dbc.opensearch.cql.QueryNode;
import dk.dbc.opensearch.solr.SolrRules;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class FieldSpec {

    private final SolrRules solrRules;
    private final String nestedGroup;
    private final String filterQuery;
    private final String query;
    private final QueryNode cqlQuery;
    private final SolrConfigFieldType type;

    public FieldSpec(SolrRules solrRules, String nestedGroup, String filterQuery, String query, QueryNode cqlQuery, SolrConfigFieldType type) {
        this.solrRules = solrRules;
        this.nestedGroup = nestedGroup;
        this.filterQuery = filterQuery;
        this.query = query;
        this.cqlQuery = cqlQuery;
        this.type = type;
    }

    public String getNestedGroup() {
        return nestedGroup;
    }

    public SolrConfigFieldType getType() {
        return type;
    }

    /**
     * Construct a FlatQuert tree node, that represents a nested query covering
     * this field
     *
     * @param i      the sequence number for the nested query spec
     * @param nested the query that will be nested (currently only used to check
     *               if the entire nested query is a "filter-query"able tree)
     * @return the nested query to use
     */
    public FlatQueryNested nestedQueryNode(int i, FlatQuery nested) {
        return new FlatQueryNested(String.format(query, i), nested, filterQuery);
    }

    /**
     * Build a nested query base using the cqlQuery rule
     *
     * @return a and/not tree to fill queries into
     */
    public FlatQueryAndNot nestedQueryTree() {
        if (cqlQuery == null) {
            return new FlatQueryAndNot();
        }
        FlatQuery cqlFlatQuery = FlatQuery.from(solrRules, cqlQuery);
        if (cqlFlatQuery instanceof FlatQueryAndNot) {
            return (FlatQueryAndNot) cqlFlatQuery;
        }
        FlatQueryAndNot base = new FlatQueryAndNot();
        base.addAnd(cqlFlatQuery);
        return base;
    }

    /**
     * Build a nested query base for an or expression combined with the cqlQuery
     * from the fieldspec
     *
     * @param or The or node that is the subtree
     * @return a nested query with the rules from sqlQuery
     */
    public FlatQuery nestedQueryForOrTree(FlatQueryOr or) {
        if (cqlQuery == null) {
            return or; // No cql query, no need to add a and/not node
        }
        FlatQueryAndNot base = nestedQueryTree();
        base.addAnd(or);
        return base;
    }

    /**
     * Is this field nestable
     *
     * @return true/false
     */
    public boolean canNest() {
        return nestedGroup != null;
    }

    /**
     * Should this field be preferred as a filter query
     *
     * @return true/false
     */
    public String getFilterQueryName() {
        return filterQuery;
    }

    /**
     * This field, is it in the same nested query group an another field?
     *
     * @param other other field
     * @return could the field be added to the others nested query block?
     */
    public boolean sameNestedGroup(FieldSpec other) {
        return canNest() && nestedGroup.equals(other.nestedGroup);
    }

    @Override
    public String toString() {
        return "FieldSpec{" + "solrRules=" + solrRules + ", nestedGroup=" + nestedGroup + ", filterQuery=" + filterQuery + ", query=" + query + ", cqlQuery=" + cqlQuery + ", type=" + type + '}';
    }
}
