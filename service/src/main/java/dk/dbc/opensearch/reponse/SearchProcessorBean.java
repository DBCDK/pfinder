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

import dk.dbc.opensearch.cache.HttpFetcher;
import dk.dbc.opensearch.cache.OpenAgencyProfiles;
import dk.dbc.opensearch.cache.RecordKey;
import dk.dbc.opensearch.cache.ResultSetKey;
import dk.dbc.opensearch.input.CollectionType;
import dk.dbc.opensearch.input.SearchRequest;
import dk.dbc.opensearch.output.Collection;
import dk.dbc.opensearch.output.Object;
import dk.dbc.opensearch.output.Root;
import dk.dbc.opensearch.output.SearchResponse;
import dk.dbc.opensearch.output.SearchResult;
import dk.dbc.opensearch.repository.RecordContent;
import dk.dbc.opensearch.repository.RepositoryAbstraction;
import dk.dbc.opensearch.setup.RepositorySettings;
import dk.dbc.opensearch.setup.Settings;
import dk.dbc.opensearch.solr.profile.Profile;
import dk.dbc.opensearch.solr.profile.Profiles;
import dk.dbc.opensearch.solr.resultset.ResultSet;
import dk.dbc.opensearch.utils.StatisticsRecorder;
import dk.dbc.opensearch.utils.Timing;
import dk.dbc.opensearch.utils.MDCLog;
import dk.dbc.opensearch.utils.UserMessage;
import dk.dbc.opensearch.utils.UserMessageException;
import dk.dbc.opensearch.xml.XMLCacheReader;
import fish.payara.cdi.jsr107.impl.NamedCache;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import javax.cache.Cache;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dbc.opensearch.solr.resultset.ResultSet.EMPTY_RESULT_SET;
import static java.util.Collections.EMPTY_LIST;

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

    @Inject
    HttpFetcher fetcher;

    @Inject
    @NamedCache(cacheName = "resultset", managementEnabled = true)
    Cache<ResultSetKey, ResultSet> resultSetCache;

    @Inject
    @NamedCache(cacheName = "records", managementEnabled = true)
    Cache<RecordKey, RecordContent> recordCache;

    @Resource(type = ManagedExecutorService.class)
    ExecutorService es;

    private SearchRequest request;
    private StatisticsRecorder timings;
    private MDCLog mdc;
    private ResultSet resultSet;
    private HashMap<String, Future<Map.Entry<RecordKey, RecordContent>>> recordFecthing;

    public Root.Scope<Root.EntryPoint> builder(SearchRequest request, StatisticsRecorder timings, MDCLog mdc) {
        this.request = request;
        this.timings = timings;
        this.mdc = mdc;
        this.resultSet = null;
        this.recordFecthing = new HashMap<>();
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
        ResultSetKey key = ResultSetKey.of(request);
        resultSet = getResultSet(key, repoSettings, trackingId);

        int start = request.getStartOrDefault();
        int step = request.getStepValueOrDefault();
        resultSet.fetchWorks(repoSettings.abstraction().getSolrClient(),
                             timings, start, step,
                             trackingId);
        log.trace("resultSet = {}", resultSet);

        resultSetCache.put(key, resultSet);

        fetchRecordsAndFormatThem(step, start, repoSettings, trackingId);
    }

    private void fetchRecordsAndFormatThem(int step, int start, RepositorySettings repoSettings, String trackingId) {
        for (int index = start ; index < start + step ; index++) {
            if (index > resultSet.getHitCount()) // Hitcount is dynamic depending of the progress of the work structure build
                break;
            String work = resultSet.workAtIndex(index);
            List<String> units = resultSet.unitsForWork(work);
            RepositoryAbstraction abstraction = repoSettings.abstraction();
            boolean formatThisUnit = true;
            boolean formatMoreThanOne = formatMoreThanOneUnit();
            for (String unit : units) {
                List<String> openFormatFormatsForThisUnit =
                        formatThisUnit ? getOpenFormatFormats(repoSettings) : EMPTY_LIST;

                RecordKey recordKey = abstraction.makeRecordKey(resultSet, request.getShowAgencyOrDefault(), unit);
                RecordContent cachedContent = recordCache.get(recordKey);
                Future<Map.Entry<RecordKey, RecordContent>> future = es.submit(() ->
                        new AbstractMap.SimpleEntry(
                                recordKey,
                                abstraction.recordContent(
                                        cachedContent, fetcher, timings, trackingId,
                                        resultSet, request.getShowAgencyOrDefault(), unit,
                                        openFormatFormatsForThisUnit)));
                recordFecthing.put(unit, future);
                formatThisUnit = formatMoreThanOne;
            }
        }
    }

    /**
     * This produces a resultset for the result-set-key.
     * <p>
     * If no resultset exists, a dummy is set in the map to lock the slot, and a
     * new is generated.
     * <p>
     * The resultset needs to be stored again if modified (most likely always)
     *
     * @param key          The resultset key
     * @param repoSettings the repository descriptor
     * @param trackingId   the tracking id for nested http calls
     * @return resultSet the resultset in question
     */
    private ResultSet getResultSet(ResultSetKey key, RepositorySettings repoSettings, String trackingId) {
        if (resultSetCache.putIfAbsent(key, EMPTY_RESULT_SET)) {
            Profiles profiles = oaProfiles.getProfileFor(
                    key.getAgencyId(), repoSettings.getName(),
                    timings, trackingId, repoSettings.getSolrRules());
            Profile profile = profiles.getProfile(key.getProfiles());
            resultSet = repoSettings.abstraction().resultSetFor(key, profile);
        } else {
            resultSet = resultSetCache.get(key);
        }
        return resultSet;
    }

    private List<String> getOpenFormatFormats(RepositorySettings repoSettings) {
        HashSet<String> set = new HashSet<>(request.getObjectFormatOrDerault());
        set.removeAll(repoSettings.getRawFormatsOrDefault().keySet());
        set.removeAll(repoSettings.getSolrFormatsOrDefault().keySet());
        return new ArrayList<>(set);
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
     * Output the units for a work
     *
     * @param objects The output writer
     * @param units   List of units
     * @throws XMLStreamException If an XML error occurs (highly unlikely)
     * @throws IOException        If connection is closed and so on
     */
    private void outputUnits(Collection.Stage.NumberOfObjects objects, List<String> units) throws XMLStreamException, IOException {
        boolean formatCurrent = true;
        boolean formatMoreThanOne = formatMoreThanOneUnit();
        for (String unit : units) {
            log.trace("outputting unit: {}", unit);
            try {
                boolean showContent = formatCurrent;
                RecordContent content = recordContentForUnit(unit);
                objects.object(object -> object.
                        _any_repeated(o -> outputObjects(o, content, showContent))
                        .identifier(content.getObjectsAvailable().get(0))
                        .creationDate(content.getCreationDate())
                        .formatsAvailable(formatsAvailable -> {
                            for (String formatAvailable : content.getFormatsAvailable()) {
                                formatsAvailable.format(formatAvailable);
                            }
                        })
                        .objectsAvailable(objectsAvailable -> {
                            for (String objectAvailable : content.getObjectsAvailable()) {
                                objectsAvailable.identifier(objectAvailable);
                            }
                        })
                        .queryResultExplanation("No explain")
                );
            } catch (InterruptedException | ExecutionException ex) {
                log.error("Error fetching async record content: {}", ex.getMessage());
                log.debug("Error fetching async record content: ", ex);
                objects.object(object -> object.error("Internal error showing record"));
            }
            formatCurrent = formatMoreThanOne;
        }
    }

    /**
     * Fetch record content for a unit, and ensure it's in the cache.
     *
     * @param unit which future to get
     * @return content
     * @throws InterruptedException async error
     * @throws ExecutionException   async error
     */
    private RecordContent recordContentForUnit(String unit) throws InterruptedException, ExecutionException {
        Future<Map.Entry<RecordKey, RecordContent>> future = recordFecthing.get(unit);
        Map.Entry<RecordKey, RecordContent> entry = future.get();
        RecordContent content = entry.getValue();
        recordCache.put(entry.getKey(), content);
        return content;
    }

    private boolean formatMoreThanOneUnit() {
        return request.getCollectionTypeOrDefault() != CollectionType.WORK1;
    }

    private void outputObjects(Object.Stage._any_repeated objectInstance, RecordContent content, boolean showContent) throws IOException, XMLStreamException {
        if (showContent) {
            for (String format : request.getObjectFormatOrDerault()) {
                XMLCacheReader reader = content.getRawFormat(format);
                if (reader != null)
                    objectInstance._any(reader);
            }
        }
    }

}
