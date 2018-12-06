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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 *
 * @author Morten Bøgeskov <mb@dbc.dk>
 */
public class XMLCacheReader extends EventReaderDelegate {

    private final XMLEvent[] events;
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
}
