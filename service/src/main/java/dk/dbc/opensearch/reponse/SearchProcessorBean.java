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
package dk.dbc.opensearch.reponse;

import dk.dbc.opensearch.cache.OpenAgencyProfiles;
import dk.dbc.opensearch.cache.ResultSetKey;
import dk.dbc.opensearch.input.CollectionType;
import dk.dbc.opensearch.input.SearchRequest;
import dk.dbc.opensearch.output.Collection;
import dk.dbc.opensearch.output.Result;
import dk.dbc.opensearch.output.Root;
import dk.dbc.opensearch.output.SearchResponse;
import dk.dbc.opensearch.output.SearchResult;
import dk.dbc.opensearch.setup.RepositorySettings;
import dk.dbc.opensearch.setup.Settings;
import dk.dbc.opensearch.solr.SolrQueryFields;
import dk.dbc.opensearch.solr.SolrRules;
import dk.dbc.opensearch.solr.profile.Profile;
import dk.dbc.opensearch.solr.profile.Profiles;
import dk.dbc.opensearch.solr.resultset.ResultSet;
import dk.dbc.opensearch.solr.resultset.ResultSetManifestation;
import dk.dbc.opensearch.solr.resultset.ResultSetWork;
import dk.dbc.opensearch.utils.StatisticsRecorder;
import dk.dbc.opensearch.utils.Timing;
import dk.dbc.opensearch.utils.MDCLog;
import dk.dbc.opensearch.utils.UserMessage;
import dk.dbc.opensearch.utils.UserMessageException;
import fish.payara.cdi.jsr107.impl.NamedCache;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.cache.Cache;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
public class SearchProcessorBean {

    private static final Logger log = LoggerFactory.getLogger(SearchProcessorBean.class);

    @Inject
    Settings settings;

    @Inject
    OpenAgencyProfiles oaProfiles;

    @NamedCache(cacheName = "resultset", managementEnabled = true)
    @Inject
    Cache<ResultSetKey, ResultSet> resultSetCache;

    private SearchRequest request;
    private StatisticsRecorder timings;
    private MDCLog mdc;
    private ResultSet resultSet;

    public Root.Scope<Root.EntryPoint> builder(SearchRequest request, StatisticsRecorder timings, MDCLog mdc) {
        this.request = request;
        this.timings = timings;
        this.mdc = mdc;
        this.resultSet = null;
        this.mdc.withAgencyId(request.getAgency())
                .withProfiles(request.getProfilesOrDefault())
                .withTrackingId(request.getTrackingId());
        return (scope) -> scope.searchResponse(this::process);
    }

    private void process(SearchResponse response) throws XMLStreamException, IOException {
        try {
            compute();
        } catch (UserMessageException ex) {
            response.error(ex.getUserMessage(settings.getUserMessages()));
            return;
        } catch (RuntimeException ex) {
            log.error("Error processing search request: {}", ex.getMessage());
            log.info("{}", request);
            log.debug("Error processing search request: ", ex);
            response.error(new UserMessageException(UserMessage.INTERNAL_SERVER_ERROR)
                    .getUserMessage(settings.getUserMessages()));
            return;
        }
        outputResponse(response);
        log.debug("timings = {}", timings);
    }

    private void compute() {
        String trackingId = request.getTrackingId();
        RepositorySettings repoSettings = getRepoSettings();
        int start = request.getStartOrDefault();
        int step = request.getStepValueOrDefault();
        ResultSetKey key = ResultSetKey.of(request);
        resultSet = resultSetCache.get(key);
        if (resultSet == null)
            resultSet = makeResultSet(repoSettings, trackingId);

        // Make it visible to others while we're working on it
        // So that others don't make the same object
        resultSetCache.put(key, resultSet);

        resultSet.fetchWorks(repoSettings.solrClient(),
                             timings, start, step,
                             trackingId);
        resultSetCache.put(key, resultSet);
    }

    private void outputResponse(SearchResponse response) throws IOException, XMLStreamException {
        try (Timing timerOutput = timings.timer("output")) {
            int start = request.getStartOrDefault();
            int step = request.getStepValueOrDefault();
            response.result(result -> result
                    .hitCount((int) resultSet.getSolrHitCount())
                    .collectionCount((int) resultSet.getHitCount())
                    .more(resultSet.getHitCount() >= start + step)
                    .searchResult(this::outputRecords)
            );
        }
    }

    private void outputRecords(SearchResult searchResult) throws XMLStreamException, IOException {
        int start = request.getStartOrDefault();
        int step = request.getStepValueOrDefault();
        for (int i = 0 ; i < step ; i++) {
            int index = start + i;
            if (index > resultSet.getHitCount())
                break;
            String work = resultSet.workAtIndex(index);
            List<String> units = resultSet.unitsForWork(work);
            searchResult
                    .collection(collection -> collection
                            .resultPosition(index)
                            .numberOfObjects(units.size())
                            ._delegate(objects -> {
                                outputUnits(objects, units);
                            }));
        }
    }

    private RepositorySettings getRepoSettings() {
        String repository = request.getRepository();
        if (repository == null)
            repository = settings.getDefaultRepository();
        RepositorySettings repoSettings = settings.lookupRepositoryByAlias(repository);
        return repoSettings;
    }

    /**
     * Make a new cacheable resultset
     *
     * @param repoSettings The settings to use for this resultset
     * @param trackingId   trackingId for open-agency http requests
     * @return new ResultSet with a query
     */
    private ResultSet makeResultSet(RepositorySettings repoSettings, String trackingId) {
        SolrRules solrRules = repoSettings.getSolrRules();
        Profiles profiles = oaProfiles.getProfileFor(request.getAgency(), repoSettings.getName(),
                                                     timings, trackingId, solrRules);
        Profile profile = profiles.getProfile(request.getProfilesOrDefault());
        SolrQueryFields solrQuery;
        switch (request.getQueryLanguage()) {
            case "cql":
            case "cqleng":
                solrQuery = SolrQueryFields.fromCQL(solrRules, request.getQuery(), profile);
                break;
            default:
                throw new UserMessageException(UserMessage.UNSUPPORTED_QUERY_LANGUAGE);
        }
        if (request.getCollectionTypeOrDefault() == CollectionType.MANIFESTATION)
            return new ResultSetManifestation(solrQuery, request.getAllObjectsOrDefault());
        else
            return new ResultSetWork(solrQuery, request.getAllObjectsOrDefault());
    }

    /**
     * Output the units for a work
     *
     * @param objects The output writer
     * @param units   List of units
     * @throws XMLStreamException If an XML error occurs (highly unlikely)
     * @throws IOException        If connection is closed and so on
     */
    private void outputUnits(Collection.Stage.NumberOfObjects objects, List<String> units) throws XMLStreamException, IOException {
        boolean outputContent = true;
        boolean outputContentNext = request.getCollectionTypeOrDefault() != CollectionType.WORK1;
        for (String unit : units) {
            Set<String> manifestations = resultSet.manifestationsForUnit(unit);
            boolean outputThisContent = outputContent;
            objects.object(object -> {
                if (outputThisContent) {
                    // Output data
                }
                object.identifier(resultSet.manifestationsForUnit(unit).iterator().next())
                        .objectsAvailable(avail -> {
                            for (String manifestation : manifestations) {
                                avail.identifier(manifestation);
                            }
                        });
            });
            // Wether 2nd and the rest should the be outputted
            outputContent = outputContentNext;
        }
    }

}
