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

import dk.dbc.opensearch.setup.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class XMLScope extends EventReaderDelegate {

    /**
     * Namespace prefix normalizing XMLEvent accumulator
     */
    public static class Writer implements XMLEventWriter {

        private final List<XMLEvent> list;
        private final NamespaceHandler ns;

        public Writer(Settings settings) {
            this.list = new ArrayList<>();
            this.ns = new NamespaceHandler(settings);
        }

        @Override
        public void flush() throws XMLStreamException {
        }

        @Override
        public void close() throws XMLStreamException {
        }

        @Override
        public void add(XMLEvent event) throws XMLStreamException {
            list.add(ns.fixPrefix(event));
        }

        @Override
        public void add(XMLEventReader reader) throws XMLStreamException {
            while (reader.hasNext()) {
                add(reader.nextEvent());
            }
        }

        @Override
        public String getPrefix(String uri) throws XMLStreamException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setPrefix(String prefix, String uri) throws XMLStreamException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setDefaultNamespace(String uri) throws XMLStreamException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public NamespaceContext getNamespaceContext() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /**
         * Replace opening tag with one that includes specification of all used
         * namespaces
         *
         * @return XMLEventReader type
         * @throws XMLStreamException if the new element cannot be made or
         *                            opening event isn't of StartElement type
         */
        public XMLScope toReader() throws XMLStreamException {
            XMLEvent[] events = list.stream().toArray(XMLEvent[]::new);
            if (!events[0].isStartElement())
                throw new XMLStreamException("Should have started with a StartElement");
            events[0] = ns.startElementWithNamespaces(events[0].asStartElement());
            return new XMLScope(events);

        }
    }

    /**
     * Construct an XMLEventReader for a given element
     *
     * @param startElement The opening tag of the element
     * @param reader       the reader that supplies events
     * @param settings     namespace prefix normalization rules
     * @return An XMLEventReader that encompasses the entire element
     * @throws XMLStreamException If there's a XML syntax error
     */
    public static XMLScope of(StartElement startElement, XMLEventReader reader, Settings settings) throws XMLStreamException {
        Writer writer = new Writer(settings);
        writer.add(startElement);
        int nesting = 1;
        while (nesting != 0 && reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            writer.add(event);
            if (event.isEndElement())
                nesting--;
            else if (event.isStartElement())
                nesting++;
        }
        if (nesting != 0)
            throw new XMLStreamException("Unexpected EOF");
        return writer.toReader();
    }

    private static final XMLEventFactory E = makeXMLEventFactory();

    private final XMLEvent[] events;
    private int pos;

    /**
     * @param events List of events describing this scope
     */
    private XMLScope(XMLEvent[] events) {
        this.events = events;
        this.pos = 0;
    }

    @Override
    public boolean hasNext() {
        return pos < events.length;
    }

    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        return events[pos++];
    }

    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        while (hasNext()) {
            XMLEvent e = nextEvent();
            if (e.isCharacters() && !e.asCharacters().getData().trim().isEmpty())
                throw new XMLStreamException("Got non skipable text looking for tag", e.getLocation());
            if (e.isStartElement() || e.isEndElement())
                return e;
        }
        throw new XMLStreamException("Got EOF looking for tag");
    }

    @Override
    public XMLEvent peek() throws XMLStreamException {
        if (hasNext())
            return events[pos];
        return null;
    }

    /**
     * Class for formalizing namespaces according to predefined prefixes
     */
    private static class NamespaceHandler {

        private final Settings settings;
        // Namespaces that aren't predefined (which is an error)
        private final HashMap<String, String> generatedNamespaces;
        private final HashMap<String, String> usedNamespaces;

        private NamespaceHandler(Settings settings) {
            this.settings = settings;
            this.generatedNamespaces = new HashMap<>();
            this.usedNamespaces = new HashMap<>();
        }

        /**
         * Collect the namespace usage
         *
         * @param uri the namespace uri
         * @return the expected prefix
         */
        private String collect(String uri) {
            if (uri.isEmpty())
                return "";
            return usedNamespaces.computeIfAbsent(
                    uri,
                    u -> settings.lookupNamespacePrefix(u, generatedNamespaces));
        }

        /**
         * Run through attributes and record/normalize their namespaces
         *
         * @param attributes iterator of attributes to normalize
         * @return null if no changes are made of new iterator with all
         *         attributes
         */
        private Iterator<Attribute> fixAttributes(Iterator<Attribute> attributes) {
            ArrayList<Attribute> attrs = new ArrayList<>();
            boolean fixed = false;
            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                QName name = attribute.getName();
                String uri = name.getNamespaceURI();
                String prefix = collect(uri);
                if (!name.getPrefix().equals(prefix)) {
                    attribute = E.createAttribute(prefix, uri, name.getLocalPart(), attribute.getValue());
                }
                attrs.add(attribute);
            }
            return fixed ? attrs.iterator() : null;
        }

        /**
         * Fin the prefix of an XMLEvent
         * <p>
         * Only alters for StartElement and EndElement
         *
         * @param e element
         * @return same (semantic) element, with normalized namespace prefix
         */
        private XMLEvent fixPrefix(XMLEvent e) {
            if (e.isStartElement()) {
                StartElement start = e.asStartElement();
                QName name = start.getName();
                String uri = name.getNamespaceURI();
                String prefix = collect(uri);
                Iterator attributes = fixAttributes(start.getAttributes());
                if (attributes != null || !name.getPrefix().equals(prefix)) {
                    if (attributes == null)
                        attributes = start.getAttributes();
                    e = E.createStartElement(
                            prefix, uri, name.getLocalPart(),
                            attributes, start.getNamespaces());
                }
            }
            if (e.isEndElement()) {
                EndElement end = e.asEndElement();
                QName name = end.getName();
                String uri = name.getNamespaceURI();
                String prefix = collect(uri);
                if (!name.getPrefix().equals(prefix))
                    e = E.createEndElement(prefix, uri, name.getLocalPart());
            }
            return e;
        }

        /**
         * Compute the StartElement (first element) to include namespace/prefix
         * defines for all the used namespaces
         *
         * @param start StartElement to alter
         * @return altered element
         */
        private StartElement startElementWithNamespaces(StartElement start) {
            ArrayList nsList = new ArrayList();
            start.getNamespaces().forEachRemaining(nsList::add);
            usedNamespaces.forEach((uri, prefix) -> nsList.add(E.createNamespace(prefix, uri)));
            QName name = start.getName();
            String uri = name.getNamespaceURI();
            String prefix = usedNamespaces.get(uri);
            Iterator attributes = fixAttributes(start.getAttributes());
            if (attributes == null)
                attributes = start.getAttributes();
            return E.createStartElement(
                    prefix, uri, name.getLocalPart(),
                    attributes, nsList.iterator());
        }
    }

    private static XMLEventFactory makeXMLEventFactory() {
        synchronized (XMLEventFactory.class) {
            return XMLEventFactory.newInstance();
        }
    }

}
