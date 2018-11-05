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
package dk.dbc.opensearch.output;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import static java.util.Collections.EMPTY_LIST;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class EventOutput implements AutoCloseable {

    @FunctionalInterface
    public interface WriteEvents {

        void output() throws XMLStreamException;
    }

    private static final XMLEventFactory E = makeXMLEventFactory();
    private static final XMLOutputFactory O = makeXMLOutputFactory();

    private static XMLEventFactory makeXMLEventFactory() {
        synchronized (XMLEventFactory.class) {
            return XMLEventFactory.newInstance();
        }
    }

    private static XMLOutputFactory makeXMLOutputFactory() {
        synchronized (XMLOutputFactory.class) {
            return XMLOutputFactory.newInstance();
        }
    }

    private final XMLEventWriter writer;

    public EventOutput(XMLEventWriter writer) {
        this.writer = writer;
    }

    public EventOutput(OutputStream os) throws XMLStreamException {
        this.writer = O.createXMLEventWriter(os, "UTF-8");
    }
    /**
     * Flush output.
     * <p>
     * If your last output was a element-start, it might not have been completed
     * (send empty text first)
     *
     * @throws XMLStreamException
     */
    public void flush() throws XMLStreamException {
        writer.flush();
    }

    @Override
    public void close() throws XMLStreamException {
        writer.close();
    }

    public void soapEnvelopeSearchResponse(WriteEvents content) throws XMLStreamException {
        writer.add(EVENT.START_DOCUMENT);
        writer.add(EVENT.SOAP_ENV_OPEN);
        writer.add(EVENT.SOAP_BODY_OPEN);
        writer.add(EVENT.SEARCH_RESPONSE_OPEN);
        content.output();
        writer.add(EVENT.SEARCH_RESPONSE_CLOSE);
        writer.add(EVENT.SOAP_BODY_CLOSE);
        writer.add(EVENT.SOAP_ENV_CLOSE);
    }

    public void searchResponse(WriteEvents content) throws XMLStreamException {
        writer.add(EVENT.START_DOCUMENT);
        writer.add(EVENT.SEARCH_RESPONSE_OPEN_NS);
        content.output();
        writer.add(EVENT.SEARCH_RESPONSE_CLOSE);
    }

    public XMLEventWriter getXMLEventWriter() {
        return writer;
    }

    public void stream(XMLEventReader reader) throws XMLStreamException {
        writer.add(reader);
    }

    public void agency(int content) throws XMLStreamException {
        agency(String.format(Locale.ROOT, "%06d", content));
    }

__SHELL__
perl -e 'for(sort@ARGV){($tag,$type)=split("=",$_,2);$name=$tag;$name=~s{(?<=[a-z])(?=[A-Z])}{_}g;$name=uc$name;print("public void $tag($type content) throws XMLStreamException {writer.add(EVENT.${name}_OPEN);writer.add(E.createCharacters(String.valueOf(content)));writer.add(EVENT.${name}_CLOSE);}\n")}'  \
accessType=String access=String agency=String allObjects=boolean callback=String collapseHitsThreshold=String collectionCount=int collectionType=String cqlIndexDoc=String creationDate=String defaultRepository=String error=String facetMinCount=int facetName=String facetOffset=int facetSort=String fedoraRecordsCached=int fedoraRecordsRead=int fieldName=String fieldType=String fieldValue=String format=String frequence=int groupIdAut=String hitCount=int holdingsCount=int identifier=String includeHoldingsCount=boolean includeMarcXchange=boolean indexName=String indexSlop=int internalType=String lendingLibraries=int linkCollectionIdentifier=String linkTo=String localIdentifier=String more=boolean numberOfObjects=int numberOfTerms=int objectFormat=String outputType=String parsedQueryString=String parsedQuery=String passwordAut=String prefix=String primaryObjectIdentifier=String profile=String queryDebug=boolean queryLanguage=String queryResultExplanation=String queryString=String query=String rawQueryString=String recordStatus=String relationData=String relationType=String relationUri=String repository=String resultPosition=int searchCollectionIdentifier=String searchCollectionIsSearched=boolean searchCollectionName=String showAgency=String sort=String sortUsed=String source=String start=int stepValue=int term=String tieValue=double time=double trackingId=String uri=String userIdAut=String weight=double
perl -e 'for(sort@ARGV){($tag,$type)=split("=",$_,2);$name=$tag;$name=~s{(?<=[a-z])(?=[A-Z])}{_}g;$name=uc$name;print("public void ${tag}(WriteEvents content) throws XMLStreamException {writer.add(EVENT.${name}_OPEN);content.output();writer.add(EVENT.${name}_CLOSE);}\n")}' \
agencyAndLocalIdentifier authentication collection cqlIndex facet facetResult facets facetTerm fieldNameAndWeight formatsAvailable formattedCollection getObjectRequest indexAlias infoGeneral infoNameSpace infoNameSpaces infoObjectFormats infoRepositories infoRepository infoRequest infoResponse infoSearchProfile infoSort infoSorts linkObject object objectsAvailable phrase queryDebugResult rankDetails rankField rankFrequency relation relationObject relations relationTypes result searchCollection searchRequest searchResult sortDetails statInfo userDefinedBoost userDefinedRanking word

    private static class EVENT {

        private static final String SOAP = "soap";
        private static final String SOAP_URI = "http://schemas.xmlsoap.org/soap/envelope/";
        private static final XMLEvent SOAP_NS = E.createNamespace(SOAP, SOAP_URI);
        private static final String OS = "os";
        private static final String OS_URI = "http://oss.dbc.dk/ns/opensearch";
        private static final XMLEvent OS_NS = E.createNamespace(OS, OS_URI);

        private static XMLEvent soapElementOpen(String tag, XMLEvent... namespaces) {
            return E.createStartElement(SOAP, SOAP_URI, tag,
                                        EMPTY_LIST.iterator(),
                                        Arrays.asList(namespaces).iterator());
        }

        private static XMLEvent soapElementClose(String tag) {
            return E.createEndElement(SOAP, SOAP_URI, tag);
        }

        private static XMLEvent elementOpen(String tag, XMLEvent... namespaces) {
            return E.createStartElement(OS, OS_URI, tag,
                                        EMPTY_LIST.iterator(),
                                        Arrays.asList(namespaces).iterator());
        }

        private static XMLEvent elementClose(String tag) {
            return E.createEndElement(OS, OS_URI, tag);
        }

        private static final XMLEvent START_DOCUMENT = E.createStartDocument("utf-8", "1.0", true);
        private static final XMLEvent SOAP_ENV_OPEN = soapElementOpen("Envelope", SOAP_NS, OS_NS);
        private static final XMLEvent SOAP_BODY_OPEN = soapElementOpen("Body");
        private static final XMLEvent SOAP_BODY_CLOSE = soapElementClose("Body");
        private static final XMLEvent SOAP_ENV_CLOSE = soapElementClose("Envelope");
        private static final XMLEvent SEARCH_RESPONSE_OPEN_NS = elementOpen("searchResponse", OS_NS);

__SHELL__
perl -e 'for$tag(sort@ARGV){$name=$tag;$name=~s{(?<=[a-z])(?=[A-Z])}{_}g;$name=uc$name;print("private static final XMLEvent ${name}_OPEN = elementOpen(\"${tag}\");\nprivate static final XMLEvent ${name}_CLOSE = elementClose(\"${tag}\");\n")}' \
accessType access agency allObjects callback collapseHitsThreshold collectionCount collectionType cqlIndexDoc creationDate defaultRepository error facetMinCount facetName facetOffset facetSort fedoraRecordsCached fedoraRecordsRead fieldName fieldType fieldValue format frequence groupIdAut hitCount holdingsCount identifier includeHoldingsCount includeMarcXchange indexName indexSlop internalType lendingLibraries linkCollectionIdentifier linkTo localIdentifier more numberOfObjects numberOfTerms objectFormat outputType parsedQueryString parsedQuery passwordAut prefix primaryObjectIdentifier profile queryDebug queryLanguage queryResultExplanation queryString query rawQueryString recordStatus relationData relationType relationUri repository resultPosition searchCollectionIdentifier searchCollectionIsSearched searchCollectionName showAgency sort sortUsed source start stepValue term tieValue time trackingId uri userIdAut weight \
agencyAndLocalIdentifier authentication collection cqlIndex facet facetResult facets facetTerm fieldNameAndWeight formatsAvailable formattedCollection getObjectRequest indexAlias infoGeneral infoNameSpace infoNameSpaces infoObjectFormats infoRepositories infoRepository infoRequest infoResponse infoSearchProfile infoSort infoSorts linkObject object objectsAvailable phrase queryDebugResult rankDetails rankField rankFrequency relation relationObject relations relationTypes result searchCollection searchRequest searchResponse searchResult sortDetails statInfo userDefinedBoost userDefinedRanking word

    }

}
