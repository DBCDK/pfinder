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
 * The root object in JSON equivalent of the root tag of XML and namespace
 * specifications
 *
 * @author DBC {@literal <dbc.dk>}
 */
class Root extends EventConsumer {

    private boolean complete = false;

    Root(Context context) {
        super(context);
    }

    @Override
    public boolean consume(XMLEvent event) throws XMLStreamException, IOException {
        if (event.isStartElement()) {
            delegateToOpen(event);
            return false; // Dont remove - we've added a consumer
        }
        if (event.isCharacters()) {
            return ignoreWhiteSpace(event);
        }
        return super.consume(event);
    }

    private void delegateToOpen(XMLEvent event) throws IOException {
        out().writeStartObject();
        StartElement element = event.asStartElement();
        out().writeFieldName(element.getName().getLocalPart());
        stack().addConsumer(new Open(context, element));
        complete = true;
    }

    private boolean ignoreWhiteSpace(XMLEvent event) throws XMLStreamException {
        Characters c = event.asCharacters();
        if (c.isIgnorableWhiteSpace())
            return false;
        throw new XMLStreamException("Didn't expect text outside root tag", event.getLocation());
    }

    @Override
    public boolean isCompleted() {
        return complete;
    }

    @Override
    public void close() throws XMLStreamException, IOException {
        ns().outputNamespaceMapping();
        out().writeEndObject();
    }
}
