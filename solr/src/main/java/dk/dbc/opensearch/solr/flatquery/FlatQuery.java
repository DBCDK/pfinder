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

import dk.dbc.opensearch.cql.BoolQuery;
import dk.dbc.opensearch.cql.QueryNode;
import dk.dbc.opensearch.cql.SearchQuery;
import dk.dbc.opensearch.cql.token.BooleanOpName;
import dk.dbc.opensearch.solr.config.FieldSpec;
import dk.dbc.opensearch.solr.SolrRules;

/**
 * Abstract class representing flattened query.
 *
 * @author DBC {@literal <dbc.dk>}
 */
public abstract class FlatQuery {

    /**
     * Map a Query Tree to a Flat tree given a set of rules
     *
     * @param solrRules the index rules
     * @param query     the query tree
     * @return a flat query representation
     */
    public static FlatQuery from(SolrRules solrRules, QueryNode query) {
        FlatQuery flat = new Flattener(solrRules).flatten(query);
        if (flat instanceof FlatQuerySearch) {
            flat = new FlatQueryAndNot(flat);
        }

        return flat;
    }

    /**
     * Tell if all nested queries can be put into same nested query
     *
     * @param spec the nested query spec wanted
     * @return true/false
     */
    public abstract boolean allAreNestedOfType(FieldSpec spec);

    /**
     * Tell if all nested queries can be put into same nested query
     *
     * @param spec the nested query spec wanted
     * @return true/false
     */
    public abstract boolean anyAreNestedOfType(FieldSpec spec);

    /**
     * Tell if all subtree nodes can be put into a filterquery
     *
     * @param filterQueryName the filter query name, that should be matched from
     *                        the FieldSpec
     * @return true/false
     */
    public abstract boolean allAreFilterQuery(String filterQueryName);

    private static class Flattener {

        private final SolrRules solrRules;

        Flattener(SolrRules solrRules) {
            this.solrRules = solrRules;
        }

        /**
         * Collapse a tree node into a flat node
         *
         * @param node tree type node
         * @return a flat node representing same query
         */
        private FlatQuery flatten(QueryNode node) {
            if (node instanceof BoolQuery) {
                BoolQuery bool = (BoolQuery) node;
                if (bool.getOperator() == BooleanOpName.OR) {
                    FlatQueryOr flat = new FlatQueryOr();
                    flattenOr(bool, flat);
                    return flat;
                } else {
                    FlatQueryAndNot flat = new FlatQueryAndNot();
                    flattenAndNot(bool, flat);
                    return flat;
                }
            } else if (node instanceof SearchQuery) {
                SearchQuery search = (SearchQuery) node;
                return flattenSearch(search);
            }
            throw new IllegalStateException("Cannot handle node type: " + node.getClass().getCanonicalName());
        }

        /**
         * Create a flat query node from a search query
         *
         * @param search tree type node
         * @return search node
         */
        private FlatQuery flattenSearch(SearchQuery search) {
            return new FlatQuerySearch(solrRules, search);
        }

        /**
         * Fill in a And/Not type flat node from a tree
         * <p>
         * Extract all AND and NOT nodes and put then into the flat query, if an
         * OR node is encountered add it to the flat query as a flatquery or
         * node.
         *
         * @param tree tree the query tree (and/not type)
         * @param flat target flat node
         */
        private void flattenAndNot(BoolQuery tree, FlatQueryAndNot flat) {
            QueryNode left = tree.getLeft();
            if (left instanceof SearchQuery) {
                flat.addAnd(flattenSearch((SearchQuery) left));
            } else if (left instanceof BoolQuery) {
                if (( (BoolQuery) left ).getOperator() == BooleanOpName.OR) {
                    flat.addAnd(flatten(left));
                } else {
                    flattenAndNot((BoolQuery) left, flat);
                }
            } else {
                throw new IllegalStateException("Cannot handle node type: " + left.getClass().getCanonicalName());
            }

            boolean not = tree.getOperator() == BooleanOpName.NOT;

            QueryNode right = tree.getRight();
            if (right instanceof SearchQuery) {
                if (not) {
                    flat.addNot(flattenSearch((SearchQuery) right));
                } else {
                    flat.addAnd(flattenSearch((SearchQuery) right));
                }
            } else if (right instanceof BoolQuery) {
                if (( (BoolQuery) right ).getOperator() == BooleanOpName.OR) {
                    if (not) {
                        flat.addNot(flatten(right));
                    } else {
                        flat.addAnd(flatten(right));
                    }
                } else {
                    flattenAndNot((BoolQuery) right, flat);
                }
            } else {
                throw new IllegalStateException("Cannot handle node type: " + right.getClass().getCanonicalName());
            }
        }

        /**
         * Fill in an or type flat node from a query tree
         * <p>
         * If other than searchTerm nodes and or nodes are encountered, build a
         * And/Not type flat node and use it
         *
         * @param tree the query tree (or type)
         * @param flat the target or type flat node
         */
        private void flattenOr(BoolQuery tree, FlatQueryOr flat) {
            QueryNode left = tree.getLeft();
            if (left instanceof SearchQuery) {
                flat.addOr(flattenSearch((SearchQuery) left));
            } else if (left instanceof BoolQuery) {
                if (( (BoolQuery) left ).getOperator() == BooleanOpName.OR) {
                    flattenOr((BoolQuery) left, flat);
                } else {
                    flat.addOr(flatten(left));
                }
            } else {
                throw new IllegalStateException("Cannot handle node type: " + left.getClass().getCanonicalName());
            }
            QueryNode right = tree.getRight();
            if (right instanceof SearchQuery) {
                flat.addOr(flattenSearch((SearchQuery) right));
            } else if (right instanceof BoolQuery) {
                if (( (BoolQuery) right ).getOperator() == BooleanOpName.OR) {
                    flattenOr((BoolQuery) right, flat);
                } else {
                    flat.addOr(flatten(right));
                }
            } else {
                throw new IllegalStateException("Cannot handle node type: " + right.getClass().getCanonicalName());
            }
        }
    }
}
