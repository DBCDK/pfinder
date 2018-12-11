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
import dk.dbc.opensearch.solr.flatquery.FlatQueryNested;
import dk.dbc.opensearch.solr.flatquery.FlatQuery;
import dk.dbc.opensearch.solr.flatquery.FlatQuerySearch;
import dk.dbc.opensearch.solr.flatquery.FlatQueryAndNot;
import dk.dbc.opensearch.cql.CQLError;
import dk.dbc.opensearch.cql.CQLException;
import dk.dbc.opensearch.cql.CQLException.Position;
import dk.dbc.opensearch.cql.SearchTerm;
import dk.dbc.opensearch.solr.config.SolrConfigFieldType;
import java.util.Iterator;
import org.apache.solr.client.solrj.util.ClientUtils;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class QueryBuilder {

    public static String queryFrom(FlatQuery query) {
        return new QueryBuilder(query).getQuery();
    }

    private final StringBuilder buffer;

    private QueryBuilder(FlatQuery query) {
        this.buffer = new StringBuilder();
        buildQueryTop(query);
    }

    private void buildQueryTop(FlatQuery query) {
        if (query instanceof FlatQueryOr &&
            // All nodes from the top or node  are extracted into filter-queries
            ( (FlatQueryOr) query ).ors().isEmpty()) {
            buffer.append("*:*");
        } else {
            buildQuery(query);
        }
    }

    private void buildQuery(FlatQuery query) {
        if (query instanceof FlatQueryAndNot) {
            Iterator<FlatQuery> i = ( (FlatQueryAndNot) query ).ands().iterator();
            if (!i.hasNext()) {
                buffer.append("*:*");
            } else {
                buildSubQuery(i.next());
                while (i.hasNext()) {
                    buffer.append(" AND ");
                    buildSubQuery(i.next());
                }
            }
            i = ( (FlatQueryAndNot) query ).nots().iterator();
            while (i.hasNext()) {
                buffer.append(" NOT ");
                buildSubQuery(i.next());
            }
        } else if (query instanceof FlatQueryOr) {
            Iterator<FlatQuery> i = ( (FlatQueryOr) query ).ors().iterator();
            buildSubQuery(i.next());
            while (i.hasNext()) {
                buffer.append(" OR ");
                buildSubQuery(i.next());
            }
        } else if (query instanceof FlatQuerySearch) {
            buildSearch((FlatQuerySearch) query);
        } else if (query instanceof FlatQueryNested) {
            buildNested((FlatQueryNested) query);
        } else {
            throw new IllegalStateException("Don't know about: " + query.getClass().getCanonicalName());
        }
    }

    private void buildSubQuery(FlatQuery query) {
        if (query instanceof FlatQueryNested) {
            buildNested((FlatQueryNested) query);
        } else if (query instanceof FlatQuerySearch) {
            buildSearch((FlatQuerySearch) query);
        } else {
            buffer.append('(');
            buildQuery(query);
            buffer.append(')');
        }
    }

    private void buildNested(FlatQueryNested nested) {
        buffer.append(nested.getQuery());
    }

    private void buildSearch(FlatQuerySearch search) {
        String index = search.getIndex();
        if (!index.isEmpty()) {
            buffer.append(index).append(":");
        }
        Position pos = search.getPos();
        String term = search.getTerm();
        FieldSpec fieldSpec = search.getFieldSpec();
        boolean isPhrase = fieldSpec.getType() == SolrConfigFieldType.PHRASE;
        switch (search.getRelation()) {
            case "any":
                if (isPhrase) {
                    throw new CQLException(CQLError.UNSUPPORTED_COMBINATION_OF_RELATION_AND_INDEX, pos);
                }
                multiWords(term, " OR ", pos);
                break;
            case "all":
                if (isPhrase) {
                    throw new CQLException(CQLError.UNSUPPORTED_COMBINATION_OF_RELATION_AND_INDEX, pos);
                }
                multiWords(term, " AND ", pos);
                break;
            case "adj":
                if (isPhrase) {
                    throw new CQLException(CQLError.UNSUPPORTED_COMBINATION_OF_RELATION_AND_INDEX, pos);
                }
                if (SearchTerm.containsMultipleWords(term)) {
                    buffer.append('"');
                    word(term, pos);
                    buffer.append("\"~0");
                } else {
                    word(term, pos);
                }
                break;
            case "=":
                if (isPhrase) {
                    if (!search.getModifiers().isEmpty()) {
                        throw new CQLException(CQLError.UNSUPPORTED_RELATION_MODIFIER, pos);
                    }
                    word(term, pos);
                } else if (search.getModifiers().containsKey("word")) {
                    if (SearchTerm.containsMultipleWords(term)) {
                        buffer.append('"');
                        word(term, pos);
                        buffer.append("\"~9999");
                    } else {
                        word(term, pos);
                    }
                } else if (search.getModifiers().containsKey("string")) {
                    if (SearchTerm.containsMultipleWords(term)) {
                        buffer.append('"')
                                .append(ClientUtils.escapeQueryChars(term))
                                .append("\"~0");
                    } else {
                        buffer.append(ClientUtils.escapeQueryChars(term));

                    }
                } else if (SearchTerm.containsMultipleWords(term)) {
                    if (SearchTerm.containsMaskingOrTruncation(term)) {
                        multiWords(term, " AND ", pos);
                    } else {
                        buffer.append('"');
                        word(term, pos);
                        buffer.append("\"~9999");
                    }
                } else {
                    word(term, pos);
                }
                break;
            case ">":
                buffer.append("{");
                rangeValue(search);
                buffer.append(" TO *]");
                break;
            case "<":
                buffer.append("[* TO ");
                rangeValue(search);
                buffer.append("}");
                break;
            case ">=":
                buffer.append("[");
                rangeValue(search);
                buffer.append(" TO *]");
                break;
            case "<=":
                buffer.append("[* TO ");
                rangeValue(search);
                buffer.append("]");
                break;
            default:
                throw new IllegalStateException("Don't know how to handle " + search.getRelation() + " in query building");
        }
    }

    private void multiWords(String words, String sep, Position pos) {
        System.out.println("words = " + words);
        multiWords(SearchTerm.wordsFrom(words), sep, pos);
    }

    private void multiWords(Iterator<Iterator<String>> words, String sep, Position pos) {
        if (!words.hasNext()) {
            throw new CQLException(CQLError.EMPTY_TERM_UNSUPPORTED, pos);
        }
        buffer.append('(');
        word(words.next(), pos);
        while (words.hasNext()) {
            buffer.append(sep);
            word(words.next(), pos);
        }
        buffer.append(')');
    }

    private void word(String word, Position pos) {
        word(SearchTerm.partsFrom(word), pos);
    }

    private void word(Iterator<String> word, Position pos) {
        String part = word.next();
        buffer.append(ClientUtils.escapeQueryChars(part));
        if (part.isEmpty() && !word.hasNext()) {
            throw new CQLException(CQLError.EMPTY_TERM_UNSUPPORTED, pos);
        }
        while (word.hasNext()) {
            buffer.append(word.next());
            buffer.append(ClientUtils.escapeQueryChars(word.next()));
        }
    }

    private void rangeValue(FlatQuerySearch search) {
        switch (search.getFieldSpec().getType()) {
            case DATE:
            case NUMBER:
                buffer.append(ClientUtils.escapeQueryChars(search.getTerm()));
                break;
            default:
                throw new CQLException(CQLError.UNSUPPORTED_COMBINATION_OF_RELATION_AND_INDEX, search.getPos());
        }
    }

    private String getQuery() {
        return buffer.toString();
    }

}
