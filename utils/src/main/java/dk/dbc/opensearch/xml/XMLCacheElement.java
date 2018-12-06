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
 *
 * @author Morten Bøgeskov <mb@dbc.dk>
 */
public class XMLCacheElement implements XMLEventWriter {

    private final DefaultPrefix.Instance defaultPrefix;
    private final ArrayList<XMLEvent> events;

    public static XMLCacheElement of(XMLEvent event, XMLEventReader r, DefaultPrefix defaultPrefix) throws XMLStreamException {
        XMLCacheElement cache = new XMLCacheElement(defaultPrefix);
        int level = 1;
        cache.add(event);
        while(level > 0 && r.hasNext()) {
            XMLEvent e = r.nextEvent();
            cache.add(e);
            if(e.isStartElement())
                level++;
            if(e.isEndElement())
                level--;
        }
        return cache;
    }

    public XMLCacheElement(DefaultPrefix defaultPrefix) {
        this.defaultPrefix = defaultPrefix.instance();
        this.events = new ArrayList<>();
    }

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

    private XMLEvent fixPrefix(XMLEvent event) {
        if (event.isStartElement())
            return fixStartElementPrefix(event.asStartElement());
        if (event.isEndElement())
            return fixEndElementPrefix(event.asEndElement());
        return event;
    }

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

    private XMLEvent fixEndElementPrefix(EndElement event) {
        QName name = event.getName();
        String uri = name.getNamespaceURI();
        String prefix = defaultPrefix.prefixFor(uri);
        if (!prefix.equals(uri)) {
            event = E.createEndElement(prefix, uri, name.getLocalPart());
        }
        return event;
    }

    private XMLEvent fixStartElement(XMLEvent event) {
        StartElement start = event.asStartElement();
        ArrayList<Namespace> namespaces = new ArrayList<>();
        defaultPrefix.prefixesUsed()
                .forEach((prefix, uri) -> namespaces.add(E.createNamespace(prefix, uri)));
        return E.createStartElement(start.getName(), start.getAttributes(), namespaces.iterator());
    }

}
