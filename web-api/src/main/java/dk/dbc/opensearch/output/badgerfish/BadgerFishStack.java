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
import java.util.Stack;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Handler for consuming XMLEvents, with support for comsumers to handle nested
 * document parts
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class BadgerFishStack {

    private final Stack<EventConsumer> consumers;

    public BadgerFishStack() {
        this.consumers = new Stack<>();
    }

    /**
     * Put another consumer at the top of the stack
     *
     * @param consumer EventConsumer for the nested/delegated context
     */
    void addConsumer(EventConsumer consumer) {
        consumers.add(consumer);
    }

    /**
     * Send event to consumer on the top of the stack, and cleanup the stack
     * afterwards
     *
     * @param event XMLEvent to delegate
     * @throws XMLStreamException If there's syntax/semantic errors within the
     *                            XML
     * @throws IOException        If there's a problem producing JSON
     */
    void consume(XMLEvent event) throws XMLStreamException, IOException {
        EventConsumer consumer = consumers.peek();
        if (consumer.consume(event)) {
            consumers.pop().close();
            while (!consumers.isEmpty() &&
                   consumers.peek().isCompleted()) {
                consumers.pop().close();
            }
        }
    }

}
