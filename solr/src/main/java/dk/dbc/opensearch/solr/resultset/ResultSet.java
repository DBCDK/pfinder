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
import dk.dbc.opensearch.utils.StatisticsRecorder;
import dk.dbc.opensearch.utils.Timing;
import dk.dbc.opensearch.utils.UserMessage;
import dk.dbc.opensearch.utils.UserMessageException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.*;
import static org.apache.solr.client.solrj.SolrRequest.METHOD.POST;
import static org.apache.solr.client.solrj.util.ClientUtils.escapeQueryChars;

/**
 *
 * This abstract class represents the three-level result set
 * <p>
 * Level 1: work id
 * <p>
 * Level 2: unit id
 * <p>
 * Level 3: manifestations
 * <p>
 * The works should be ordered by appearance, within each work the units should
 * be ordered by appearance too. All the units for a work should be present
 * before showing the unit.
 * <p>
 * The manifestations should not be ordered at this point. When presented, the
 * order is determined by OpenAgency showOrder.
 *
 * @author DBC {@literal <dbc.dk>}
 */
public abstract class ResultSet implements Serializable {

    private static final long serialVersionUID = -6814210766876494993L;

    private static final Logger log = LoggerFactory.getLogger(ResultSet.class);

    public static final String WORK_ID = "rec.workId";
    public static final String UNIT_ID = "rec.unitId";
    public static final String MANIFESTATION_ID = "rec.manifestationId";

    private static final int SOLR_CLOUD_MAX_ROWS = 10000;

    // The reason for the List is that we need the units in the order they appear
    private final Map<String, List<String>> workToUnits;
    private final Map<String, Set<String>> unitToManifestations;
    // workOrder is the order in which work-id's are added to workToUnit.
    // They could be handled by a ordererd map, however some claim
    // that when a ordered map has been Serialized the order is not always kept
    // - this seems unlikely to me, however tracking an error like that will be
    // expensive and we need access to work-ids by position anyway.
    private final List<String> workOrder;
    private final Set<String> worksExpanded;
    private final SolrQueryFields solrQuery;
    private final boolean allObjects;
    private boolean complete;
    // Estimated number of works until complete, then exact number of works
    private long hitCount;
    private long solrHitCount;

    private transient SolrClient client;
    private transient StatisticsRecorder recorder;

    public ResultSet(SolrQueryFields solrQuery, boolean allObjects) {
        this.workToUnits = new HashMap<>();
        this.unitToManifestations = new HashMap<>();
        this.workOrder = new ArrayList<>();
        this.worksExpanded = new HashSet<>();
        this.solrQuery = solrQuery;
        this.allObjects = allObjects;
        this.complete = false;
    }

    /**
     * Supply the name of the field representing a work in the SolR
     *
     * @return A field name
     */
    protected abstract String nameOfWorkField();

    /**
     * Supply the name of the field representing a unit in the SolR
     *
     * @return A field name
     */
    protected abstract String nameOfUnitField();

    /**
     * Supply the name of the field representing a manifestation in the SolR
     *
     * @return A field name
     */
    protected abstract String nameOfManifestationField();

    /**
     * List all the fields required by this logic
     * <p>
     * Should contain all of Work-, Unit- and ManifestationField
     *
     * @return Collection of fields wanted from the SolR for work construction
     */
    protected abstract Collection<String> namesOfFieldsRequired();

    /**
     * If all the work ids of the resultset are found
     *
     * @return true/false
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Get a collection count
     * <p>
     * until {@link #isComplete() } returns true the work count is
     * estimated
     *
     * @return estimated / actual work count
     */
    public long getHitCount() {
        if (isComplete())
            return hitCount;
        else
            return Long.max(worksFound() + 1, hitCount);
    }

    /**
     * The number of hits in the SolR query
     * <p>
     * This value is set by {@link #approximateHitCount(int, long)},
     * which should be called after the 1st call to SolR in
     * {@link #fetchMore(org.apache.solr.client.solrj.SolrQuery, boolean)}
     *
     * @return Hit count from SolR
     */
    public long getSolrHitCount() {
        return solrHitCount;
    }

    /**
     * Get a list of units for a given work.
     *
     * @param work the id of the work seen
     * @return list in order of appearance
     */
    public List<String> unitsForWork(String work) {
        return unmodifiableList(workToUnits.getOrDefault(work, EMPTY_LIST));
    }

