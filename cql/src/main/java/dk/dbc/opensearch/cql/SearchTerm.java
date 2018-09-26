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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class SearchTerm {

    private final List<String> parts;

    public SearchTerm(String text) {
        List<String> build = null;
        char[] chars = text.toCharArray();
        int pos = 0;
        StringBuilder sb = new StringBuilder();
        while (pos < chars.length) {
            char c = chars[pos++];
            switch (c) {
                case '?':
                case '*':
                    if (build == null) {
                        build = new ArrayList<>();
                    }
                    build.add(sb.toString());
                    sb = new StringBuilder();
                    int start = pos - 1;
                    while (pos < chars.length && ( chars[pos] == '?' || chars[pos] == '*' )) {
                        pos++;
                    }
                    build.add(new String(chars, start, pos - start));
                    break;
                case '\\':
                    sb.append(chars[pos++]);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        if (build == null) {
            build = Collections.singletonList(sb.toString());
        } else {
            build.add(sb.toString());
        }
        this.parts = build;
    }

    /**
     * Get the parts of a search term
     * <p>
     * There's always an odd number of parts.
     * The even positions (0,2,4,...) are literal, the odd (1,3,...) are
     * masking/truncation.
     * <p>
     * The even positions can be empty.
     *
     * @return list of string parts
     */
    public List<String> getParts() {
        return parts;
    }

    @Override
    public String toString() {
        if (parts.size() == 1) {
            return parts.get(0);
        } else {
            return parts.toString();
        }
    }
}
