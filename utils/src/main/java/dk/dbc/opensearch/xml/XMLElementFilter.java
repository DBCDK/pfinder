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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import static dk.dbc.opensearch.xml.XMLEventFactories.I;
import static javax.xml.stream.XMLStreamConstants.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class XMLElementFilter {

    private static final int UNWANTED_EVENTS =
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
     * <p>
     * Only events allowed Start-/EndElement and Characters
     *
     * @param event event to test if is wanted
     * @return if the bit is set
     */
    public static boolean isWanted(XMLEvent event) {
        return ( UNWANTED_EVENTS & maskOf(event.getEventType()) ) == 0;
    }

    /**
     * Make an XML event stream that contains only Start/EndElement and
     * Character events
     *
     * @param reader input
     * @return filtered reader
     * @throws XMLStreamException if there's a problem constructing the reader
     */
    public static XMLEventReader elementReader(XMLEventReader reader) throws XMLStreamException {
        return I.createFilteredReader(reader, XMLElementFilter::isWanted);
    }
}