    /**
     * Lookup work at a given position in the result set
     *
     * @param index position of work (starting at 1)
     * @return id of work
     */
    public String workAtIndex(int index) {
        log.debug("index = {}", index);
        log.debug("worksExpanded.size() = {}", worksExpanded.size());
        if (index - 1 >= worksExpanded.size())
            throw new IllegalStateException("Asking for a work that isn't expanded");
        return workOrder.get(index - 1);
    }

    /**
     * Get a collection of manifestations seen for a given unit
     *
     * @param unit The unit the manifestations belong to
     * @return Collection of manifestations
     */
    public Set<String> manifestationsForUnit(String unit) {
        return unmodifiableSet(unitToManifestations.getOrDefault(unit, EMPTY_SET));
    }

    /**
     * Fill in work structure and expand the wanted works
     * <p>
     * This is the main entry point for the class
     * <p>
     * It is synchronized, since multiple threads shouldn't compete for fetching
     * data
     *
     * @param client     SolR client to perform queries with
     * @param recorder   Where timings are logged
     * @param start      The first result position wanted (origin=1)
     * @param step       The number of results wanted
     * @param trackingId The tracking id - added to SolR requests
     * @return List of work ids starting at position start (empty in no hits/no
     *         more)
     */
    public synchronized List<String> fetchWorks(SolrClient client, StatisticsRecorder recorder, int start, int step, String trackingId) {
        this.client = client;
        this.recorder = recorder;
        // Since start origin is 1 this produces atleast one work more than
        // wanted - which in turn allows for the "has more" value
        findWorks(start + step, trackingId);
        int workCount = worksFound();
        int first = Integer.min(start - 1, workCount);
        int last = Integer.min(start - 1 + step, workCount);
        List<String> worksInRange = workOrder.subList(first, last);
        ensureWorksAreExpanded(worksInRange, trackingId);
        worksExpanded.addAll(worksInRange);
        return unmodifiableList(worksInRange);
    }

    private int worksFound() {
        return workOrder.size();
    }

    /**
     * Has no works been found yet?
     * <p>
     * ie. the state is that
     * {@link #fetchMore(org.apache.solr.client.solrj.SolrQuery, boolean)}
     * should call {@link #approximateHitCount(int, long)}
     *
     * @return If this is before anything has been fetched
     */
    protected boolean noWorksFound() {
        return workOrder.isEmpty();
    }

    /**
     * The entrypoint for inherited classes to register results
     *
     * @param doc The document as returned from SolR
     */
    protected void registerManifestation(SolrDocument doc) {
        Map<String, Collection<Object>> values = doc.getFieldValuesMap();
        String work = extractValue(values, nameOfWorkField(), true).iterator().next();
        String unit = extractValue(values, nameOfUnitField(), true).iterator().next();
        Collection<String> manifestations = extractValue(values, nameOfManifestationField(), false);
        manifestations.forEach(manifestation -> registerManifestation(work, unit, manifestation));
        // At this point you could record the values from the doc, for mapping into solr-formats
        // It could be done on manifestation or unit level.
    }

    /**
     * Extract a value from a SolRDocument
     *
     * @param values     The map of SolR values as returned by
     *                   {@link SolrDocument#getFieldValuesMap()}
     * @param field      The name of the field wanted
     * @param exactlyOne If one and only one value is allowed
     * @return A String representation of the wanted value
     */
    private Collection<String> extractValue(Map<String, Collection<Object>> values, String field, boolean exactlyOne) {
        Collection workValues = values.getOrDefault(field, EMPTY_LIST);
        if (workValues.isEmpty()) {
            log.error("SolrError no value for field: `{}' in: {}", field, values);
            throw new UserMessageException(UserMessage.BACKEND_SOLR);
        }
        if (exactlyOne && workValues.size() != 1) {
            log.error("SolrError too many values for field: `{}' in: {}", field, values);
            throw new UserMessageException(UserMessage.BACKEND_SOLR);
        }
        return workValues;
    }

    /**
     * Put a manifestation into the structure
     * <p>
     * Ensures that the work and unit is known
     *
     * @param work          The id of the work
     * @param unit          The id of the unit
     * @param manifestation The id of the manifestation
     */
    private void registerManifestation(String work, String unit, String manifestation) {
        log.trace("registering: {}/{}/{}", work, unit, manifestation);
        Set<String> manifestationsInUnit = unitToManifestations
                .computeIfAbsent(unit, u -> registerUnit(work, unit));
        manifestationsInUnit.add(manifestation);
    }

