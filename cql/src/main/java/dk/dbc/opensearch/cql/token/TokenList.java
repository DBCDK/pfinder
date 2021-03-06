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
package dk.dbc.opensearch.cql.token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class TokenList {

    public static final Map<String, BooleanOpName> BOOLEAN_NAMES = Collections.unmodifiableMap(makeBooleanNameMap());
    public static final Map<String, BooleanOpName> BOOLEAN_NAMES_DK = Collections.unmodifiableMap(makeBooleanNameMapDk());

    private final char[] query;
    private final Set<String> relations;
    private int tokenStartingAt;
    private int chPos;

    private final ArrayList<Token> tokens;
    private int pos;
    private final Map<String, BooleanOpName> booleanNameMap;

    public TokenList(String query, Set<String> relations, Map<String, BooleanOpName> booleanNameMap) {
        this.query = query.toCharArray();
        this.chPos = 0;
        this.tokenStartingAt = 0;
        this.relations = relations;
        this.tokens = new ArrayList<>();
        this.pos = 0;
        this.booleanNameMap = booleanNameMap;
        tokneize();
    }

    public boolean take(List<Token> target, Token.Type... types) {
        ListIterator<Token> i = tokens.listIterator(pos);
        for (Token.Type type : types) {
            if (!( i.hasNext() && i.next().isA(type) )) {
                return false;
            }
        }
        target.clear();
        target.addAll(tokens.subList(pos, pos + types.length));
        pos += types.length;
        return true;
    }

    public int at() {
        return tokens.get(pos).getInputPosition();
    }

    private boolean eof() {
        return chPos >= query.length;
    }

    private char read() {
        return query[chPos++];
    }

    private void unread() {
        --chPos;
    }

    private void tokneize() {
        while (!eof()) {
            tokenStartingAt = chPos;
            char c = read();
            if (Character.isWhitespace(c)) {
                continue;
            }
            switch (c) {
                case '(':
                    tokens.add(new ParL(tokenStartingAt));
                    break;
                case ')':
                    tokens.add(new ParR(tokenStartingAt));
                    break;
                case '/':
                    tokens.add(new Slash(tokenStartingAt));
                    break;
                case '=':
                case '<':
                case '>':
                    tokens.add(readBoolean(c));
                    break;
                case '\'':
                case '"':
                    tokens.add(readQuoted(c));
                    break;
                default:
                    tokens.add(readUnquoted());
                    break;
            }
        }
        tokens.add(new EOL(tokenStartingAt));
    }

    private Token readBoolean(char first) {
        if (!eof()) {
            char second = read();
            if (first == '=' && second == '=') {
                return new CompareOp(CompareOpName.EXACT, "==", tokenStartingAt);
            }
            if (first == '<' && second == '=') {
                return new CompareOp(CompareOpName.LE, "<=", tokenStartingAt);
            }
            if (first == '>' && second == '=') {
                return new CompareOp(CompareOpName.GE, ">=", tokenStartingAt);
            }
            if (first == '<' && second == '>') {
                return new CompareOp(CompareOpName.NE, "<>", tokenStartingAt);
            }
            unread();
        }
        if (first == '=') {
            return new CompareOp(CompareOpName.EQ, "=", tokenStartingAt);
        }
        if (first == '<') {
            return new CompareOp(CompareOpName.LT, "<", tokenStartingAt);
        }
        if (first == '>') {
            return new CompareOp(CompareOpName.GT, ">", tokenStartingAt);
        }
        throw new IllegalStateException("Internal Logic Error");
    }

    private Token readQuoted(char closingChar) {
        int start = chPos;
        for (;;) {
            if (eof()) {
                throw new IllegalStateException("Unterminated quote starting at: " + tokenStartingAt);
            }
            char c = read();
            if (c == closingChar) {
                break;
            }
            if (c == '\\') {
                if (eof()) {
                    throw new IllegalStateException("Dangling backslash at end of input");
                }
                read();
            }
        }
        return new Term(new String(query, tokenStartingAt, chPos - tokenStartingAt),
                        new String(query, start, chPos - 1 - start), tokenStartingAt);
    }

    private Token readUnquoted() {
        int start = chPos - 1;
        for (;;) {
            if (eof()) {
                break;
            }
            char c = read();
            if (Character.isWhitespace(c) ||
                c == '(' || c == ')' ||
                c == '=' || c == '<' || c == '>' ||
                c == '/') {
                unread();
                break;
            }
        }
        String content = new String(query, start, chPos - start);
        String key = content.toLowerCase(Locale.ROOT);
        if (content.equalsIgnoreCase("sortby")) {
            return new SortBy(content, tokenStartingAt);
        }
        if (booleanNameMap.containsKey(key)) {
            return new BooleanOp(booleanNameMap.get(key), content, tokenStartingAt);
        }
        if (relations.contains(key)) {
            return new Relation(content, tokenStartingAt);
        }
        return new Term(content, tokenStartingAt);
    }

    private static HashMap<String, BooleanOpName> makeBooleanNameMap() {
        HashMap<String, BooleanOpName> map = new HashMap<>();
        map.put("and", BooleanOpName.AND);
        map.put("or", BooleanOpName.OR);
        map.put("not", BooleanOpName.NOT);
        map.put("prox", BooleanOpName.PROX);
        return map;
    }

    private static HashMap<String, BooleanOpName> makeBooleanNameMapDk() {
        HashMap<String, BooleanOpName> map = makeBooleanNameMap();
        map.put("og", BooleanOpName.AND);
        map.put("eller", BooleanOpName.OR);
        map.put("ikke", BooleanOpName.NOT);
        return map;
    }

    @Override
    public String toString() {
        return tokens.toString();
    }

}
