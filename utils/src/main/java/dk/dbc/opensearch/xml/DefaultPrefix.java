/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-utils
 *
 * opensearch-utils is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-utils is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morten Bøgeskov <mb@dbc.dk>
 */
public class DefaultPrefix {

    private static final Logger log = LoggerFactory.getLogger(DefaultPrefix.class);

    private final Map<String, String> namespaceToPrefix;

    public DefaultPrefix(Map<String, String> prefixToNamespace) {
        this.namespaceToPrefix = prefixToNamespace.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        Map.Entry::getKey));
    }

    public Instance instance() {
        return new Instance(namespaceToPrefix);
    }

    public static class Instance {

        private final Map<String, String> defaultNamespaceToPrefix;
        private final Map<String, String> namespaceToPrefix;
        private int undefinedPrefixNumber = 0;

        private Instance(Map<String, String> defaultNamespaceToPrefix) {
            this.defaultNamespaceToPrefix = defaultNamespaceToPrefix;
            this.namespaceToPrefix = new HashMap<>();
        }

        public String prefixFor(String uri) {
            if(uri.isEmpty())
                return "";
            return namespaceToPrefix.computeIfAbsent(uri, this::findPrefix);
        }

        public Map<String, String> prefixesUsed() {
            return namespaceToPrefix.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getValue,
                            Map.Entry::getKey));
        }

        private String findPrefix(String uri) {
            String prefix = defaultNamespaceToPrefix.get(uri);
            if (prefix != null)
                return prefix;
            log.error("Prefix undeclared for namespace: {}", uri);
            return "ns" + ( ++undefinedPrefixNumber );
        }
    }
}
