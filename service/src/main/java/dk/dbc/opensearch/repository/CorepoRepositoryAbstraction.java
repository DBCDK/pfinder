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
import dk.dbc.opensearch.cache.ResultSetKey;
import dk.dbc.opensearch.input.CollectionType;
import dk.dbc.opensearch.setup.RepositorySettings;
import dk.dbc.opensearch.solr.Solr;
import dk.dbc.opensearch.solr.SolrQueryFields;
import dk.dbc.opensearch.solr.SolrRules;
import dk.dbc.opensearch.solr.profile.Profile;
import dk.dbc.opensearch.solr.resultset.ResultSet;
import dk.dbc.opensearch.solr.resultset.ResultSetManifestation;
import dk.dbc.opensearch.solr.resultset.ResultSetWork;
import dk.dbc.opensearch.utils.StatisticsRecorder;
import dk.dbc.opensearch.utils.UserMessage;
import dk.dbc.opensearch.utils.UserMessageException;
import dk.dbc.opensearch.xml.DefaultPrefix;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class CorepoRepositoryAbstraction implements RepositoryAbstraction {

    private final DefaultPrefix defaultPrefix;
    private final SolrClient solrClient;
    private final String contentUriTemplate;
    private final SolrRules solrRules;
    private final Map<QName, String> knownFormats;

    public CorepoRepositoryAbstraction(DefaultPrefix defaultPrefix, RepositorySettings repositorySettings) throws SolrServerException, IOException {
        this.defaultPrefix = defaultPrefix;
        this.solrRules = repositorySettings.getSolrRules();
        this.solrClient = Solr.client(repositorySettings.getSolrUrl());
        this.contentUriTemplate = repositorySettings.getContentServiceUrl();
        this.knownFormats = repositorySettings.getKnownFormatsByQName();
    }

    @Override
    public ResultSet resultSetFor(ResultSetKey key, Profile profile) {
        SolrQueryFields solrQuery;
        switch (key.getQueryLanguage()) {
            case "cql":
            case "cqleng":
                solrQuery = SolrQueryFields.fromCQL(solrRules, key.getQuery(), profile);
                break;
            default:
                throw new UserMessageException(UserMessage.UNSUPPORTED_QUERY_LANGUAGE);
        }
        if (key.getCollectionType() == CollectionType.MANIFESTATION)
            return new ResultSetManifestation(solrQuery, key.getAllObjects());
        else
            return new ResultSetWork(solrQuery, key.getAllObjects());
    }

    @Override
    public SolrClient getSolrClient() {
        return solrClient;
    }

    @Override
    public RecordContent recordContent(HttpFetcher fetcher, StatisticsRecorder recorder, String trackingId,
                                       ResultSet resultSet, int showAgencyId, String unitId,
                                       List<String> openFormatFormats) throws IOException, XMLStreamException {
        Set<String> manifestations = resultSet.manifestationsForUnit(unitId);
        try (InputStream is = fetcher.get(contentUriTemplate, trackingId)
                .with("agency", String.format(Locale.ROOT, "%06d", showAgencyId))
                .with("unit", unitId)
                .with("manifestations", String.join(",", manifestations))
                .request(recorder, "corepo-content-service")) {
            CorepoRecordContent recordContent = new CorepoRecordContent(is, knownFormats, defaultPrefix);
            //! TODO openFormatFormats
            return recordContent;
        }
    }

}
