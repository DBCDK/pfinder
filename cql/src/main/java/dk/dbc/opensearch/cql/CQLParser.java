/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-cql
 *
 * opensearch-cql is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-cql is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.cql;

import dk.dbc.opensearch.cql.token.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static dk.dbc.opensearch.cql.token.Token.Type.*;
import static java.util.Collections.*;

/**
 *
 * https://www.loc.gov/standards/sru/cql/
 * <p>
 * Supports single quote strings as well as double quote (outside standard, but
 * single quote doesn't need shift)
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class CQLParser {

    private static final int MAX_BOOLEAN_OPERATORS = 200;
    public static final Map<String, Function<ModifierCollection, CQLError>> DEFAULT_RELATIONS = makeDefaultRelations();
    public static final Map<String, Function<ModifierCollection, CQLError>> DEFAULT_BOOLEANS = makeDefaultBooleans();

    public static QueryNode parse(String query) throws CQLException {
        return new CQLParser(query, DEFAULT_RELATIONS, DEFAULT_BOOLEANS, TokenList.BOOLEAN_NAMES).parse();
    }

    public static QueryNode parse(String query, Map<String, Function<ModifierCollection, CQLError>> relations) throws CQLException {
        return new CQLParser(query, relations, DEFAULT_BOOLEANS, TokenList.BOOLEAN_NAMES).parse();
    }

    public static QueryNode parse(String query, Map<String, Function<ModifierCollection, CQLError>> relations, Map<String, Function<ModifierCollection, CQLError>> booleans) throws CQLException {
        return new CQLParser(query, relations, booleans, TokenList.BOOLEAN_NAMES).parse();
    }

    public static QueryNode parse(String query, Map<String, Function<ModifierCollection, CQLError>> relations, Map<String, Function<ModifierCollection, CQLError>> booleans, Map<String, BooleanOpName> booleanNameMap) throws CQLException {
        return new CQLParser(query, relations, booleans, booleanNameMap).parse();
    }

    private final TokenList tokens;
    private final ArrayList<Token> taken;
    private final String queryString;
    private final Map<String, Function<ModifierCollection, CQLError>> relations;
    private final Map<String, Function<ModifierCollection, CQLError>> booleans;
    private int booleanOperatorCount;

    /**
     * Make a CQL parser for a given string
     * <p>
     * The validators and booleans, are used to validate/allow search and
     * boolean operators.
     * <p>
     * The map key is the name of the operator (lowercased)
     * If the function is null, the the operator is unsupported
     * <p>
     * The function is given a map there the key is modifier name as lower case
     * and the value is a modifier object.
     * <p>
     * The function return the error message, or null if no error occurred
     *
     * @param query          the query string
     * @param relations      the rules for compl
     * @param booleans
     * @param booleanNameMap
     */
    private CQLParser(String query, Map<String, Function<ModifierCollection, CQLError>> relations, Map<String, Function<ModifierCollection, CQLError>> booleans, Map<String, BooleanOpName> booleanNameMap) {
        this.queryString = query;
        this.relations = relations;
        this.booleans = booleans;
        this.tokens = new TokenList(query, relations.keySet(), booleanNameMap);
        this.taken = new ArrayList<>(5);
        this.booleanOperatorCount = 0;
    }

    private void countBooleanOperator(Token at) {
        if(++this.booleanOperatorCount >= MAX_BOOLEAN_OPERATORS)
            throw new CQLException(CQLError.TOO_MANY_BOOLEAN_OPERATORS_IN_QUERY, pos(at), "Too complex query");
    }

    private QueryNode parse() {
        QueryNode query = parseBoolean();
        if (take(EOL)) {
            return query;
        }
        if (take(SORTBY)) {
            int at = get(1).getInputPosition();
            throw new CQLException(CQLError.SORT_NOT_SUPPORTED, pos(at), "SortBy is not supported");
        }
        if (take(TERM)) {
            int at = get(1).getInputPosition();
            throw new CQLException(CQLError.QUERY_SYNTAX_ERROR, pos(at), "Illegal operator");
        }
        throw expected(CQLError.QUERY_SYNTAX_ERROR, "EOL");
    }

    private QueryNode parseBoolean() { // LEFT PRECEDENCE
        QueryNode left = parseSearch();
        while (take(BOOLEAN)) {
            BooleanOp bool = (BooleanOp) get(1);
            countBooleanOperator(bool);
            Function<ModifierCollection, CQLError> validator = booleans.get(bool.getName());
            if (validator == null) {
                throw new CQLException(CQLError.UNSUPPORTED_BOOLEAN_OPERATOR, pos(bool.getInputPosition()), "Unsupported boolean operator");
            }
            ModifierCollection modifiers = modifiers(validator);
            QueryNode right = parseSearch();
            left = new BoolQuery(left, pos(bool), bool.getOperator(), modifiers, right);
        }
        return left;
    }

    private QueryNode parseSearch() {
        if (take(PAR_L)) {
            QueryNode query = parseBoolean();
            if (!take(PAR_R)) {
                throw expected(CQLError.INVALID_OR_UNSUPPORTED_USE_OF_PARENTHESES, ")");
            }
            return query;
        } else if (take(TERM, RELATION)) {
            Term term = (Term) get(1);
            Keyword relation = (Keyword) get(2);
            Function<ModifierCollection, CQLError> validator = relations.get(relation.getName());
            if (validator == null) {
                throw new CQLException(CQLError.UNSUPPORTED_RELATION, pos(relation.getInputPosition()), "Unsupported relation operator");
            }
            ModifierCollection modifiers = modifiers(validator);
            if (take(PAR_L)) {
                return parseSearchCCLExtensionGroup(term.getContent(), relation.getName(), modifiers);
            }
            if (!take(TEXT)) {
                throw expected(CQLError.QUERY_SYNTAX_ERROR, "search term");
            }
            Text text = (Text) get(1);
            return new SearchQuery(pos(term), term.getContent(), relation.getName(), modifiers, text.getText());
        } else if (take(TEXT)) {
            Text text = (Text) get(1);
            return new SearchQuery(pos(text), text.getText());
        } else {
            throw expected(CQLError.QUERY_SYNTAX_ERROR, "seach term or parenthesis");
        }
    }

    private QueryNode parseSearchCCLExtensionGroup(String term, String relation, ModifierCollection modifiers) {
        QueryNode left = parseSearchCCLExtenstionValue(term, relation, modifiers);
        while (take(BOOLEAN)) {
            BooleanOp bool = (BooleanOp) get(1);
            countBooleanOperator(bool);
            if (bool.getOperator() == BooleanOpName.PROX) {
                throw new CQLException(CQLError.PROXIMITY_NOT_SUPPORTED, pos(), "Unsupported operator prox");
            }
            QueryNode right = parseSearchCCLExtenstionValue(term, relation, modifiers);
            left = new BoolQuery(left, pos(bool), bool.getOperator(), ModifierCollection.EMPTY, right);
        }
        if (!take(PAR_R)) {
            throw expected(CQLError.INVALID_OR_UNSUPPORTED_USE_OF_PARENTHESES, ")");
        }
        return left;
    }

    private QueryNode parseSearchCCLExtenstionValue(String term, String relation, ModifierCollection modifiers) {
        if (take(PAR_L)) {
            return parseSearchCCLExtensionGroup(term, relation, modifiers);
        } else if (take(TEXT)) {
            Text text = (Text) get(1);
            return new SearchQuery(pos(text), term, relation, modifiers, text.getText());
        } else {
            throw expected(CQLError.QUERY_SYNTAX_ERROR, "search term");
        }
    }

    private ModifierCollection modifiers(Function<ModifierCollection, CQLError> validator) {
        int at = tokens.at();
        if (take(SLASH, TERM)) {
            ModifierCollection modifiers = new ModifierCollection();
            do {
                Term term = (Term) get(2);
                String modifier = term.getContent();
                String key = modifier.toLowerCase(Locale.ROOT);
                if (modifiers.containsKey(key)) {
                    throw new CQLException(CQLError.QUERY_SYNTAX_ERROR, pos(at), "Modifier " + key + " is repeated");
                }
                if (take(COMPARE, TERM)) {
                    CompareOp cmp = (CompareOp) get(1);
                    Term value = (Term) get(2);
                    modifiers.put(modifier.toLowerCase(Locale.ROOT),
                                  new Modifier(pos(term), modifier, cmp.getName(), value.getContent()));
                } else {
                    modifiers.put(modifier.toLowerCase(Locale.ROOT),
                                  new Modifier(pos(term), modifier));
                }
            } while (take(SLASH, TERM));
            CQLError error = validator.apply(modifiers);
            if (error != null) {
                throw new CQLException(error, pos(at));
            }
            return modifiers;
        }
        return ModifierCollection.EMPTY;
    }

    /**
     * Match tokens
     *
     * @param types Token types to match
     * @return if they've been matched, and tokens are available
     */
    private boolean take(Token.Type... types) {
        return tokens.take(taken, types);
    }

    /**
     * One indexed getter
     *
     * @param i offset
     * @return matched token
     */
    private Token get(int i) {
        return taken.get(i - 1);
    }

    private CQLException.Position pos() {
        return pos(tokens.at());
    }

    private CQLException.Position pos(Token token) {
        return pos(token.getInputPosition());
    }
    private CQLException.Position pos(int at) {
        return new CQLException.Position(queryString, at);
    }

    private CQLException expected(CQLError error, String content) {
        return new CQLException(error, pos(), "Expected " + content);
    }

    private static Map<String, Function<ModifierCollection, CQLError>> makeDefaultRelations() {
        Map<String, Function<ModifierCollection, CQLError>> map = new HashMap();
        map.put("adj", m -> CQLError.UNSUPPORTED_RELATION_MODIFIER);
        map.put("any", m -> CQLError.UNSUPPORTED_RELATION_MODIFIER);
        map.put("all", m -> CQLError.UNSUPPORTED_RELATION_MODIFIER);
        map.put("=", m -> {
            Modifier string = m.get("string");
            Modifier word = m.get("word");
            if (string != null && word != null) {
                return CQLError.UNSUPPORTED_COMBINATION_OF_RELATION_MODIFERS;
            }
            if (string != null && !string.isFlag()) {
                return CQLError.UNSUPPORTED_RELATION_MODIFIER;
            }
            if (word != null && !word.isFlag()) {
                return CQLError.UNSUPPORTED_RELATION_MODIFIER;
            }
            for (String key : m.keySet()) {
                switch (key) {
                    case "string":
                    case "word":
                        break;
                    default:
                        return CQLError.UNSUPPORTED_RELATION_MODIFIER;
                }
            }
            return null;
        });
        map.put(">", m -> CQLError.UNSUPPORTED_RELATION_MODIFIER);
        map.put("<", m -> CQLError.UNSUPPORTED_RELATION_MODIFIER);
        map.put(">=", m -> CQLError.UNSUPPORTED_RELATION_MODIFIER);
        map.put("<=", m -> CQLError.UNSUPPORTED_RELATION_MODIFIER);
        map.put("<>", null);
        map.put("==", null);
        map.put("prox", null);
        map.put("encloses", null);
        map.put("within", null);
        return unmodifiableMap(map);
    }

    private static Map<String, Function<ModifierCollection, CQLError>> makeDefaultBooleans() {
        Map<String, Function<ModifierCollection, CQLError>> map = new HashMap();
        map.put("and", m -> CQLError.UNSUPPORTED_BOOLEAN_MODIFIER);
        map.put("or", m -> CQLError.UNSUPPORTED_BOOLEAN_MODIFIER);
        map.put("not", m -> CQLError.UNSUPPORTED_BOOLEAN_MODIFIER);
        map.put("prox", null);
        return unmodifiableMap(map);
    }

}
