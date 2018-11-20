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
package dk.dbc.opensearch.solr;

import dk.dbc.opensearch.solr.config.FieldSpec;
import dk.dbc.opensearch.solr.flatquery.FlatQueryOr;
import dk.dbc.opensearch.solr.flatquery.FlatQuery;
import dk.dbc.opensearch.solr.flatquery.FlatQuerySearch;
import dk.dbc.opensearch.solr.flatquery.FlatQueryAndNot;
import dk.dbc.opensearch.cql.CQLError;
import dk.dbc.opensearch.cql.CQLException;
import dk.dbc.opensearch.cql.CQLException.Position;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class NestedQueries implements Serializable {

    private static final long serialVersionUID = 8706346364547335984L;

    static List<FlatQuery> from(FlatQuery query) {
        return new NestedQueries(query).getQueries();
    }

    private final ArrayList<FlatQuery> queries;

    private NestedQueries(FlatQuery query) {
        this.queries = new ArrayList<>();
        extractNested(query);
    }

    /**
     * Get the list of queries that are build
     * <p>
     * index 0 = q=
     * index 1 = q1= (for nested query)
     * index 2 = q2= (for nested query)
     * aso.
     *
     * @return list
     */
    List<FlatQuery> getQueries() {
        return queries;
    }

    /**
     * Extract all nested queries, replacing them with FlatQueryNested nodes
     * <p>
     * After all nested are extracted, traverse remaining nodes, and perform
     * same task for them
     *
     * @param query the flat query node to extract from
     */
    private void extractNested(FlatQuery query) {
        if (query instanceof FlatQueryAndNot) {
            FlatQueryAndNot flat = (FlatQueryAndNot) query;
            for (;;) {
                boolean extractedNestedQuery = extractNestedAndNot(flat);
                if (!extractedNestedQuery) {
                    break;
                }
            }
            flat.ands().forEach(this::extractNested);
            flat.nots().forEach(this::extractNested);
        } else if (query instanceof FlatQueryOr) {
            FlatQueryOr flat = (FlatQueryOr) query;
            for (;;) {
                boolean extractedNestedQuery = extractNestedOr(flat);
                if (!extractedNestedQuery) {
                    break;
                }
            }
            flat.ors().forEach(this::extractNested);
        }
    }

    /**
     * Extract nested queries from a AND/NOT type node
     *
     * @param query the query node
     * @return if a nested query has been extracted
     */
    private boolean extractNestedAndNot(FlatQueryAndNot query) {
        ListIterator<FlatQuery> i = query.ands().listIterator();
        while (i.hasNext()) {
            FlatQuery element = i.next();
            if (element instanceof FlatQuerySearch) {
                FlatQuerySearch search = (FlatQuerySearch) element;
                FieldSpec fieldSpec = search.getFieldSpec();
                if (fieldSpec.canNest()) {
                    Position pos = search.getPos();
                    FlatQueryAndNot replace = fieldSpec.nestedQueryTree();
                    queries.add(replace);
                    i.set(fieldSpec.nestedQueryNode(queries.size(), replace));
                    replace.addAnd(element);
                    extractNestedAnd(replace, fieldSpec, query.ands().listIterator(), pos);
                    extractNestedNot(replace, fieldSpec, query.nots().listIterator(), pos);
                    return true;
                }
            }
        }
        i = query.nots().listIterator();
        while (i.hasNext()) {
            FlatQuery element = i.next();
            if (element instanceof FlatQuerySearch) {
                FlatQuerySearch search = (FlatQuerySearch) element;
                FieldSpec fieldSpec = search.getFieldSpec();
                if (fieldSpec.canNest()) {
                    Position pos = search.getPos();
                    FlatQueryAndNot replace = fieldSpec.nestedQueryTree();
                    queries.add(replace);
                    i.set(fieldSpec.nestedQueryNode(queries.size(), replace));
                    replace.addNot(element);
                    extractNestedAnd(replace, fieldSpec, query.ands().listIterator(), pos);
                    extractNestedNot(replace, fieldSpec, query.nots().listIterator(), pos);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Extract nested queries from a OR type node
     *
     * @param query the query node
     * @return if a nested query has been extracted
     */
    private boolean extractNestedOr(FlatQueryOr query) {
        ListIterator<FlatQuery> i = query.ors().listIterator();
        while (i.hasNext()) {
            FlatQuery element = i.next();
            if (element instanceof FlatQuerySearch) {
                FlatQuerySearch search = (FlatQuerySearch) element;
                FieldSpec nested = search.getFieldSpec();
                if (nested.canNest()) {
                    Position pos = search.getPos();
                    FlatQueryOr replace = new FlatQueryOr();
                    queries.add(nested.nestedQueryForOrTree(replace));
                    FlatQuery base = nested.nestedQueryForOrTree(replace);
                    replace.addOr(element);
                    i.set(nested.nestedQueryNode(queries.size(), base));
                    extractNestedOr(replace, nested, i, pos);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove all nodes from the AND part, that can be grouped with a given nest
     *
     * @param nested the expression that should receive the nodes removed
     * @param spec   the nesting spec
     * @param i      iterator to remove nodes from
     */
    private void extractNestedAnd(FlatQueryAndNot nested, FieldSpec spec, ListIterator<FlatQuery> i, Position pos) {
        while (i.hasNext()) {
            FlatQuery q = i.next();
            if (q.allAreNestedOfType(spec)) {
                i.remove();
                nested.addAnd(q);
            } else if (q.anyAreNestedOfType(spec)) {
                throw new CQLException(CQLError.UNSUPPORTED_COMBINATION_OF_INDEXES, pos);
            }
        }
    }

    /**
     * Remove all nodes from the NOT part, that can be grouped with a given nest
     *
     * @param nested the expression that should receive the nodes removed
     * @param spec   the nesting spec
     * @param i      iterator to remove nodes from
     */
    private void extractNestedNot(FlatQueryAndNot nested, FieldSpec spec, ListIterator<FlatQuery> i, Position pos) {
        while (i.hasNext()) {
            FlatQuery q = i.next();
            if (q.allAreNestedOfType(spec)) {
                i.remove();
                nested.addNot(q);
            } else if (q.anyAreNestedOfType(spec)) {
                throw new CQLException(CQLError.UNSUPPORTED_COMBINATION_OF_INDEXES, pos);
            }
        }
    }

    /**
     * Remove all nodes from an OR list, that can be grouped with a given nest
     *
     * @param nested the expression that should receive the nodes removed
     * @param spec   the nesting spec
     * @param i      iterator to remove nodes from
     */
    private void extractNestedOr(FlatQueryOr nested, FieldSpec spec, ListIterator<FlatQuery> i, Position pos) {
        while (i.hasNext()) {
            FlatQuery q = i.next();
            if (q.allAreNestedOfType(spec)) {
                i.remove();
                nested.addOr(q);
            } else if (q.anyAreNestedOfType(spec)) {
                throw new CQLException(CQLError.UNSUPPORTED_COMBINATION_OF_INDEXES, pos);
            }
        }
    }

    @Override
    public String toString() {
        return queries.toString();
    }

}
