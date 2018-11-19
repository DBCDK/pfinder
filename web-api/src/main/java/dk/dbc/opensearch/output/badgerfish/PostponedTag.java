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
import java.util.ArrayList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Consumed a balanced tag, for later replaying
 * <p>
 When a nested tag shows up, but the previous nested tag isn't declared as
 single, we cannot be sure that there's no more tags that should be clustered
 into the previous group.
 Therefore we need to collect a subdocument, that can be replayed, when the
 tags of same name as the previous nested tag, is guaranteed to be completed.
 *
 * @author DBC {@literal <dbc.dk>}
 */
class PostponedTag extends EventConsumer {

    private final ArrayList<XMLEvent> events;
    private int level;

    PostponedTag(Context context) {
        super(context);
        this.events = new ArrayList<>();
        level = 0;
    }

    /**
     * Reconsume all the events that has been gathered fot this nested
     * subdocument
     *
     * @throws XMLStreamException XML exception from underlying stack
     * @throws IOException        If the subdocument cannot be converted to JSON
     */
    void replayTag() throws XMLStreamException, IOException {
        for (XMLEvent event : events) {
            stack().consume(event);
        }
    }

    @Override
    boolean consume(XMLEvent event) throws XMLStreamException, IOException {
        events.add(event);
        if (event.isEndElement())
            return --level == 0;
        if (event.isStartElement())
            level++;
        return false;
    }

    // This is never hit, since return false from consume, removes this instance
    @Override
    boolean isCompleted() {
        return level == 0;
    }

}
