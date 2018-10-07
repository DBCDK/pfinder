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
            Profile profile = profiles.getProfile("opac");
            System.out.println("profile = " + profile);

            assertThat(profile.hasRelation("870971-anmeld", "dbcaddi:isReviewOf"), is(true));
            assertThat(profile.hasRelation("870971-anmeld", "dbcaddi:hasReviewOf"), is(false));
        }
    }

    @Test(timeout = 1_000L)
    public void testRelations() throws Exception {
        System.out.println("testRelations");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("profiles/777777.json")) {
            Profiles profiles = Profiles.from(solrRules, is);
            Profile profile = profiles.getProfile("example");
            System.out.println("profile = " + profile);

            String searchFilter = QueryBuilder.queryFrom(profile.getSearchFilterQuery());
            String relationFilter = QueryBuilder.queryFrom(profile.getRelationFilterQuery());
            System.out.println("searchFilter = " + searchFilter);
            System.out.println("relationFilter = " + relationFilter);

            assertThat(searchFilter, containsString("100001\\-a"));
            assertThat(searchFilter, not(containsString("100001\\-b")));
            assertThat(searchFilter, not(is(profile.getRelationFilterQuery())));
            assertThat(relationFilter, containsString("100002\\-b"));
        }
    }

}