    /**
     * Called when a unit is unknown
     * <p>
     * This supplies a new (empty) set of manifestations, and ensures the unit
     * is registered as part of the work (in the order it is seen compared to
     * other units)
     *
     * @param work The id of the work
     * @param unit The id of the unit
     * @return A new empty set of manifestations
     */
    private Set<String> registerUnit(String work, String unit) {
        List<String> unitsInWork = workToUnits
                .computeIfAbsent(work, this::registerWork);
        unitsInWork.add(unit);
        return new HashSet<>();
    }

    /**
     * Called then a work is unknown
     * <p>
     * Records the order in which the works are seen, and supplies an empty
     * list of units for this work
     *
     * @param work The id of the work
     * @return A new empty list of units
     */
    private List<String> registerWork(String work) {
        workOrder.add(work);
        return new ArrayList<>();
    }

    /**
     * Find work ids until enough are seen
     *
     * @param workCount  The number of work ids in {@link #workOrder} wanted
     * @param trackingId The tracking id to supply for SolR queries
     * @throws SolrServerException If there's a problem with the SolR syntax, or
     *                             the values in the SolR response
     * @throws IOException         If there's a communication problem
     */
    private void findWorks(int workCount, String trackingId) {
        while (!complete && worksFound() <= workCount) {
            SolrQuery query = solrQuery.asSolrQuery();
            query.set("trackingId", trackingId);
            query.setRows(getFetchRows(workCount - worksFound()));
            setQueryFields(query);
            boolean firstRun = noWorksFound();
            complete = fetchMore(query, firstRun);
            if (complete) {
                hitCount = worksFound(); // Exact work count
                if (firstRun && !allObjects)
                    // Not allAbjects and complete without skipping seen works (firstRun)
                    // everything is fetched from SolR, all works are fully
                    // expanded
                    worksExpanded.addAll(workOrder);
            }
        }
    }

    /**
     * Find work ids until enough are seen
     * <p>
     * Register the SolR documents retrieved using
     * {@link #registerManifestation(org.apache.solr.common.SolrDocument)}
     *
     * @param query    The SolR query
     * @param firstRun If this is the initial query and
     *                 {@link #approximateHitCount(int, long)} should be called
     * @return if all rows in the SolR result set has been read
     */
    protected boolean fetchMore(SolrQuery query, boolean firstRun) {
        if (!firstRun)
            filterOutSeenWorks(query);
        query.setStart(0);
        QueryResponse response = performQuery(query, QueryType.BUILD_WORK);
        SolrDocumentList resultList = response.getResults();
        resultList.forEach(this::registerManifestation);
        long rowsFound = resultList.getNumFound();
        int rowsFetched = resultList.size();
        if (firstRun)
            approximateHitCount(rowsFetched, rowsFound);
        boolean fetchedAllResults = rowsFound == rowsFetched;
        return fetchedAllResults;
    }

    /**
     * Given the works the user wants to see, make sure the entire work
     * structure for those ids is expanded
     *
     * @param worksInRange Collection of works the user wants
     * @param trackingId   The tracking id - added to SolR requests
     */
    protected void ensureWorksAreExpanded(List<String> worksInRange, String trackingId) {
        Set<String> worksWanted = new HashSet<>(worksInRange);
        worksWanted.removeAll(worksExpanded);
        if (worksWanted.isEmpty())
            return;
        // Not all are expanded
        SolrQuery query = solrQuery.asSolrQuery();
        query.set("trackingId", trackingId);
        query.setFacet(false); // Nothing but the query sorted/boosted/ranked
        query.setHighlight(false);
        query.setMoreLikeThis(false);
        query.setShowDebugInfo(false);
        query.setTerms(false);
        setQueryFields(query);
        query.setRows(SOLR_CLOUD_MAX_ROWS);
        expandWorksQuery(query, worksWanted);
        log.debug("Expanding works: {}", worksWanted);
        expandWorks(query);
    }

    /**
     * Find all unit/manifestations for a group of works
     * <p>
     * Register the SolR documents retrieved using
     * {@link #registerManifestation(org.apache.solr.common.SolrDocument)}
     *
     * @param query The SolR query (limited to the works needed to be expanded)
     */
    protected void expandWorks(SolrQuery query) {
        int start = 0;
        boolean allFetched = false;
        while (!allFetched) {
            query.setStart(start);
            QueryResponse response = performQuery(query, QueryType.EXPAND_WORK);
            SolrDocumentList resultList = response.getResults();
            resultList.forEach(this::registerManifestation);
            start += resultList.size();
            allFetched = resultList.getNumFound() == start;
        }
    }

