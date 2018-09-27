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
import java.util.Collections;
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

    public static final Map<String, Function<Map<String, Modifier>, String>> DEFAULT_RELATIONS = makeDefaultRelations();
    public static final Map<String, Function<Map<String, Modifier>, String>> DEFAULT_BOOLEANS = makeDefaultBooleans();

    private final TokenList tokens;
    private final ArrayList<Token> taken;
    private final String queryString;
    private final Map<String, Function<Map<String, Modifier>, String>> relations;
    private final Map<String, Function<Map<String, Modifier>, String>> booleans;

    public CQLParser(String query) {
        this(query, DEFAULT_RELATIONS);
    }

    public CQLParser(String query, Map<String, Function<Map<String, Modifier>, String>> relations) {
        this(query, relations, DEFAULT_BOOLEANS, TokenList.BOOLEAN_NAMES);
    }

    public CQLParser(String query, Map<String, Function<Map<String, Modifier>, String>> relations, Map<String, Function<Map<String, Modifier>, String>> booleans) {
        this(query, relations, booleans, TokenList.BOOLEAN_NAMES);
    }

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
     * @param query the query string
     * @param relations the rules for compl
     * @param booleans
     * @param booleanNameMap
     */
    public CQLParser(String query, Map<String, Function<Map<String, Modifier>, String>> relations, Map<String, Function<Map<String, Modifier>, String>> booleans, Map<String, BooleanOpName> booleanNameMap) {
        this.queryString = query;
        this.relations = relations;
        this.booleans = booleans;
        this.tokens = new TokenList(query, relations.keySet(), booleanNameMap);
        this.taken = new ArrayList<>(5);
    }

    public Query parse() {
        Query query = parseBoolean();
        if (take(EOL)) {
            return query;
        }
        if (take(SORTBY)) {
            int at = get(1).getInputPosition();
            throw new CQLException(pos(at), "SortBy is not supported");
        }
        if (take(TERM)) {
            int at = get(1).getInputPosition();
            throw new CQLException(pos(at), "Illegal operator");
        }
        throw expected("EOL");
    }

    private Query parseBoolean() { // LEFT PRECEDENCE
        Query left = parseSearch();
        while (take(BOOLEAN)) {
            BooleanOp op = (BooleanOp) get(1);
            Function<Map<String, Modifier>, String> validator = booleans.get(op.getName());
            if (validator == null) {
                throw new CQLException(pos(op.getInputPosition()), "Unsupported boolean operator");
            }
            Map<String, Modifier> modifiers = modifiers(validator);
            Query right = parseSearch();
            left = new Query.Bool(left, op.getName(), modifiers, right);
        }
        return left;
    }

    private Query parseSearch() {
        if (take(PAR_L)) {
            Query query = parseBoolean();
            if (!take(PAR_R)) {
                throw expected(")");
            }
            return query;
        } else if (take(TERM, RELATION)) {
            Term term = (Term) get(1);
            Keyword relation = (Keyword) get(2);
            Function<Map<String, Modifier>, String> validator = relations.get(relation.getName());
            if (validator == null) {
                throw new CQLException(pos(relation.getInputPosition()), "Unsupported relation operator");
            }
            Map<String, Modifier> modifiers = modifiers(validator);
            if (!take(TEXT)) {
                throw expected("search term");
            }
            Text text = (Text) get(1);
            return new Query.Search(term.getContent(), relation.getName(), modifiers, text.getText());
        } else if (take(TEXT)) {
            Text text = (Text) get(1);
            return new Query.Search(text.getText());
        } else {
            throw expected("seach term or parenthesis");
        }
    }

    private Map<String, Modifier> modifiers(Function<Map<String, Modifier>, String> validator) {
        int at = tokens.at();
        if (take(SLASH, TERM)) {
            HashMap<String, Modifier> modifiers = new HashMap<>();
            do {
                Term term = (Term) get(2);
                String modifier = term.getContent();
                String key = modifier.toLowerCase(Locale.ROOT);
                if (modifiers.containsKey(key)) {

                    throw new CQLException(pos(at), "Modifier " + modifier.toLowerCase(Locale.ROOT) + " is repeated");
                }
                if (take(COMPARE, TERM)) {
                    CompareOp cmp = (CompareOp) get(1);
                    Term value = (Term) get(2);
                    modifiers.put(modifier.toLowerCase(Locale.ROOT),
                                  new Modifier(modifier, cmp.getName(), value.getContent()));
                } else {
                    modifiers.put(modifier.toLowerCase(Locale.ROOT),
                                  new Modifier(modifier));
                }
            } while (take(SLASH, TERM));
            String error = validator.apply(modifiers);
            if (error != null) {
                throw new CQLException(pos(at), error);
            }
            return modifiers;
        }
        return EMPTY_MAP;
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

    private CQLException.Position pos(int at) {
        return new CQLException.Position(queryString, at);
    }

    private CQLException expected(String content) {
        return new CQLException(pos(), "Expected " + content);
    }

    private static Map<String, Function<Map<String, Modifier>, String>> makeDefaultRelations() {
        Map<String, Function<Map<String, Modifier>, String>> map = new HashMap();
        map.put("adj", m -> "Modifiers not supported for 'adj'");
        map.put("any", m -> "Modifiers not supported for 'any'");
        map.put("all", m -> "Modifiers not supported for 'all'");
        map.put("=", m -> {
            Modifier string = m.get("string");
            Modifier word = m.get("word");
            if (string != null && word != null) {
                return "Illegal modifiers, cannot have both 'string' and 'word'";
            }
            if (string != null && !string.isFlag()) {
                return "Modifier 'string' does not take an operator";
            }
            if (word != null && !word.isFlag()) {
                return "Modifier 'word' does not take an operator";
            }
            for (String key : m.keySet()) {
                switch (key) {
                    case "string":
                    case "word":
                        break;
                    default:
                        return "Unsupported modifier '" + key + "' does not take an operator";
                }
            }
            return null;
        });
        map.put(">", m -> "Modifiers not supported for >");
        map.put("<", m -> "Modifiers not supported for <");
        map.put(">=", m -> "Modifiers not supported for >=");
        map.put("<=", m -> "Modifiers not supported for <=");
        map.put("<>", m -> "Modifiers not supported for <>");
        map.put("==", null);
        map.put("prox", null);
        map.put("encloses", null);
        map.put("within", null);
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Function<Map<String, Modifier>, String>> makeDefaultBooleans() {
        Map<String, Function<Map<String, Modifier>, String>> map = new HashMap();
        map.put("and", m -> "Modifiers not supported for 'and'");
        map.put("or", m -> "Modifiers not supported for 'or'");
        map.put("not", m -> "Modifiers not supported for 'not'");
        map.put("prox", null);
        return Collections.unmodifiableMap(map);
    }

}
