/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-utils
 *
 * opensearch-utils is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-utils is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import static dk.dbc.opensearch.xml.XMLElementFilter.isWanted;
import static dk.dbc.opensearch.xml.XMLEventFactories.E;

/**
 * an {@link XMLEventWriter} for storing an element
 * <p>
 * First event to be added to this container needs to be a {@link StartElement},
 * since this will be rewritten in {@link #toReader()} to contain all
 * declarations for all used namespaces. It is possible to have more that just
 * the one element in the container, however this will most likely fail
 * regarding to namespaces. Misuse of this feature is NOT recommended.
 * <p>
 * The {@link XMLCacheReader} does not support unmatched elements when
 * serializing
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class XMLCacheElement implements XMLEventWriter {

    private final DefaultPrefix.Instance defaultPrefix;
    private final ArrayList<XMLEvent> events;

    /**
     * Construct an {@link XMLCacheElement} from a StartElement and remainder of
     * a stream
     *
     * @param event         StartElement (tag opening)
     * @param reader        stream of events after this
     * @param defaultPrefix namespace prefix mapper
     * @return a cached element
     * @throws XMLStreamException if there's a semantic error in the input
     */
    public static XMLCacheElement of(StartElement event, XMLEventReader reader, DefaultPrefix defaultPrefix) throws XMLStreamException {
        XMLCacheElement cache = new XMLCacheElement(defaultPrefix);
        int level = 1;
        cache.add(event);
        while (level > 0 && reader.hasNext()) {
            XMLEvent e = reader.nextEvent();
            cache.add(e);
            if (e.isStartElement())
                level++;
            if (e.isEndElement())
                level--;
        }
        return cache;
    }

    public XMLCacheElement(DefaultPrefix defaultPrefix) {
        this.defaultPrefix = defaultPrefix.instance();
        this.events = new ArrayList<>();
    }

    /**
     * Convert into a cached reader
     *
     * @return a reader that encompasses the stores events
     */
    public XMLCacheReader toReader() {
        XMLEvent[] eventsArray = events.toArray(new XMLEvent[events.size()]);
        if (eventsArray.length != 0)
            eventsArray[0] = fixStartElement(eventsArray[0]);
        return new XMLCacheReader(eventsArray);
    }

    @Override
    public void flush() throws XMLStreamException {
    }

    @Override
    public void close() throws XMLStreamException {
    }

    @Override
    public void add(XMLEvent event) throws XMLStreamException {
        if (isWanted(event)) {
            events.add(fixPrefix(event));
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

    /**
     * For a given event (Start/EndElement) fic the namespace prefix
     *
     * @param event event
     * @return same event or namespace-prefix mapped
     */
    private XMLEvent fixPrefix(XMLEvent event) {
        if (event.isStartElement())
            return fixStartElementPrefix(event.asStartElement());
        if (event.isEndElement())
            return fixEndElementPrefix(event.asEndElement());
        return event;
    }

    /**
     * Convert a StartElement to one with prefix normalization
     *
     * @param event an tag open event
     * @return same or equivalent event
     */
    private XMLEvent fixStartElementPrefix(StartElement event) {
        QName name = event.getName();
        String uri = name.getNamespaceURI();
        String prefix = defaultPrefix.prefixFor(uri);
        Optional<Iterator> fixedAttributes = fixAttributePrefix(event.getAttributes());
        if (!prefix.equals(uri) ||
            fixedAttributes.isPresent() ||
            !event.getNamespaces().hasNext()) {
            event = E.createStartElement(prefix, uri, name.getLocalPart(),
                                         fixedAttributes.orElse(event.getAttributes()),
                                         Collections.EMPTY_LIST.iterator());
        }
        return event;
    }

    /**
     * Map namespaces of attributes to default namespaces
     *
     * @param src attribute iterator of attributes, that might need to have
     *            namespace prefix changed
     * @return optional empty if nothing is changed, otherwise an attribute
     *         iterator
     */
    private Optional<Iterator> fixAttributePrefix(Iterator<Attribute> src) {
        ArrayList<Attribute> attrs = new ArrayList<>();
        boolean fixed = false;
        while (src.hasNext()) {
            Attribute attr = src.next();
            QName name = attr.getName();
            String uri = name.getNamespaceURI();
            String prefix = defaultPrefix.prefixFor(uri);
            if (!prefix.equals(uri)) {
                attr = E.createAttribute(prefix, uri, name.getLocalPart(), attr.getValue());
                fixed = true;
            }
            attrs.add(attr);
        }
        if (fixed)
            return Optional.of(attrs.iterator());
        return Optional.empty();
    }

    /**
     * Convert a EnvElement to one with prefix normalization
     *
     * @param event an tag clode event
     * @return same or equivalent event
     */
    private XMLEvent fixEndElementPrefix(EndElement event) {
        QName name = event.getName();
        String uri = name.getNamespaceURI();
        String prefix = defaultPrefix.prefixFor(uri);
        if (!prefix.equals(uri)) {
            event = E.createEndElement(prefix, uri, name.getLocalPart());
        }
        return event;
    }

    /**
     * Rebuild a StartElement to contain namespace declaration of all
     *
     * @param event
     * @return
     */
    private XMLEvent fixStartElement(XMLEvent event) {
        StartElement start = event.asStartElement();
        ArrayList<Namespace> namespaces = new ArrayList<>();
        defaultPrefix.prefixesUsed()
                .forEach((prefix, uri) -> namespaces.add(E.createNamespace(prefix, uri)));
        return E.createStartElement(start.getName(), start.getAttributes(), namespaces.iterator());
    }

}
