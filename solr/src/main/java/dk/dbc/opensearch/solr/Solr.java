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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;

/**
 *
 * Helper class for creating SolrClient from a URL.
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Solr {

    private static final Pattern ZK = Pattern.compile("zk://([^/]+)(/.*)?/([^/]*)");

    /**
     * Create a solr client
     * <p>
     * The URL parameter can be of zookeeper type:
     * zk://[host,host,...][/chroot]/(collection|alias)
     * or web type: http://host[:port]/solr/(collection|alias)
     *
     * @param solrUrl SolR collection location
     * @return SolrClient
     * @throws SolrServerException If there's a problem getting aliases list
     *                             (for zk:// urls)
     * @throws IOException         If there's a problem getting aliases list
     *                             (for zk:// urls)
     */
    public static SolrClient client(String solrUrl) throws SolrServerException, IOException {

        Matcher zkMatcher = ZK.matcher(solrUrl);
        if (zkMatcher.matches()) {
            List<String> zkHosts = Arrays.asList(zkMatcher.group(1).split(","));
            Optional<String> zkChroot = Optional.empty();
            if (zkMatcher.group(2) != null) {
                zkChroot = Optional.of(zkMatcher.group(2));
            }
            CloudSolrClient solrClient = new CloudSolrClient.Builder(zkHosts, zkChroot).build();
            String collectionName = zkMatcher.group(3);
            CollectionAdminRequest.ListAliases request = new CollectionAdminRequest.ListAliases();
            CollectionAdminResponse response = request.process(solrClient);
            collectionName = response.getAliases().getOrDefault(collectionName, collectionName);
            solrClient.setDefaultCollection(collectionName);
            return solrClient;
        } else {
            return new HttpSolrClient.Builder(solrUrl)
                    .allowCompression(true)
                    .build();
        }
    }

}
