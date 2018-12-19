/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-service
 *
 * opensearch-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.repository;

import dk.dbc.opensearch.cache.HttpFetcher;
import dk.dbc.opensearch.cache.RecordKey;
import dk.dbc.opensearch.cache.ResultSetKey;
import dk.dbc.opensearch.solr.profile.Profile;
import dk.dbc.opensearch.solr.resultset.ResultSet;
import dk.dbc.opensearch.utils.StatisticsRecorder;
import java.io.IOException;
import java.util.List;
import javax.cache.Cache;
import javax.xml.stream.XMLStreamException;
import org.apache.solr.client.solrj.SolrClient;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public interface RepositoryAbstraction {

    /**
     * Build a ResultSet for this repository
     *
     * @param key     The cache key (contains all that is needed)
     * @param profile the build profile (from the values in key)
     * @return a new result set
     */
    ResultSet resultSetFor(ResultSetKey key, Profile profile);

    /**
     * Provides a SolrClient for use with a resultset
     * <p>
     * SolrClient is not stored in the ResultSet since we don't want that shared
     * across instances
     *
     * @return client for use with ResultSet
     */
    SolrClient getSolrClient();

    /**
     * Extract content for a given unit
     *
     * @param cachedContent     cached entry for this record
     * @param fetcher           web access module
     * @param recorder          timings for different actions
     * @param trackingId        trackingId for sending to other services
     * @param resultSet         the resultset knowing about the unit expansion
     * @param showAgencyId      which agency a resultset should be show as
     *                          (ordering of manifestations)
     * @param unitId            the unit to retrieve content for
     * @param openFormatFormats list of formats to get from openFormat service
     * @return Content object representing this unit in the resultset
     * @throws IOException        When there's communication errors or JSON
     *                            response errors
     * @throws XMLStreamException If there's inconsistency in the supplied XML
     */
    RecordContent recordContent(RecordContent cachedContent,
                                HttpFetcher fetcher, StatisticsRecorder recorder, String trackingId,
                                ResultSet resultSet, int showAgencyId, String unitId,
                                List<String> openFormatFormats) throws IOException, XMLStreamException;

    RecordKey makeRecordKey(ResultSet resultSet, int showAgencyId, String unitId);
}
