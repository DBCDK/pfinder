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

import dk.dbc.opensearch.cql.CQLError;
import dk.dbc.opensearch.cql.CQLException;
import dk.dbc.opensearch.cql.CQLException.Position;
import dk.dbc.opensearch.cql.CQLParser;
import dk.dbc.opensearch.cql.QueryNode;
import dk.dbc.opensearch.solr.SolrRules;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class SolrConfig {

    public Map<String, SolrConfigField> fields;
    public Map<String, SolrConfigNested> nested;

    @Override
    public String toString() {
        return "SolrConfig{nested=" + nested + ", fields=" + fields + '}';
    }

    public SolrRules makeSolrRules() {
        return new Rules(this);
    }

    private static class Rules implements SolrRules {

        private static class Entry {

            private final String indexName;
            private final FieldSpec fieldSpec;
            private final boolean internal;

            private Entry(String indexName, FieldSpec fieldSpec, boolean internal) {
                this.indexName = indexName;
                this.fieldSpec = fieldSpec;
                this.internal = internal;
            }

            private String getIndexName() {
                return indexName;
            }

            private FieldSpec getNestedSpec() {
                return fieldSpec;
            }

            private boolean isInternal() {
                return internal;
            }

        }

        private final SolrConfig solrConfig;
        private final SolrRules allowInternalSolrRules;

        private final HashMap<String, Entry> indexes = new HashMap<>();
        private final HashMap<SolrConfigField, FieldSpec> nestedSpecs = new HashMap<>();

        public Rules(SolrConfig solrConfig) {
            this.solrConfig = solrConfig;
            this.allowInternalSolrRules = new SolrRules() {
                @Override
                public String indexName(String index, Position pos) {
                    return Rules.this.indexName(index, pos, true);
                }

                @Override
                public FieldSpec fieldSpec(String index, Position pos) {
                    return Rules.this.fieldSpec(index, pos, true);
                }
            };
            solrConfig.fields.keySet().stream()
                    .filter(k -> !k.endsWith("."))
                    .forEach(k -> indexes.put(k, computeEntry(k)));
        }

        @Override
        public String indexName(String index, Position pos) {
            return indexName(index, pos, false);
        }

        private String indexName(String index, Position pos, boolean allowInternal) {
            Entry entry = indexes.computeIfAbsent(index, this::computeEntry);
            if (entry == null || entry.isInternal() && !allowInternal) {
                throw new CQLException(CQLError.UNSUPPORTED_INDEX, pos);
            }
            return entry.getIndexName();
        }

        @Override
        public FieldSpec fieldSpec(String index, Position pos) {
            return fieldSpec(index, pos, false);
        }

        private FieldSpec fieldSpec(String index, Position pos, boolean allowInternal) {
            Entry entry = indexes.computeIfAbsent(index, this::computeEntry);
            if (entry == null || entry.isInternal() && !allowInternal) {
                throw new CQLException(CQLError.UNSUPPORTED_INDEX, pos);
            }
            return entry.getNestedSpec();
        }

        private Entry computeEntry(String index) {
            SolrConfigField prefixField = null;
            String name = null;
            int dot = index.indexOf('.') + 1;
            if (dot > 0) {
                prefixField = solrConfig.fields.get(index.substring(0, dot));
                if (prefixField == null &&
                    solrConfig.fields.containsKey(index.substring(0, dot))) {
                    prefixField = new SolrConfigField();
                }
                if (prefixField != null) {
                    name = prefixField.solrName == null ? index : prefixField.solrName + index.substring(dot);
                }
            }
            SolrConfigField exactField = solrConfig.fields.get(index);
            if (exactField == null && solrConfig.fields.containsKey(index)) {
                exactField = new SolrConfigField();
            }
            if (exactField != null) {
                name = exactField.solrName == null ? index : exactField.solrName;
            }
            if (name != null) {
                SolrConfigField field = mergeFields(prefixField, exactField);
                FieldSpec spec = nestedSpecs.computeIfAbsent(field, this::makeFieldSpec);
                boolean internal = field.internal;

                return new Entry(name, spec, internal);
            }
            return null;
        }

        private SolrConfigField mergeFields(SolrConfigField prefixField, SolrConfigField exactField) {
            if (exactField == null && prefixField == null) {
                return null;
            }
            if (exactField == null) {
                return new SolrConfigField(prefixField);
            }
            if (prefixField == null) {
                return new SolrConfigField(exactField);
            }
            return new SolrConfigField(exactField, prefixField);
        }

        private FieldSpec makeFieldSpec(SolrConfigField field) {
            QueryNode cql = null;
            String query = "[INTERNAL_SERVER_ERROR NESTED_QUERY]";
            String nestedGroup = field.nestedGroup;
            String filterQuery = field.filterQuery;
            if (nestedGroup != null) {
                SolrConfigNested nested = solrConfig.nested.get(nestedGroup);
                if (nested != null) {
                    query = nested.query;
                    if (nested.cql != null) {
                        cql = CQLParser.parse(nested.cql);
                    }
                    filterQuery = nested.filterQuery; // Override filter query group from nested spec
                }
            }
            return new FieldSpec(allowInternalSolrRules,
                                 nestedGroup, filterQuery,
                                 query, cql, field.type);
        }
    }
}
