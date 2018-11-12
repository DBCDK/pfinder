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
package dk.dbc.opensearch.solr;

import dk.dbc.opensearch.solr.flatquery.FlatQuery;
import dk.dbc.opensearch.cql.CQLParser;
import dk.dbc.opensearch.cql.QueryNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static dk.dbc.testutil.JsonTester.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@RunWith(Parameterized.class)
public class FilterQueryTest {

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> tests() throws Exception {
        SolrRules solrRules = solrRules("filter-query");
        return jsonTests("filter-query",
                         (title, map) -> new Object[] {
                             title,
                             map.get("query"),
                             toList(map.get("expected")),
                             map.get("exception"),
                             solrRules
                         });
    }

    private final String name;
    private final String query;
    private final List<Object> expected;
    private final String exception;
    private final SolrRules solrRules;

    public FilterQueryTest(String name, String query, List<Object> expected, String exception, SolrRules solrRules) {
        this.name = name;
        this.query = query;
        this.expected = expected;
        this.exception = exception;
        this.solrRules = solrRules;
    }

    @Test(timeout = 100L)
    public void testCase() throws Exception {
        System.out.println(name);
        try {
            QueryNode cql = CQLParser.parse(query);
            FlatQuery flatQuery = FlatQuery.from(solrRules, cql);
            List<FlatQuery> nestedQueries = NestedQueries.from(flatQuery);
            List<FlatQuery> filterQueries = FilterQuery.from(flatQuery);

            List<String> queries = new ArrayList<>();
            queries.add(QueryBuilder.queryFrom(flatQuery));
            nestedQueries.stream()
                    .map(QueryBuilder::queryFrom)
                    .forEach(queries::add);
            List<Object> actual = Arrays.asList(
                    queries,
                    filterQueries.stream()
                    .map(QueryBuilder::queryFrom)
                    .collect(Collectors.toList()));
            System.out.println("query = " + query);
            System.out.println("cql = " + cql);
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

}
