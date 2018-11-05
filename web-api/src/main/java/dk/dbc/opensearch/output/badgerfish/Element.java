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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * XML tag with nested tags to JSON
 * <p>
 * This ensures clustering tags if they're not listed as single occurrence
 * tags.
 * This is isCompleted by consuming a balanced tag, and then replaying these
 * balanced
 * tag, when the enclosing tag is completed, before adding namespace.
 *
 * @author DBC {@literal <dbc.dk>}
 */
class Element extends EventConsumer {

    private final StartElement enclosing;
    private final LinkedList<String> nestedSeenOrder;
    private final HashMap<String, LinkedList<PostponedTag>> nestedPostponed;
    private final HashSet<String> seenSingleTags;

    Element(Context context, StartElement encloseing) throws XMLStreamException, IOException {
        super(context);
        this.enclosing = encloseing;
        this.nestedSeenOrder = new LinkedList<>();
        this.nestedPostponed = new HashMap<>();
        this.seenSingleTags = new HashSet<>();
        out().writeStartObject();
        outputAttributes(encloseing);
    }

    @Override
    boolean consume(XMLEvent event) throws XMLStreamException, IOException {
        if (event.isEndElement()) {
            completeNestedTags();
            ns().outputNamespace(enclosing.getName());
            out().writeEndObject();
            completed = true;
            return true;
        }
        if (event.isStartElement()) {
            processNestedTag(event.asStartElement());
            return false;
        }
        if (event.isCharacters()) {
            skipWhiteSpace(event.asCharacters());
            return false;
        }
        return super.consume(event);
    }

    /**
     * If nested tags are being outputted, ensure JSON is balanced, and output
     * any postponed tags.
     *
     * @throws XMLStreamException If there's syntax/semantic errors within the
     *                            XML
     * @throws IOException        If there's a problem producing JSON
     */
    private void completeNestedTags() throws XMLStreamException, IOException {
        if (!nestedSeenOrder.isEmpty()) {
            out().writeEndArray();
            nestedSeenOrder.poll();
            rerunPostponedTags();
        }
    }

    /**
     *
     * @throws XMLStreamException If there's syntax/semantic errors within the
     *                            XML
     * @throws IOException        If there's a problem producing JSON
     */
    private void rerunPostponedTags() throws IOException, XMLStreamException {
        while (!nestedSeenOrder.isEmpty()) {
            String name = nestedSeenOrder.peek(); // Look at what tag name to replay
            boolean isSingle = single().isSingle(enclosing, name);

            out().writeFieldName(name);
            if (!isSingle)
                out().writeStartArray();
            LinkedList<PostponedTag> tags = nestedPostponed.remove(name);
            if (isSingle && ( seenSingleTags.contains(name) || tags.size() > 1 ))
                throw new IOException("tag: " + name + " in context " + enclosing + " is declared as single entry but tag is repeated");
            for (PostponedTag tag : tags) {
                tag.replayTag();
            }
            if (!isSingle)
                out().writeEndArray();
            nestedSeenOrder.poll(); // Tag(s) replayed, remove name from order list
        }
    }

    /**
     * Handle a StartElement event
     * <p>
     * If nothing is outputting, then
     *
     *
     * @param tag The tag that starts here
     * @throws XMLStreamException If there's syntax/semantic errors within the
     *                            XML
     * @throws IOException        If there's a problem producing JSON
     */
    private void processNestedTag(StartElement tag) throws XMLStreamException, IOException {
        String name = tag.getName().getLocalPart();

        boolean isSingle = false;
        if (nestedSeenOrder.isEmpty()) {
            nestedSeenOrder.add(name);
            isSingle = single().isSingle(enclosing, name);
            if (isSingle) {
                if (seenSingleTags.contains(name))
                    throw new IOException("tag: " + name + " in context " + enclosing + " is declared as single entry but tag is repeated");
                seenSingleTags.add(name);
            }
            out().writeFieldName(name);
            if (!isSingle)
                out().writeStartArray();
        }
        if (nestedSeenOrder.peek().equals(name)) { // This tag is opened for outputting
            stack().addConsumer(new Open(context, tag));
            if (isSingle)
                nestedSeenOrder.poll(); // Only process one
        } else {
            delegateToPostponeTag(tag, name);
        }
    }

    /**
     * Ensure that the text node contains only whitespace
     *
     * @param c the characters event
     * @throws XMLStreamException If there's content in the text
     */
    private void skipWhiteSpace(Characters c) throws XMLStreamException {
        if (c.getData().trim().isEmpty())
            return;
        throw new XMLStreamException("Unexpected text in tag", c.getLocation());
    }

    /**
     * Create and register a postponeTag ElementConsumer
     * <p>
     * Store it under it's name, and add the name to the order list, if it's the
     * first of said name
     *
     * @param event The event that open a nested tag
     * @param name  the name of the tag (taken from event)
     * @throws XMLStreamException If there's syntax/semantic errors within the
     *                            XML
     * @throws IOException        If there's a problem producing JSON
     */
    private void delegateToPostponeTag(StartElement event, String name) throws IOException, XMLStreamException {
        PostponedTag postponed = new PostponedTag(context);
        postponed.consume(event);
        stack().addConsumer(postponed);
        nestedPostponed.computeIfAbsent(name, n -> {
                                    nestedSeenOrder.add(n);
                                    return new LinkedList<>();
                                })
                .add(postponed);
    }

    /**
     * Pull all the attributes from a StartElement, and output them as JSON
     *
     * @param open the StartElement with optional attributes
     * @throws IOException If there's a problem producing JSON
     */
    private void outputAttributes(StartElement open) throws IOException {
        for (Iterator<Attribute> i = open.getAttributes() ; i.hasNext() ;) {
            Attribute attr = i.next();
            out().writeObjectFieldStart("@" + attr.getName().getLocalPart());
            out().writeStringField("$", attr.getValue());
            ns().outputNamespace(attr.getName());
            out().writeEndObject();
        }
    }

}