    /**
     * Perform a query against a SolR
     *
     * @param query     The query
     * @param queryType String used for identifying the query type in the error
     *                  log
     * @return The successful query
     */
    protected QueryResponse performQuery(SolrQuery query, QueryType queryType) {
        try {
            log.trace("fetching: {}", query);
            QueryResponse response;
            try (Timing timer = recorder.timer(queryType.getTimingName())) {
                response = client.query(query, POST);
            }
            log.trace("retrieved: {}", response);
            if (response.getStatus() != 0)
                throw new SolrServerException(String.valueOf(response.getResponse().get("error")));
            return response;
        } catch (SolrServerException | IOException ex) {
            String logName = queryType.getLogName();
            log.error("SolrError: {}: {}", logName, ex.getMessage());
            log.error("SolrError: {}: Query: {}", logName, query.getQuery());
            log.debug("SolrError: {}:", logName, ex);
            throw new UserMessageException(UserMessage.BACKEND_SOLR);
        }
    }

    /**
     * Expand the query to filter out already seen works
     *
     * @param query the SolR query to manipulate
     */
    protected void filterOutSeenWorks(SolrQuery query) {
        if (noWorksFound())
            return;
        String workField = nameOfWorkField();
        StringBuilder q = new StringBuilder();
        q.append('(').append(query.getQuery()).append(") NOT ")
                .append(workField).append(":(");
        for (Iterator<String> iterator = workOrder.iterator() ; iterator.hasNext() ;) {
            String workId = iterator.next();
            q.append(escapeQueryChars(workId));
            if (iterator.hasNext())
                q.append(" OR ");
        }
        q.append(')');
        query.setQuery(q.toString());
    }

    /**
     * Replace the query to match only the wanted works
     *
     * @param query       the SolR query to manipulate
     * @param worksWanted A collection of work ids
     */
    protected void expandWorksQuery(SolrQuery query, Set<String> worksWanted) {
        String workField = nameOfWorkField();
        StringBuilder q = new StringBuilder();
        q.append(workField).append(":(");
        for (Iterator<String> iterator = workOrder.iterator() ; iterator.hasNext() ;) {
            String workId = iterator.next();
            q.append(escapeQueryChars(workId));
            if (iterator.hasNext())
                q.append(" OR ");
        }
        q.append(')');
        if (!allObjects)
            q.append("AND (").append(query.getQuery()).append(")");
        query.setQuery(q.toString());
    }

    /**
     * Roughly compute the number of manifestations in a result set based
     * <p>
     * Computed upon the number of hits in the solr, the number of rows fetched
     * from the SolR and the number of works seen already
     * <p>
     * This could be rounded to nearest magnitude?
     *
     * @param rowsFetched Number of results processed
     * @param rowsTotal   Total number of results
     */
    protected void approximateHitCount(int rowsFetched, long rowsTotal) {
        this.solrHitCount = rowsTotal;
        double workCount = (double) worksFound();
        double rows = (double) rowsTotal;
        double fetched = (double) rowsFetched;
        double linearProgression = workCount * rows / fetched;
        hitCount = trimTo2Digits((int) linearProgression);
    }

    private int trimTo2Digits(int base) {
        int digits = (int) Math.floor(Math.log10(base)) - 1;
        int trim = Integer.max(1, (int) Math.pow(10, digits - 1));
        return ( base + trim / 2 ) / trim * trim;
    }

    /**
     * Set the fields wanted by a SolR query
     * <p>
     * This is where solr-formats could add all the wanted fields
     *
     * @param query The query to set the field names upon
     */
    protected void setQueryFields(SolrQuery query) {
        query.setFields();
        namesOfFieldsRequired().forEach(query::addField);
    }

    /**
     * Compute the number of rows wanted to get a number of manifestations
     * <p>
     * For optimization purposes (minimize the number of rows fetched, but still
     * get enough to build the works in one round trip, and estimate a hit
     * count)
     *
     * @param wantedWorkCount the number of works to create from a single SolR
     *                        query
     * @return number of results from the SolR to give the wanted number of
     *         works
     */
    protected int getFetchRows(int wantedWorkCount) {
        return 1000;
    }

    @Override
    public String toString() {
        return "ResultSet{" + "workToUnits=" + workToUnits + ", unitToManifestations=" + unitToManifestations + ", workOrder=" + workOrder + ", worksExpanded=" + worksExpanded + ", solrQuery=" + solrQuery + ", allObjects=" + allObjects + ", complete=" + complete + ", hitCount=" + hitCount + ", solrHitCount=" + solrHitCount + '}';
    }

}
