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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.opensearch.cql.token.TokenListTest;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@RunWith(Parameterized.class)
public class CQLParserTest {

    private static final ObjectMapper O = new ObjectMapper();
    private static final HashMap<String, Function<Map<String, Modifier>, String>> VALIDATORS = makeValidators();

    private final String name;
    private final String query;
    private final String expected;
    private final String exception;

    public CQLParserTest(String name, String query, String expected, String exception) {
        this.name = name;
        this.query = query;
        this.expected = expected;
        this.exception = exception;
    }

    @Test(timeout = 100L)
    public void testCase() throws Exception {
        System.out.println(name);
        try {
            QueryNode result = new CQLParser(query, VALIDATORS).parse();
            String actual = result.toString();
            System.out.println("actual = " + actual);
            System.out.println("expected = " + expected);
            if (expected == null) {
                fail("Expected exception");
            } else {
                assertThat(actual, is(expected));
            }
        } catch (Exception e) {
            System.out.println("exception = " + e.getMessage());
            if (exception != null && !exception.isEmpty()) {
                assertThat(e.getMessage(), containsString(exception));
            } else {
                throw e;
            }
        }
    }

    private static  HashMap<String, Function<Map<String, Modifier>, String>> makeValidators() {
        HashMap<String, Function<Map<String, Modifier>, String>> validators = new HashMap<>(CQLParser.DEFAULT_RELATIONS);
        validators.put("test1", m -> null);
        validators.put("test2", m -> null);
        return validators;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> tests() throws Exception {
        URL base = TokenListTest.class.getClassLoader().getResource("parser");
        return Arrays.stream(new File(base.toURI()).listFiles())
                .filter(f -> f.getName().endsWith(".json"))
                .sorted()
                .flatMap(f -> testsFromFile(f).stream())
                .collect(Collectors.toList());
    }

    private static List<Object[]> testsFromFile(File file) {
        String fileName = file.getName();
        String testName = fileName.substring(0, fileName.length() - 5);
        try (InputStream is = new FileInputStream(file)) {
            JsonNode tree = O.readTree(is);
            if (!tree.isArray()) {
                return Collections.singletonList(test(testName, tree));
            }
            ArrayNode a = (ArrayNode) tree;
            AtomicInteger i = new AtomicInteger(1);
            return StreamSupport.stream(a.spliterator(), false)
                    .map(n -> test(testName + ":" + i.getAndIncrement(), n))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object[] test(String title, JsonNode node) {
        return new Object[] {
            title,
            value(node, "query"),
            value(node, "expected"),
            value(node, "exception")
        };
    }

    private static String value(JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value == null) {
            return null;
        }
        return value.asText(null);
    }

}
