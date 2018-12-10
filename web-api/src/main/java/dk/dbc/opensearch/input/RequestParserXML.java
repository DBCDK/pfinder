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
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import static javax.xml.stream.XMLStreamConstants.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class RequestParserXML extends RequestParser {

    private static final XMLInputFactory I = makeXMLInputFactory();
    private static final int UNWANTED_EVENTS =
            maskOf(PROCESSING_INSTRUCTION) | maskOf(COMMENT) | maskOf(SPACE) |
            maskOf(START_DOCUMENT) | maskOf(END_DOCUMENT) |
            maskOf(ENTITY_REFERENCE) | maskOf(ATTRIBUTE) |
            maskOf(DTD) | maskOf(NAMESPACE) | maskOf(NOTATION_DECLARATION) |
            maskOf(ENTITY_DECLARATION);

    public static final String OS_URI = "http://oss.dbc.dk/ns/opensearch";
    public static final String SOAP_URI = "http://schemas.xmlsoap.org/soap/envelope/";

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
        return ( UNWANTED_EVENTS & maskOf(bitNo) ) == 0;
    }

    public RequestParserXML(InputStream is) throws XMLStreamException {
        this(readOuterMost(is));
    }

    private RequestParserXML(Map.Entry<BaseRequest, OutputType> entry) {
        super(entry.getKey(), entry.getValue());
    }

    private static Map.Entry<BaseRequest, OutputType> readOuterMost(InputStream is) throws XMLStreamException {
        XMLEventReader reader = I.createFilteredReader(
                I.createXMLEventReader(is),
                e -> isWanted(e.getEventType()));
        XMLEvent event = reader.nextEvent();
        if (!event.isStartElement()) {
            throw new XMLStreamException("Expected tag as opening of request", event.getLocation());
        }
        if (isOpen(event, SOAP_URI, "Envelope")) {
            BaseRequest request = readSoapEnvelope(reader);
            return new AbstractMap.SimpleEntry(request, OutputType.SOAP);
        } else {
            BaseRequest request = readRequest(event, reader);
            return new AbstractMap.SimpleEntry(request, OutputType.XML);
        }
    }

    private static BaseRequest readSoapEnvelope(XMLEventReader reader) throws XMLStreamException {
        XMLEvent event = reader.nextTag();
        event = readSoapHeader(event, reader);
        BaseRequest request;
        if (isOpen(event, SOAP_URI, "Body")) {
            event = reader.nextTag();
            request = readRequest(event, reader);
        } else {
            throw new XMLStreamException("Expected Opening tag of SOAP Body", event.getLocation());
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
        return request;
    }

    private static XMLEvent readSoapHeader(XMLEvent event, XMLEventReader reader) throws XMLStreamException {
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

    private static BaseRequest readRequest(XMLEvent event, XMLEventReader reader) throws XMLStreamException {
        if (isOpen(event, OS_URI, "getObjectRequest")) {
            return GetObjectRequest.FACTORY.from(event.asStartElement(), reader);
        } else if (isOpen(event, OS_URI, "infoRequest")) {
            return InfoRequest.FACTORY.from(event.asStartElement(), reader);
        } else if (isOpen(event, OS_URI, "searchRequest")) {
            return SearchRequest.FACTORY.from(event.asStartElement(), reader);
        } else {
            throw new XMLStreamException("Expected OpenSearch getObjectRequest, infoRequest or searchRequest", event.getLocation());
        }
    }

    private static boolean isOpen(XMLEvent element, String namespace, String name) {
        if (!element.isStartElement())
            return false;
        QName qname = element.asStartElement().getName();
        return namespace.equals(qname.getNamespaceURI()) &&
               name.equals(qname.getLocalPart());
    }

    private static boolean isClose(XMLEvent element, String namespace, String name) {
        if (!element.isEndElement())
            return false;
        QName qname = element.asEndElement().getName();
        return namespace.equals(qname.getNamespaceURI()) &&
               name.equals(qname.getLocalPart());
    }

    private static XMLInputFactory makeXMLInputFactory() {
        synchronized (XMLInputFactory.class) {
            return XMLInputFactory.newInstance();
        }
    }

}