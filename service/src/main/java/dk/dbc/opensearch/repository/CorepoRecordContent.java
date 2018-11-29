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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.opensearch.setup.Settings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.xml.XMLConstants.NULL_NS_URI;
import static javax.xml.stream.XMLStreamConstants.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class CorepoRecordContent implements RecordContent {

    private static final Logger log = LoggerFactory.getLogger(CorepoRecordContent.class);

    private static final ObjectMapper O = makeObjectMapper();
    private static final XMLInputFactory I = makeXMLInputFactory();
    private static final int UNWANTED_EVENTS =
            maskOf(PROCESSING_INSTRUCTION) | maskOf(COMMENT) | maskOf(SPACE) |
            maskOf(START_DOCUMENT) | maskOf(END_DOCUMENT) |
            maskOf(ENTITY_REFERENCE) | maskOf(ATTRIBUTE) |
            maskOf(DTD) | maskOf(NAMESPACE) | maskOf(NOTATION_DECLARATION) |
            maskOf(ENTITY_DECLARATION);

    private final HashMap<String, XMLScope> rawRecords;
    private final HashMap<String, XMLScope> formattedRecords;
    private String recordStatus;
    private String creationDate;
    private final List<String> objectsAvailable;
    private final String primaryObjectIdentifier;

    public CorepoRecordContent(InputStream is, Map<QName, String> formatSpecs, Settings settings) throws IOException, XMLStreamException {
        this.rawRecords = new HashMap<>();
        this.formattedRecords = new HashMap<>();
        Payload payload = O.readValue(is, Payload.class);
        this.objectsAvailable = payload.getPids();
        this.primaryObjectIdentifier = payload.getPrimaryPid();
        log.trace("payload = {}", payload);
        byte[] bytes = payload.getDataStream().getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            XMLEventReader reader = filteredReader(bis);
            parseXML(reader, formatSpecs, settings);
            reader.close();
        }
    }

    private XMLEventReader filteredReader(InputStream is) throws XMLStreamException {
        return I.createFilteredReader(
                I.createXMLEventReader(is),
                e -> isWanted(e.getEventType()));
    }

    @Override
    public String getCreationDate() {
        return creationDate;
    }

    @Override
    public XMLScope getRawFormat(String format) {
        return rawRecords.get(format);
    }

    @Override
    public XMLScope getFormattedRecord(String format) {
        return formattedRecords.get(format);
    }

    public void addFormattedRecord(String format, XMLScope content) {
        formattedRecords.put(format, content);
    }

    @Override
    public List<String> getFormatsAvailable() {
        return rawRecords.keySet().stream().sorted().collect(Collectors.toList());
    }

    @Override
    public List<String> getObjectsAvailable() {
        return objectsAvailable;
    }

    @Override
    public String getPrimaryObjectIdentifier() {
        return primaryObjectIdentifier;
    }

    @Override
    public String getRecordStatus() {
        return recordStatus;
    }

    /**
     *
     * Parses corepo-xml extracting raw formats, and parsing adminData
     *
     * @param reader      XML parts from the response
     * @param formatSpecs QName to format name
     * @param settings    for namespace normalization of formats
     * @throws XMLStreamException in case of syntax/semantic errors in the XML
     *                            stream
     */
    private void parseXML(XMLEventReader reader, Map<QName, String> formatSpecs, Settings settings) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent tag = reader.nextEvent();
            if (tag.isStartElement()) {
                StartElement start = tag.asStartElement();
                QName name = start.getName();
                if (name.getLocalPart().equals("adminData") &&
                    name.getNamespaceURI().equals(NULL_NS_URI)) {
                    processAdminData(reader);
                } else {
                    String format = formatSpecs.get(name);
                    if (format != null) {
                        rawRecords.put(format, XMLScope.of(start, reader, settings));
                    }
                }
            }
        }
    }

    /**
     * Extract user exposed parts of the adminData element
     *
     * @param reader XML input stream
     * @throws XMLStreamException in case of syntax/semantic errors in the XML
     *                            stream
     */
    private void processAdminData(XMLEventReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent tag = reader.nextEvent();
            if (tag.isStartElement()) {
                StartElement start = tag.asStartElement();
                QName name = start.getName();
                switch (name.getLocalPart()) {
                    case "recordStatus":
                        this.recordStatus = readValue(reader);
                        break;
                    case "creationDate":
                        this.creationDate = readValue(reader);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Extract a textual value from the input stream
     *
     * @param reader Where to extract content from
     * @return empty string or content of next node
     * @throws XMLStreamException in case of syntax/semantic errors in the XML
     *                            stream
     */
    private String readValue(XMLEventReader reader) throws XMLStreamException {
        if (reader.hasNext()) {
            XMLEvent event = reader.peek();
            if (event.isCharacters())
                return event.asCharacters().getData();
        }
        return "";
    }

    @Override
    public String toString() {
        return "CorepoRecordContent{" + "rawRecords=" + rawRecords.keySet() + ", formattedRecords=" + formattedRecords.keySet() + ", recordStatus=" + recordStatus + ", creationDate=" + creationDate + ", objectsAvailable=" + objectsAvailable + ", primaryObjectIdentifier=" + primaryObjectIdentifier + '}';
    }

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

    private static ObjectMapper makeObjectMapper() {
        ObjectMapper o = new ObjectMapper();
        o.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return o;
    }

    private static XMLInputFactory makeXMLInputFactory() {
        synchronized (XMLInputFactory.class) {
            return XMLInputFactory.newInstance();
        }
    }

    /**
     * POJO representing corepo-content-service response
     */
    private static class Payload {

        private String dataStream;
        private List<String> pids;
        private String primaryPid;

        public Payload() {
        }

        public String getDataStream() {
            return dataStream;
        }

        public void setDataStream(String dataStream) {
            this.dataStream = dataStream;
        }

        public List<String> getPids() {
            return pids;
        }

        public void setPids(List<String> pids) {
            this.pids = pids;
        }

        public String getPrimaryPid() {
            return primaryPid;
        }

        public void setPrimaryPid(String primaryPid) {
            this.primaryPid = primaryPid;
        }

        @Override
        public String toString() {
            return "Payload{" + "dataStream=" + dataStream + ", pids=" + pids + ", primaryPid=" + primaryPid + '}';
        }

    }

}
