/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-web-api
 *
 * opensearch-web-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-web-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.output.badgerfish;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import static javax.xml.stream.XMLStreamConstants.*;

/**
 * An {@link XMLEventWriter} that outputs BadgerFish JSON
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class BadgerFishWriter implements XMLEventWriter {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private static final int EVENT_FILTER =
            maskOf(PROCESSING_INSTRUCTION) | maskOf(COMMENT) | maskOf(SPACE) |
            maskOf(START_DOCUMENT) | maskOf(END_DOCUMENT) |
            maskOf(ENTITY_REFERENCE) | maskOf(ATTRIBUTE) |
            maskOf(DTD) | maskOf(NAMESPACE) | maskOf(NOTATION_DECLARATION) |
            maskOf(ENTITY_DECLARATION);

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

    private final Context cxt;

    /**
     * Build and initialize root object
     *
     * @param os     output stream
     * @param single which tags are ensured to be non repeated
     * @throws IOException if initialization of jackson fails
     */
    public BadgerFishWriter(OutputStream os, BadgerFishSingle single) throws IOException {
        this(os, single, BadgerFishNamespace.DEFAULT_NAMESPACE_MAP);
    }

    /**
     * Build and initialize root object
     *
     * @param os                      output stream
     * @param single                  which tags are ensured to be non repeated
     * @param defaultNamespaceMapping mapping from namespaceuri to symbolic
     *                                name
     * @throws IOException if initialization of jackson fails
     */
    public BadgerFishWriter(OutputStream os, BadgerFishSingle single, Map<String, String> defaultNamespaceMapping) throws IOException {
        JsonGenerator out = JSON_FACTORY.createGenerator(os);
        cxt = new Context(out, new BadgerFishStack(), single, defaultNamespaceMapping);
        Root root = new Root(cxt);
        cxt.stack.addConsumer(root);
    }

    @Override
    public void flush() throws XMLStreamException {
        try {
            cxt.out.flush();
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public void close() throws XMLStreamException {
        try {
            cxt.out.close();
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public void add(XMLEvent event) throws XMLStreamException {
        if (isWanted(event.getEventType())) {
            try {
                cxt.stack.consume(event);
            } catch (IOException ex) {
                throw new XMLStreamException(ex);
            }
        }
    }

    @Override
    public void add(XMLEventReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            add(reader.nextEvent());
        }
    }

    @Override
    public String getPrefix(String string) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPrefix(String string, String string1) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDefaultNamespace(String string) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setNamespaceContext(NamespaceContext nc) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
