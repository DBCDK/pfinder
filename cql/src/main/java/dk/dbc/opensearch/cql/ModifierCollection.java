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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class ModifierCollection extends HashMap<String, Modifier> {

    private static final long serialVersionUID = -2152127882150109255L;

    public ModifierCollection() {
    }

    public static final ModifierCollection EMPTY = new ModifierCollection() {

        private static final long serialVersionUID = 130701394567992974L;

        @Override
        public void clear() {
            throw new IllegalStateException("unmodifiable");
        }

        @Override
        public Modifier computeIfAbsent(String k, Function<? super String, ? extends Modifier> fnctn) {
            throw new IllegalStateException("unmodifiable");
        }

        @Override
        public Modifier merge(String k, Modifier v, BiFunction<? super Modifier, ? super Modifier, ? extends Modifier> bf) {
            throw new IllegalStateException("unmodifiable");
        }

        @Override
        public Modifier put(String k, Modifier v) {
            throw new IllegalStateException("unmodifiable");
        }

        @Override
        public void putAll(Map<? extends String, ? extends Modifier> map) {
            throw new IllegalStateException("unmodifiable");
        }

        @Override
        public Modifier putIfAbsent(String k, Modifier v) {
            throw new IllegalStateException("unmodifiable");
        }

        @Override
        public Modifier remove(Object o) {
            throw new IllegalStateException("unmodifiable");
        }

        @Override
        public boolean remove(Object o, Object o1) {
            throw new IllegalStateException("unmodifiable");
        }

        @Override
        public Modifier replace(String k, Modifier v) {
            throw new IllegalStateException("unmodifiable");
        }

        @Override
        public boolean replace(String k, Modifier v, Modifier v1) {
            throw new IllegalStateException("unmodifiable");
        }

        @Override
        public void replaceAll(BiFunction<? super String, ? super Modifier, ? extends Modifier> bf) {
            throw new IllegalStateException("unmodifiable");
        }
    };

}
