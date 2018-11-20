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
package dk.dbc.testutil;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dk.dbc.opensearch.solr.SolrRules;
import dk.dbc.opensearch.solr.config.SolrConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class JsonTester {

    private static final ObjectMapper O = newObjectMapper();

    private static ObjectMapper newObjectMapper() {
        ObjectMapper o = new ObjectMapper();
        o.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        return o;
    }

    public static List<Object[]> jsonTests(String folder, BiFunction<String, Map, Object[]> func) throws Exception {
        URL base = JsonTester.class.getClassLoader().getResource(folder);
        return Arrays.stream(new File(base.toURI()).listFiles())
                .filter(f -> f.getName().endsWith(".json"))
                .sorted()
                .flatMap(f -> testsFromFile(f, func).stream())
                .collect(Collectors.toList());
    }

    public static SolrRules solrRules(String folder) throws IOException {
        try (InputStream is = JsonTester.class.getClassLoader().getResourceAsStream(
                 folder + "/solr-spec.yml")) {
            System.out.println("is = " + is);
            ObjectMapper o = new YAMLMapper();
            SolrConfig config = o.readValue(is, SolrConfig.class);
            return config.makeSolrRules();
        }
    }
    public static SolrRules defaultSolrRules() throws IOException {
        try (InputStream is = JsonTester.class.getClassLoader().getResourceAsStream(
                "solr-spec.yml")) {
            ObjectMapper o = new YAMLMapper();
            SolrConfig config = o.readValue(is, SolrConfig.class);
            return config.makeSolrRules();
        }
    }

    public static Object toList(Object obj) {
        if (obj instanceof List) {
            return obj;
        }
        return Collections.singletonList(obj);
    }

    private static List<Object[]> testsFromFile(File file, BiFunction<String, Map, Object[]> func) {
        String fileName = file.getName();
        String testName = fileName.substring(0, fileName.length() - 5);
        try (InputStream is = new FileInputStream(file)) {
            JsonNode tree = O.readTree(is);
            if (!tree.isArray()) {
                Map m = nodeToMap(tree);
                return Collections.singletonList(func.apply(testName(testName, m), m));
            }
            ArrayNode a = (ArrayNode) tree;
            AtomicInteger i = new AtomicInteger(1);
            return StreamSupport.stream(a.spliterator(), false)
                    .map(JsonTester::nodeToMap)
                    .map(m -> func.apply(testName(testName + ":" + i.getAndIncrement(), m), m))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String testName(String title, Map map) {
        Object extra = map.get("title");
        if (extra == null) {
            return title;
        }
        return title + "(" + extra + ")";
    }

    private static Map nodeToMap(JsonNode tree) {
        try {
            return O.readValue(O.writeValueAsString(tree), Map.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
