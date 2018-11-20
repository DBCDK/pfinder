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

import dk.dbc.opensearch.solr.profile.Profiles;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import org.junit.Test;

import static dk.dbc.testutil.JsonTester.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class SolrQueryFieldsTest {

    @Test(timeout = 2_000L)
    public void testCase() throws Exception {
        System.out.println("testCase");

        SolrRules solrRules = solrRules("profiles");
        Profiles profiles = readProfiles(solrRules);

        SolrQueryFields solrQueryFields = SolrQueryFields.fromCQL(solrRules, "hello AND agency=777777", profiles.getProfile(Arrays.asList("a")));
        System.out.println("solrQueryFields = " + solrQueryFields);

        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(solrQueryFields);
            oos.flush();
            bos.flush();
            bytes = bos.toByteArray();
        }
        SolrQueryFields solrQueryFieldsAfter;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes) ;
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            solrQueryFieldsAfter = (SolrQueryFields) ois.readObject();
        }
        System.out.println("solrQueryFieldsAfter = " + solrQueryFieldsAfter);
        assertThat(solrQueryFieldsAfter.toString(), is(solrQueryFields.toString()));
    }

    public Profiles readProfiles(SolrRules solrRules) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("profiles/777777.json")) {
            return Profiles.from(solrRules, is);
        }
    }
}
