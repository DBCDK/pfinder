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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

import static dk.dbc.opensearch.xml.XMLElementFilter.elementReader;
import static dk.dbc.opensearch.xml.XMLEventFactories.*;

/**
 * A {@link XMLEventReader} implementation for an array or events
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class XMLCacheReader extends EventReaderDelegate implements Serializable {

    private static final long serialVersionUID = -5468666206156869952L;

    private XMLEvent[] events;
    private int pos;

    public XMLCacheReader(XMLEvent[] events) {
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

    private void writeObject(ObjectOutputStream s) throws IOException {
        try {
            s.writeInt(events.length);
            byte[] xml = toBytes(events);
            s.writeInt(xml.length);
            s.write(xml);
        } catch (XMLStreamException ex) {
            throw new IOException("Cannot serialize XMLEvents", ex);
        }
    }

    private void readObject(ObjectInputStream s) throws IOException {
        try {
            events = new XMLEvent[s.readInt()];
            byte[] xml = new byte[s.readInt()];
            s.readFully(xml);
            fromBytes(xml, events);
            pos = 0;
        } catch (XMLStreamException ex) {
            throw new IOException("Cannot serialize XMLEvents", ex);
        }
    }

    private static byte[] toBytes(XMLEvent[] events) throws XMLStreamException, IOException {
        try (ByteArrayOutputStream w = new ByteArrayOutputStream()) {
            XMLEventWriter wr = O.createXMLEventWriter(w);
            for (XMLEvent event : events) {
                wr.add(event);
            }
            wr.close();
            return w.toByteArray();
        }
    }

    private static void fromBytes(byte[] xml, XMLEvent[] events) throws XMLStreamException, IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(xml)) {
            XMLEventReader r = elementReader(I.createXMLEventReader(bis));
            for (int i = 0 ; i < events.length ; i++) {
                events[i] = r.nextEvent();
            }
        }
    }

}
