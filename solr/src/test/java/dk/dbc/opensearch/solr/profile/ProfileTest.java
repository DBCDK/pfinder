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
package dk.dbc.opensearch.solr.profile;

import dk.dbc.opensearch.solr.QueryBuilder;
import dk.dbc.opensearch.solr.SolrRules;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.Test;

import static dk.dbc.testutil.JsonTester.solrRules;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class ProfileTest {

    private final SolrRules solrRules;

    public ProfileTest() throws Exception {
        this.solrRules = solrRules("profiles");
    }

    @Test(timeout = 1_000L)
    public void testRelationNames() throws Exception {
        System.out.println("testRelationNames");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("profiles/777777.json")) {
            Profiles profiles = Profiles.from(solrRules, is);
            Profile profileA = profiles.getProfile(Arrays.asList("a"));
            System.out.println("profileA = " + profileA);
            Profile profileB = profiles.getProfile(Arrays.asList("b"));
            System.out.println("profileB = " + profileB);
            Profile profileAB = profiles.getProfile(Arrays.asList("a", "b"));
            System.out.println("profileAB = " + profileAB);

            assertThat(profileA.hasRelation("100000", "dbcaddi:isSomething"), is(true));
            assertThat(profileA.hasRelation("100000", "dbcaddi:isSomethingElse"), is(false));
            assertThat(profileA.hasRelation("100000", "dbcaddi:hasSomethingElse"), is(false));

            assertThat(profileB.hasRelation("100000", "dbcaddi:isSomething"), is(false));
            assertThat(profileB.hasRelation("100000", "dbcaddi:isSomethingElse"), is(true));
            assertThat(profileB.hasRelation("100000", "dbcaddi:hasSomethingElse"), is(false));

            assertThat(profileAB.hasRelation("100000", "dbcaddi:isSomething"), is(true));
            assertThat(profileAB.hasRelation("100000", "dbcaddi:isSomethingElse"), is(true));
            assertThat(profileAB.hasRelation("100000", "dbcaddi:hasSomethingElse"), is(false));
        }
    }

    @Test(timeout = 1_000L)
    public void testFilterQueriesProfileA() throws Exception {
        System.out.println("testFilterQueriesProfileA");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("profiles/777777.json")) {
            Profiles profiles = Profiles.from(solrRules, is);
            Profile profile = profiles.getProfile(Arrays.asList("a"));
            String searchFilter = QueryBuilder.queryFrom(profile.getSearchFilterQuery());
            System.out.println("searchFilter = " + searchFilter);
            String relationFilter = QueryBuilder.queryFrom(profile.getRelationFilterQuery());
            System.out.println("relationFilter = " + relationFilter);

            assertThat(searchFilter, containsString(":100001\\-a"));
            assertThat(searchFilter, not(containsString(":100000")));

            assertThat(relationFilter, containsString(":100001\\-a"));
            assertThat(relationFilter, containsString(":100000"));
        }
    }

    @Test(timeout = 1_000L)
    public void testFilterQueriesProfileB() throws Exception {
        System.out.println("testFilterQueriesProfileB");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("profiles/777777.json")) {
            Profiles profiles = Profiles.from(solrRules, is);
            Profile profile = profiles.getProfile(Arrays.asList("b"));
            String searchFilter = QueryBuilder.queryFrom(profile.getSearchFilterQuery());
            System.out.println("searchFilter = " + searchFilter);
            String relationFilter = QueryBuilder.queryFrom(profile.getRelationFilterQuery());
            System.out.println("relationFilter = " + relationFilter);

            assertThat(searchFilter, containsString(":100001\\-b"));
            assertThat(searchFilter, not(containsString(":100000")));

            assertThat(relationFilter, containsString(":100001\\-b"));
            assertThat(relationFilter, containsString(":100000"));
        }
    }

    @Test(timeout = 1_000L)
    public void testFilterQueriesProfileAB() throws Exception {
        System.out.println("testFilterQueriesProfileAB");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("profiles/777777.json")) {
            Profiles profiles = Profiles.from(solrRules, is);
            Profile profile = profiles.getProfile(Arrays.asList("a", "b"));
            String searchFilter = QueryBuilder.queryFrom(profile.getSearchFilterQuery());
            System.out.println("searchFilter = " + searchFilter);
            String relationFilter = QueryBuilder.queryFrom(profile.getRelationFilterQuery());
            System.out.println("relationFilter = " + relationFilter);

            assertThat(searchFilter, containsString(":100001\\-a"));
            assertThat(searchFilter, containsString(":100001\\-b"));
            assertThat(searchFilter, not(containsString(":100000")));

            assertThat(relationFilter, containsString(":100001\\-a"));
            assertThat(relationFilter, containsString(":100001\\-b"));
            assertThat(relationFilter, containsString(":100000"));
        }
    }
}
