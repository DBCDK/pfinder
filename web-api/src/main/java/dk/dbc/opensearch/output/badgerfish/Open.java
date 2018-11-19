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

import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Handler of an ElementStart.
 * <p>
 * It's primary purpose is to look at the next content, and determine if it's a
 * TEXT node or it has nested elements, then delegate to {@link Element}.
 *
 * @author DBC {@literal <dbc.dk>}
 */
class Open extends EventConsumer {

    private final StartElement encloseing;
    private String characters;

    Open(Context context, StartElement enclosing) {
        super(context);
        this.encloseing = enclosing;
        this.characters = "";
    }

    @Override
    public boolean consume(XMLEvent event) throws XMLStreamException, IOException {
        if (event.isEndElement()) {
            writeText();
            return true;
        }
        if (event.isCharacters()) {
            Characters c = event.asCharacters();
            characters += c.getData();
            return false;
        }
        if (event.isStartElement()) {
            deletateToElement(event);
            return false;
        }
        throw new XMLStreamException("Unexpected input", event.getLocation());
    }

    private void deletateToElement(XMLEvent event) throws XMLStreamException, IOException {
        if (!characters.trim().isEmpty())
            throw new XMLStreamException("Didn't expect text before tag", event.getLocation());
        stack().addConsumer(new Element(context, encloseing));
        stack().consume(event);
        completed = true;
    }

    private void writeText() throws IOException {
        out().writeStartObject();
        out().writeStringField("$", characters);
        ns().outputNamespace(encloseing.getName());
        out().writeEndObject();
    }

}
