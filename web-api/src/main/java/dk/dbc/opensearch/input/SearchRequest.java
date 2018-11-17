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
package dk.dbc.opensearch.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import static dk.dbc.opensearch.input.RequestHelpers.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class SearchRequest extends CommonRequest {

    public static final InputPartFactory<SearchRequest> FACTORY =
            new InputPartFactory<>(SearchRequest::new)
                    // Base
                    .with("agency", obj -> obj::putAgency)
                    .with("profile", obj -> obj::addProfile)
                    .with("callback", obj -> obj::putCallback)
                    .with("outputType", obj -> obj::putOutputType)
                    .with("trackingId", obj -> obj::putTrackingId)
                    // Common
                    .with("showAgency", obj -> obj::putShowAgency)
                    .with("authentication", Authentication.FACTORY, obj -> obj::putAuthentication)
                    .with("includeHoldingsCount", obj -> obj::putIncludeHoldingsCount)
                    .with("relationData", obj -> obj::putRelationData)
                    .with("repository", obj -> obj::putRepository)
                    // Local
                    .with("allObjects", obj -> obj::putAllObjects)
                    .with("collapseHitsThreshold", obj -> obj::putCollapseHitsThreshold)
                    .with("collectionType", obj -> obj::putCollectionType)
                    .with("facets", Facets.FACTORY, obj -> obj::addFacets)
                    .with("objectFormat", obj -> obj::addObjectFormat)
                    .with("query", obj -> obj::putQuery)
                    .with("queryDebug", obj -> obj::putQueryDebug)
                    .with("queryLanguage", obj -> obj::putQueryLanguage)
                    .with("sort", obj -> obj::addSort)
                    .with("start", obj -> obj::putStart)
                    .with("stepValue", obj -> obj::putStepValue)
                    .with("userDefinedBoost", UserDefinedBoost.FACTORY, obj -> obj::addUserDefinedBoost)
                    .with("userDefinedRanking", UserDefinedRanking.FACTORY, obj -> obj::addUserDefinedRanking);

    private static final Function<String, CollectionType> COLLECTION_TYPES = mapTo(makeTrimOneOf("collectionType", "work", "work-1", "manifestation"),
                                                                                   CollectionType::from);
    private static final Function<String, String> QUERY_LANGUAGES = makeTrimOneOf("queryLanguage", "cqleng", "bestMatch");
    private static final Function<String, String> OBJECT_FORMATS = makeTrimOneOf("objectFormat",
                                                                                 "dkabm", "docbook", "marcxchange", "opensearchobject", "briefWorkDisplay",
                                                                                 "bibliotekdkWorkDisplay", "briefDisplayHtml", "fullDisplayHtml",
                                                                                 "workDisplayHtml", "briefDisplay", "fullDisplay", "refWorks", "ris");

    private String query = null;
    private String queryLanguage = null;
    private Boolean allObjects = null;
    private CollectionType collectionType;
    private List<Facets> facets = null;
    private Integer collapseHitsThreshold = null;
    private final List<String> objectFormat = new ArrayList<>();
    private Integer start = null;
    private Integer stepValue = null;
    private List<UserDefinedRanking> userDefinedRanking = null;
    private List<String> sort = null;
    private List<UserDefinedBoost> userDefinedBoost = null;
    private Boolean queryDebug = null;

    public SearchRequest() {
    }

    @Override
    public void validate(Location location) throws XMLStreamException {
        super.validate(location);
        if (query == null)
            throw new XMLStreamException("property 'query' is required in a searchRequest");
    }

    //
    // Setters and getters
    //
    public void putAllObjects(String content, Location location) throws XMLStreamException {
        allObjects = get("allObjects", allObjects, content, location,
                         s -> Boolean.parseBoolean(trimNotEmpty(s)));
    }

    public Boolean getAllObjects() {
        return allObjects;
    }

    public void putCollapseHitsThreshold(String content, Location location) throws XMLStreamException {
        collapseHitsThreshold = get("collapseHitsThreshold", collapseHitsThreshold, content, location,
                                    s -> Integer.parseUnsignedInt(trimNotEmpty(s)));
    }

    public Integer getCollapseHitsThreshold() {
        return collapseHitsThreshold;
    }

    public void putCollectionType(String content, Location location) throws XMLStreamException {
        collectionType = get("collectionType", collectionType, content, location,
                             COLLECTION_TYPES);
    }

    public CollectionType getCollectionType() {
        return collectionType == null ? CollectionType.WORK : collectionType;
    }

    public void addFacets(Facets content, Location location) throws XMLStreamException {
        if (facets == null)
            facets = new ArrayList<>();
        facets.add(content);
    }

    public List<Facets> getFacets() {
        return facets;
    }

    public void addObjectFormat(String content, Location location) throws XMLStreamException {
        objectFormat.add(get("objectFormat", content, location, OBJECT_FORMATS));
    }

    public List<String> getObjectFormats() {
        return objectFormat.isEmpty() ? Arrays.asList("marcxchange") : objectFormat;
    }

    public void putQuery(String content, Location location) throws XMLStreamException {
        query = get("query", query, content, location,
                    s -> trimNotEmpty(s));
    }

    public String getQuery() {
        return query;
    }

    public void putQueryDebug(String content, Location location) throws XMLStreamException {
        queryDebug = get("queryDebug", queryDebug, content, location,
                         s -> Boolean.parseBoolean(trimNotEmpty(s)));
    }

    public Boolean getQueryDebug() {
        return queryDebug;
    }

    public void putQueryLanguage(String content, Location location) throws XMLStreamException {
        queryLanguage = get("queryLanguage", queryLanguage, content, location,
                            QUERY_LANGUAGES);
    }

    public String getQueryLanguage() {
        return queryLanguage;
    }

    public void addSort(String content, Location location) throws XMLStreamException {
        if (userDefinedRanking != null)
            throw new XMLStreamException("Cannot have both userDefinedRanking and sort", location);
        if (sort == null)
            sort = new ArrayList<>();
        sort.add(get("sort", content, location, s -> trimNotEmptyOneWord(s)));
    }

    public List<String> getSort() {
        return sort;
    }

    public void putStart(String content, Location location) throws XMLStreamException {
        start = get("start", start, content, location,
                    s -> Integer.parseUnsignedInt(trimNotEmpty(s)));
    }

    public Integer getStart() {
        return start == null ? 1 : Integer.max(1, start);
    }

    public void putStepValue(String content, Location location) throws XMLStreamException {
        stepValue = get("stepValue", stepValue, content, location,
                        s -> Integer.parseUnsignedInt(trimNotEmpty(s)));
    }

    public Integer getStepValue() {
        return stepValue == null ? 0 : stepValue;
    }

    public void addUserDefinedBoost(UserDefinedBoost content, Location location) {
        if (userDefinedBoost == null)
            userDefinedBoost = new ArrayList<>();
        userDefinedBoost.add(content);
    }

    public List<UserDefinedBoost> getUserDefinedBoost() {
        return userDefinedBoost;
    }

    public void addUserDefinedRanking(UserDefinedRanking content, Location location) throws XMLStreamException {
        if (sort != null)
            throw new XMLStreamException("Cannot have both sort and userDefinedRanking", location);
        if (userDefinedRanking == null)
            userDefinedRanking = new ArrayList<>();
        userDefinedRanking.add(content);
    }

    public List<UserDefinedRanking> getUserDefinedRanking() {
        return userDefinedRanking;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return "SearchRequest{" + s.substring(s.indexOf('{') + 1, s.lastIndexOf('}')) + ",query=" + query + ", queryLanguage=" + queryLanguage + ", allObjects=" + allObjects + ", collectionType=" + collectionType + ", facets=" + facets + ", collapseHitsThreshold=" + collapseHitsThreshold + ", objectFormat=" + objectFormat + ", start=" + start + ", stepValue=" + stepValue + ", userDefinedRanking=" + userDefinedRanking + ", sort=" + sort + ", userDefinedBoost=" + userDefinedBoost + ", queryDebug=" + queryDebug + '}';
    }

}
