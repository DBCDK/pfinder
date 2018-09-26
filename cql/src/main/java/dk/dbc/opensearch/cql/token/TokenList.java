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
    private int at;
    private int chPos;

    private final ArrayList<Token> tokens;
    private int pos;
    private final Map<String, BooleanOpName> booleanNameMap;

    public TokenList(String query, Set<String> relations, Map<String, BooleanOpName> booleanNameMap) {
        this.query = query.toCharArray();
        this.chPos = 0;
        this.at = 0;
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

    private char getch() {
        return query[chPos++];
    }

    private void ungetch() {
        --chPos;
    }

    private void tokneize() {
        while (!eof()) {
            at = chPos;
            char c = getch();
            if (Character.isWhitespace(c)) {
                continue;
            }
            switch (c) {
                case '(':
                    tokens.add(new ParL(at));
                    break;
                case ')':
                    tokens.add(new ParR(at));
                    break;
                case '/':
                    tokens.add(new Slash(at));
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
        tokens.add(new EOL(at));
    }

    private Token readBoolean(char first) {
        if (!eof()) {
            char second = getch();
            if (first == '=' && second == '=') {
                return new CompareOp(CompareOpName.EXACT, "==", at);
            }
            if (first == '<' && second == '=') {
                return new CompareOp(CompareOpName.LE, "<=", at);
            }
            if (first == '>' && second == '=') {
                return new CompareOp(CompareOpName.GE, ">=", at);
            }
            if (first == '<' && second == '>') {
                return new CompareOp(CompareOpName.NE, "<>", at);
            }
            ungetch();
        }
        if (first == '=') {
            return new CompareOp(CompareOpName.EQ, "=", at);
        }
        if (first == '<') {
            return new CompareOp(CompareOpName.LT, "<", at);
        }
        if (first == '>') {
            return new CompareOp(CompareOpName.GT, ">", at);
        }
        throw new IllegalStateException("Internal Logic Error");
    }

    private Token readQuoted(char closingChar) {
        int start = chPos;
        for (;;) {
            if (eof()) {
                throw new IllegalStateException("Unterminated quote starting at: " + at);
            }
            char c = getch();
            if (c == closingChar) {
                break;
            }
            if (c == '\\') {
                if (eof()) {
                    throw new IllegalStateException("Dangling backslash at end of input");
                }
                getch();
            }
        }
        return new Term(new String(query, at, chPos - at),
                        new String(query, start, chPos - 1 - start), at);
    }

    private Token readUnquoted() {
        int start = chPos - 1;
        for (;;) {
            if (eof()) {
                break;
            }
            char c = getch();
            if (Character.isWhitespace(c) ||
                c == '(' || c == ')' ||
                c == '=' || c == '<' || c == '>' ||
                c == '/') {
                ungetch();
                break;
            }
        }
        String content = new String(query, start, chPos - start);
        String key = content.toLowerCase(Locale.ROOT);
        if (content.equalsIgnoreCase("sortby")) {
            return new SortBy(content, at);
        }
        if (BOOLEAN_NAMES.containsKey(key)) {
            return new BooleanOp(booleanNameMap.get(key), content, at);
        }
        if (relations.contains(content.toLowerCase(Locale.ROOT))) {
            return new Relation(content, at);
        }
        return new Term(content, at);
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
