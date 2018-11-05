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

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
class EventConsumer {

    protected final Context context;
    protected boolean completed;

    EventConsumer(Context context) {
        this.context = context;
        this.completed = false;
    }

    protected final JsonGenerator out() {
        return context.out;
    }

    protected final BadgerFishNamespace ns() {
        return context.ns;
    }

    protected final BadgerFishStack stack() {
        return context.stack;
    }

    protected final BadgerFishSingle single() {
        return context.single;
    }

    /**
     * Consume (process) a XMLEvent,
     *
     * @param event
     * @return if this EventConsumer should be removed from the stack
     * @throws XMLStreamException If there's syntax/semantic errors within the
     *                            XML
     * @throws IOException        If there's a problem producing JSON
     */
    boolean consume(XMLEvent event) throws XMLStreamException, IOException {
        throw new XMLStreamException("Unexpected input event: " + event, event.getLocation());
    }

    /**
     * Is this EventConsumer no longer able to consume events
     *
     * @return if the consumer can be pruned
     */
    boolean isCompleted() {
        return completed;
    }

    /**
     * Called when the consumer is pruned
     *
     * @throws XMLStreamException If there's syntax/semantic errors within the
     *                            XML
     * @throws IOException        If there's a problem producing JSON
     */
    void close() throws XMLStreamException, IOException {
    }
}
