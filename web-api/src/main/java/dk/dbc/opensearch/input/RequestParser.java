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

import java.io.InputStream;
import java.util.UUID;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import static javax.xml.stream.XMLStreamConstants.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class RequestParser {

    private static final XMLInputFactory I = makeXMLInputFactory();
    private static final int EVENT_FILTER =
            maskOf(PROCESSING_INSTRUCTION) | maskOf(COMMENT) | maskOf(SPACE) |
            maskOf(START_DOCUMENT) | maskOf(END_DOCUMENT) |
            maskOf(ENTITY_REFERENCE) | maskOf(ATTRIBUTE) |
            maskOf(DTD) | maskOf(NAMESPACE) | maskOf(NOTATION_DECLARATION) |
            maskOf(ENTITY_DECLARATION);
    private static final String OS_URI = "http://oss.dbc.dk/ns/opensearch";
    private static final String SOAP_URI = "http://schemas.xmlsoap.org/soap/envelope/";

    /**
     * Convert a bit number into a bit mask
     *
     * @param bitNo the bit that should be set
     * @return integer with the bit set
     */
    private static int maskOf(int bitNo) {
        return 1 << bitNo;
    }

    /**
     * Check if a bit is set in EVENT_FILTER
     *
     * @param bitNo this bit to test
     * @return if the bit is set
     */
    private static boolean isWanted(int bitNo) {
        return ( EVENT_FILTER & maskOf(bitNo) ) != 0;
    }

    private final XMLEventReader reader;
    private BaseRequest request;

    public RequestParser(InputStream is) throws XMLStreamException {
        this.reader = I.createFilteredReader(
                I.createXMLEventReader(is),
                e -> isWanted(e.getEventType()));
        readOuterMost();
    }

    public boolean isGetObjectRequest() {
        return request instanceof GetObjectRequest;
    }

    public GetObjectRequest asGetObjectRequest() {
        return (GetObjectRequest) request;
    }

    public boolean isInfoRequest() {
        return request instanceof InfoRequest;
    }

    public InfoRequest asInfoRequest() {
        return (InfoRequest) request;
    }

    public boolean isSearchRequest() {
        return request instanceof SearchRequest;
    }

    public SearchRequest asSearchRequest() {
        return (SearchRequest) request;
    }

    public CommonRequest asCommonRequest() {
        return (CommonRequest) request;
    }

    public BaseRequest asBaseRequest() {
        return request;
    }

    private void readOuterMost() throws XMLStreamException {
        XMLEvent event = reader.nextEvent();
        if (!event.isStartElement()) {
            throw new XMLStreamException("Expected tag as opening of request", event.getLocation());
        }
        if (isOpen(event, SOAP_URI, "Envelope")) {
            readSoapEnvelope();
            setDefaultOutputType(OutputType.SOAP);
        } else {
            readRequest(event);
            setDefaultOutputType(OutputType.XML);
        }
        setDefaultTrackingId();
    }

    private void setDefaultOutputType(OutputType type) {
        OutputType outputType = request.getOutputType();
        if (outputType == null) {
            request.setOutputType(type);
        }
    }

    private void setDefaultTrackingId() {
        String trackingId = request.getTrackingId();
        if (trackingId == null) {
            request.setTrackingId(UUID.randomUUID().toString());
        }
    }

    private void readSoapEnvelope() throws XMLStreamException {
        XMLEvent event = reader.nextTag();
        event = readSoapHeader(event);
        if (isOpen(event, SOAP_URI, "Body")) {
            event = reader.nextTag();
            readRequest(event);
        }
        if (!isClose(reader.nextTag(), SOAP_URI, "Body")) {
            throw new XMLStreamException("Expected Closing tag of SOAP Body", event.getLocation());
        }
        if (!isClose(reader.nextTag(), SOAP_URI, "Envelope")) {
            throw new XMLStreamException("Expected Closing tag of SOAP Envelope", event.getLocation());
        }
        if (reader.hasNext()) { // This shouldn't happen... unpalanced xml should be cought by parser
            throw new XMLStreamException("Expected EOT", reader.nextEvent().getLocation());
        }
    }

    private XMLEvent readSoapHeader(XMLEvent event) throws XMLStreamException {
        if (!isOpen(event, SOAP_URI, "Header")) {
            return event;
        }
        int level = 1;
        while (level > 0) {
            switch (reader.nextEvent().getEventType()) {
                case START_ELEMENT:
                    level++;
                    break;
                case END_ELEMENT:
                    level--;
                    break;
                default:
                    break;
            }
        }
        return reader.nextTag();
    }

    private void readRequest(XMLEvent event) throws XMLStreamException {
        if (isOpen(event, OS_URI, "getObjectRequest")) {
            request = readGetObjectRequest();
        } else if (isOpen(event, OS_URI, "infoRequest")) {
            request = readInfoRequest();
        } else if (isOpen(event, OS_URI, "searchRequest")) {
            request = readSearchRequest();
        } else {
            throw new XMLStreamException("Expected OpenSearch getObjectRequest, infoRequest or searchRequest", event.getLocation());
        }
    }

    private SearchRequest readSearchRequest() throws XMLStreamException {
        SearchRequest searchRequest = new SearchRequest();
        for (;;) {
            XMLEvent event = reader.nextTag();
            if (isClose(event, OS_URI, "searchRequest")) {
                break;
            }
            if (!event.isStartElement()) {
                throw new XMLStreamException("Expected searchRequest close or request parameter", event.getLocation());
            }
            StartElement element = event.asStartElement();
            Location location = element.getLocation();
            QName qname = element.getName();
            if (!OS_URI.equals(qname.getNamespaceURI())) {
                throw new XMLStreamException("Expected searchRequest close or request parameter", event.getLocation());
            }
            String name = qname.getLocalPart();
            if (!readCommonRequestElement(searchRequest, name, location)) {
                switch (name) {
                    case "allObjects":
                        searchRequest.setAllObjects(readTextAndClose(name), location);
                        break;
                    case "collapseHitsThreshold":
                        searchRequest.setCollapseHitsThreshold(readTextAndClose(name), location);
                        break;
                    case "collectionType":
                        searchRequest.setCollectionType(readTextAndClose(name), location);
                        break;
                    case "objectFormat":
                        searchRequest.addObjectFormat(readTextAndClose(name), location);
                        break;
                    case "query":
                        searchRequest.setQuery(readTextAndClose(name), location);
                        break;
                    case "queryDebug":
                        searchRequest.setQueryDebug(readTextAndClose(name), location);
                        break;
                    case "queryLanguage":
                        searchRequest.setQueryLanguage(readTextAndClose(name), location);
                        break;
                    case "sort":
                        searchRequest.addSort(readTextAndClose(name), location);
                        break;
                    case "start":
                        searchRequest.setStart(readTextAndClose(name), location);
                        break;
                    case "stepValue":
                        searchRequest.setStepValue(readTextAndClose(name), location);
                        break;

                    case "facets":
                        searchRequest.addFacets(readFacets(location), location);
                        break;
                    case "userDefinedBoost":
                        searchRequest.addUserDefinedBoost(readUserDefinedBoost(location), location);
                        break;
                    case "userDefinedRanking":
                        searchRequest.addUserDefinedRanking(readUserDefinedRanking(location), location);
                        break;
                    default:
                        throw new XMLStreamException("Unknown request property " + name, event.getLocation());
                }
            }
        }
        searchRequest.validate();
        return searchRequest;
    }

    private GetObjectRequest readGetObjectRequest() throws XMLStreamException {
        GetObjectRequest getObjectRequest = new GetObjectRequest();
        for (;;) {
            XMLEvent event = reader.nextTag();
            if (isClose(event, OS_URI, "getObjectRequest")) {
                break;
            }
            if (!event.isStartElement()) {
                throw new XMLStreamException("Expected getObjectRequest close or request parameter", event.getLocation());
            }
            StartElement element = event.asStartElement();
            Location location = element.getLocation();
            QName qname = element.getName();
            if (!OS_URI.equals(qname.getNamespaceURI())) {
                throw new XMLStreamException("Expected getObjectRequest close or request parameter", event.getLocation());
            }
            String name = qname.getLocalPart();
            if (!readCommonRequestElement(getObjectRequest, name, location)) {
                switch (name) {
                    case "identifier":
                        getObjectRequest.addIdentifier(readTextAndClose(name), location);
                        break;
                    case "localIdentifier":
                        getObjectRequest.addLocalIdentifier(readTextAndClose(name), location);
                        break;
                    case "objectFormat":
                        getObjectRequest.addObjectFormat(readTextAndClose(name), location);
                        break;

                    case "agencyAndLocalIdentifer":
                        getObjectRequest.addAgencyAndLocalIdentifier(readAgencyAndLocalIdentifier(location), location);
                        break;
                    default:
                        throw new XMLStreamException("Unknown request property " + name, event.getLocation());
                }
            }
        }
        getObjectRequest.validate();
        return getObjectRequest;
    }

    private InfoRequest readInfoRequest() throws XMLStreamException {
        InfoRequest infoRequest = new InfoRequest();
        for (;;) {
            XMLEvent event = reader.nextTag();
            if (isClose(event, OS_URI, "infoRequest")) {
                break;
            }
            if (!event.isStartElement()) {
                throw new XMLStreamException("Expected infoRequest close or request parameter", event.getLocation());
            }
            StartElement element = event.asStartElement();
            Location location = element.getLocation();
            QName qname = element.getName();
            if (!OS_URI.equals(qname.getNamespaceURI())) {
                throw new XMLStreamException("Expected infoRequest close or request parameter", event.getLocation());
            }
            String name = qname.getLocalPart();
            if (!readBaseRequestElement(infoRequest, name, location)) {
                switch (name) {
                    // This is unnessecary, but to be like other request parsers...
                    default:
                        throw new XMLStreamException("Unknown request property " + name, event.getLocation());
                }
            }
        }
        infoRequest.validate();
        return infoRequest;
    }

    private boolean readBaseRequestElement(BaseRequest getObjectRequest, String name, Location location) throws XMLStreamException {
        switch (name) {
            case "agency":
                getObjectRequest.setAgency(readTextAndClose(name), location);
                return true;
            case "callback":
                getObjectRequest.setCallback(readTextAndClose(name), location);
                return true;
            case "outputType":
                getObjectRequest.setOutputType(readTextAndClose(name), location);
                return true;
            case "profile":
                getObjectRequest.addProfile(readTextAndClose(name), location);
                return true;
            case "trackingId":
                getObjectRequest.setTrackingId(readTextAndClose(name), location);
                return true;
            default:
                return false;
        }
    }

    private boolean readCommonRequestElement(CommonRequest getObjectRequest, String name, Location location) throws XMLStreamException {
        if (readBaseRequestElement(getObjectRequest, name, location))
            return true;
        switch (name) {
            case "includeHoldingsCount":
                getObjectRequest.setIncludeHoldingsCount(readTextAndClose(name), location);
                return true;
            case "relationData":
                getObjectRequest.setRelationData(readTextAndClose(name), location);
                return true;
            case "repository":
                getObjectRequest.setRepository(readTextAndClose(name), location);
                return true;
            case "showAgency":
                getObjectRequest.setShowAgency(readTextAndClose(name), location);
                return true;

            case "authentication":
                getObjectRequest.setAuthentication(readAuthentication(location), location);
                return true;
            default:
                return false;
        }
    }

    private Authentication readAuthentication(Location openLocation) throws XMLStreamException {
        Authentication auth = new Authentication();
        for (;;) {
            XMLEvent event = reader.nextTag();
            if (isClose(event, OS_URI, "authentication")) {
                break;
            }
            if (!event.isStartElement()) {
                throw new XMLStreamException("Expected authentication close or authentication parameter", event.getLocation());
            }
            StartElement element = event.asStartElement();
            Location location = element.getLocation();
            QName qname = element.getName();
            if (!OS_URI.equals(qname.getNamespaceURI())) {
                throw new XMLStreamException("Expected authentication close or authentication parameter", event.getLocation());
            }
            String name = qname.getLocalPart();
            switch (name) {
                case "groupIdAut":
                    auth.setGroupIdAut(readTextAndClose(name), location);
                    break;
                case "passwordAut":
                    auth.setPasswordAut(readTextAndClose(name), location);
                    break;
                case "userIdAut":
                    auth.setUserIdAut(readTextAndClose(name), location);
                    break;
                default:
                    throw new XMLStreamException("Unknown authentication property " + name, event.getLocation());
            }
        }
        auth.validate(openLocation);
        return auth;
    }

    private Facets readFacets(Location openLocation) throws XMLStreamException {
        Facets facet = new Facets();
        for (;;) {
            XMLEvent event = reader.nextTag();
            if (isClose(event, OS_URI, "facets")) {
                break;
            }
            if (!event.isStartElement()) {
                throw new XMLStreamException("Expected facets close or facets parameter", event.getLocation());
            }
            StartElement element = event.asStartElement();
            Location location = element.getLocation();
            QName qname = element.getName();
            if (!OS_URI.equals(qname.getNamespaceURI())) {
                throw new XMLStreamException("Expected facets close or facets parameter", event.getLocation());
            }
            String name = qname.getLocalPart();
            switch (name) {
                case "facetName":
                    facet.addFacetName(readTextAndClose(name), location);
                    break;
                case "facetMinCount":
                    facet.setFacetMinCount(readTextAndClose(name), location);
                    break;
                case "facetOffset":
                    facet.setFacetOffset(readTextAndClose(name), location);
                    break;
                case "facetSort":
                    facet.setFacetSort(readTextAndClose(name), location);
                    break;
                case "numberOfTerms":
                    facet.setNumberOfTerms(readTextAndClose(name), location);
                    break;
                default:
                    throw new XMLStreamException("Unknown facets property " + name, event.getLocation());
            }
        }
        facet.validate(openLocation);
        return facet;
    }

    private UserDefinedRanking readUserDefinedRanking(Location openLocation) throws XMLStreamException {
        UserDefinedRanking userDefinedRanking = new UserDefinedRanking();
        for (;;) {
            XMLEvent event = reader.nextTag();
            if (isClose(event, OS_URI, "userDefinedRanking")) {
                break;
            }
            if (!event.isStartElement()) {
                throw new XMLStreamException("Expected userDefinedRanking close or userDefinedRanking parameter", event.getLocation());
            }
            StartElement element = event.asStartElement();
            Location location = element.getLocation();
            QName qname = element.getName();
            if (!OS_URI.equals(qname.getNamespaceURI())) {
                throw new XMLStreamException("Expected userDefinedRanking close or userDefinedRanking parameter", event.getLocation());
            }
            String name = qname.getLocalPart();
            switch (name) {
                case "tieValue":
                    userDefinedRanking.setTieValue(readTextAndClose(name), location);
                    break;
                case "rankField":
                    userDefinedRanking.addRankField(readRankField(location), location);
                    break;
                default:
                    throw new XMLStreamException("Unknown userDefinedRanking property " + name, event.getLocation());
            }
        }
        userDefinedRanking.validate(openLocation);
        return userDefinedRanking;
    }

    private UserDefinedBoost readUserDefinedBoost(Location openLocation) throws XMLStreamException {
        UserDefinedBoost boost = new UserDefinedBoost();
        for (;;) {
            XMLEvent event = reader.nextTag();
            if (isClose(event, OS_URI, "userDefinedBoost")) {
                break;
            }
            if (!event.isStartElement()) {
                throw new XMLStreamException("Expected userDefinedBoost close or userDefinedBoost parameter", event.getLocation());
            }
            StartElement element = event.asStartElement();
            Location location = element.getLocation();
            QName qname = element.getName();
            if (!OS_URI.equals(qname.getNamespaceURI())) {
                throw new XMLStreamException("Expected userDefinedBoost close or userDefinedBoost parameter", event.getLocation());
            }
            String name = qname.getLocalPart();
            switch (name) {
                case "fieldName":
                    boost.setFieldName(readTextAndClose(name), location);
                    break;
                case "fieldValue":
                    boost.setFieldValue(readTextAndClose(name), location);
                    break;
                case "weight":
                    boost.setWeight(readTextAndClose(name), location);
                    break;
                default:
                    throw new XMLStreamException("Unknown userDefinedBoost property " + name, event.getLocation());
            }
        }
        boost.validate(openLocation);
        return boost;
    }

    private RankField readRankField(Location openLocation) throws XMLStreamException {
        RankField rankField = new RankField();
        for (;;) {
            XMLEvent event = reader.nextTag();
            if (isClose(event, OS_URI, "rankField")) {
                break;
            }
            if (!event.isStartElement()) {
                throw new XMLStreamException("Expected rankField close or rankField parameter", event.getLocation());
            }
            StartElement element = event.asStartElement();
            Location location = element.getLocation();
            QName qname = element.getName();
            if (!OS_URI.equals(qname.getNamespaceURI())) {
                throw new XMLStreamException("Expected rankField close or rankField parameter", event.getLocation());
            }
            String name = qname.getLocalPart();
            switch (name) {
                case "fieldName":
                    rankField.setFieldName(readTextAndClose(name), location);
                    break;
                case "fieldType":
                    rankField.setFieldType(readTextAndClose(name), location);
                    break;
                case "weight":
                    rankField.setWeight(readTextAndClose(name), location);
                    break;
                default:
                    throw new XMLStreamException("Unknown rankField property " + name, event.getLocation());
            }
        }
        rankField.validate(openLocation);
        return rankField;
    }

    private AgencyAndLocalIdentifier readAgencyAndLocalIdentifier(Location location) {
        AgencyAndLocalIdentifier agencyAndLocalIdentifier = new AgencyAndLocalIdentifier();

        return agencyAndLocalIdentifier;
    }

    private static boolean isOpen(XMLEvent element, String namespace, String name) {
        if (!element.isStartElement()) {
            return false;
        }
        QName qname = element.asStartElement().getName();
        return namespace.equals(qname.getNamespaceURI()) &&
               name.equals(qname.getLocalPart());
    }

    private static boolean isClose(XMLEvent element, String namespace, String name) {
        if (!element.isEndElement()) {
            return false;
        }
        QName qname = element.asEndElement().getName();
        return namespace.equals(qname.getNamespaceURI()) &&
               name.equals(qname.getLocalPart());
    }

    private String readTextAndClose(String name) throws XMLStreamException {
        StringBuilder buffer = new StringBuilder();
        XMLEvent event;
        for (;;) {
            event = reader.nextEvent();
            if (!event.isCharacters()) {
                break;
            }
            buffer.append(event.asCharacters().getData());
        }
        if (!isClose(event, OS_URI, name)) {
            throw new XMLStreamException("Expected " + name + " close", event.getLocation());
        }
        return buffer.toString();
    }

    private static XMLInputFactory makeXMLInputFactory() {
        synchronized (XMLInputFactory.class) {
            return XMLInputFactory.newInstance();
        }
    }

}
