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
package dk.dbc.opensearch.solr.resultset;

import dk.dbc.opensearch.solr.SolrQueryFields;
import dk.dbc.opensearch.solr.SolrRules;
import dk.dbc.opensearch.solr.profile.Profiles;
import dk.dbc.opensearch.utils.StatisticsRecorder;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Matchers;

import static dk.dbc.testutil.JsonTester.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@RunWith(Parameterized.class)
public class ResultSetTest {

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> tests() throws Exception {
        ClassLoader classLoader = ResultSetTest.class.getClassLoader();
        SolrRules solrRules = solrRules("result-set");
        Profiles profiles = Profiles.from(solrRules, classLoader.getResourceAsStream("result-set/default.profile"));
        return jsonTests("result-set",
                         (title, map) -> new Object[] {
                             title,
                             map.get("query"),
                             map.getOrDefault("allObjects", false),
                             map.getOrDefault("start", 1),
                             map.getOrDefault("step", 10),
                             map.get("solr"),
                             map.get("works"),
                             solrRules,
                             profiles
                         });
    }
    private final String name;
    private final String query;
    private final boolean allObjects;
    private final int start;
    private final int step;
    private final Map solr;
    private final List<Map> works;
    private final SolrRules solrRules;
    private final Profiles profiles;

    public ResultSetTest(String name, String query, boolean allObjects, int start, int step, Map solr, List works, SolrRules solrRules, Profiles profiles) {
        this.name = name;
        this.query = query;
        this.allObjects = allObjects;
        this.start = start;
        this.step = step;
        this.solr = solr;
        this.works = works;
        this.solrRules = solrRules;
        this.profiles = profiles;
    }

    @Test(timeout = 2_000L)
    public void testCase() throws Exception {
        System.out.println(name);

        SolrQueryFields queryFields = SolrQueryFields.fromCQL(solrRules, query, profiles.getProfile(Arrays.asList("test")));
        ResultSet resultSet = new ResultSetWork(queryFields, allObjects, false);

        List<String> workList = resultSet.fetchWorks(mockedClient(solr), new StatisticsRecorder(),
                                                     start, step, "tracking-id-test");

        System.out.println("workList = " + workList);
        // Check work order
        assertThat(workList, contains(works.stream().map(w -> w.get("work")).toArray(String[]::new)));
        for (Map work : works) {
            Object workId = work.get("work");
            System.out.println("Checking work: " + workId);
            List<String> unitList = resultSet.unitsForWork((String) workId);
            System.out.println(" unitList = " + unitList);
            List<Map> units = (List<Map>) work.get("units");
            // Check unit order in work
            assertThat(unitList, contains(units.stream().map(w -> w.get("unit")).toArray(String[]::new)));
            for (Map unit : units) {
                String unitId = (String) unit.get("unit");
                System.out.println(" Checking unit: " + unitId);
                Set<String> manifestationsSet = resultSet.manifestationsForUnit(unitId);
                System.out.println("  manifestationsSet = " + manifestationsSet);
                List manifestations = (List) unit.get("manifestations");
                assertThat(manifestationsSet, is(new HashSet<>(manifestations)));
            }
        }
    }

    public SolrClient mockedClient(Map<String, Map> responses) throws SolrServerException, IOException {
        SolrClient client = mock(SolrClient.class);
        when(client.query(Matchers.any(SolrParams.class), Matchers.any(SolrRequest.METHOD.class)))
                .thenAnswer(i -> {
                    SolrQuery q = (SolrQuery) i.getArguments()[0];
                    Map json = responses.get(q.getQuery());
                    if (json == null) {
                        String msg = "Query: " + q.getQuery() + " is not defined";
                        System.out.println(msg);
                        throw new RuntimeException(msg);
                    }
                    QueryResponse resp = mock(QueryResponse.class);
                    when(resp.getStatus()).thenReturn(0); // Always OK
                    SolrDocumentList docs = new SolrDocumentList();
                    when(resp.getResults()).thenReturn(docs);
                    List<Map<String, Object>> records = (List<Map<String, Object>>) json.get("records");
                    docs.setStart((long) (int) json.getOrDefault("start", 1));
                    docs.setNumFound((long) (int) json.getOrDefault("numFound", records.size()));
                    records.forEach(record -> docs.add(new SolrDocument(record)));
                    HashMap<String, String> explainMap = new HashMap<>();
                    records.forEach(record -> explainMap.put((String) record.getOrDefault("id", "what!"), "no explain"));
                    when(resp.getExplainMap()).thenReturn(explainMap);
                    return resp;
                });
        return client;
    }
}
