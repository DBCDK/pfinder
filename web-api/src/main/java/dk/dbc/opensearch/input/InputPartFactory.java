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
package dk.dbc.opensearch.input;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 *
 * @param <T> Type (POJO) of input XML element
 */
public class InputPartFactory<T extends InputPart> {

    @FunctionalInterface
    public interface PropertySetter<T> {

        /**
         * Interface for method that given a value (usually String) and where
         * that value is taken from stores it.
         *
         * @param content  that value extracted from XML
         * @param location the opening tag location
         * @throws XMLStreamException If value somehow is invalid
         */
        void set(T content, Location location) throws XMLStreamException;
    }

    @FunctionalInterface
    private interface ValueFromXML<T> {

        T get(StartElement open, XMLEventReader reader) throws XMLStreamException;
    }

    @FunctionalInterface
    private interface ElementAction<T extends InputPart> {

        /**
         * The action of actually setting a value on an object
         *
         * @param obj    the target object
         * @param open   the element just read form the stream
         * @param reader the rest of the stream
         * @throws XMLStreamException if the value showhow cannot be extracted
         */
        void set(T obj, StartElement open, XMLEventReader reader) throws XMLStreamException;
    }

    private static final String OS_NS_URI = "http://oss.dbc.dk/ns/opensearch";

    private final Supplier<T> objectCreator;
    private final Map<String, ElementAction<T>> actions = new HashMap<>();

    public InputPartFactory(Supplier<T> objectCreator) {
        this.objectCreator = objectCreator;
    }

    public T from(StartElement open, XMLEventReader reader) throws XMLStreamException {
        String tagName = open.getName().getLocalPart();
        Location startLocation = open.getLocation();
        T obj = objectCreator.get();
        for (;;) {
            XMLEvent event = reader.nextTag();
            Location location = event.getLocation();
            if (isClose(event, RequestParser.OS_URI, tagName))
                break;
            if (!event.isStartElement())
                throw new XMLStreamException("Expected " + tagName + " close or " + tagName + " parameter", location);
            QName qname = event.asStartElement().getName();
            if (!OS_NS_URI.equals(qname.getNamespaceURI()))
                throw new XMLStreamException("Expected " + tagName + " close or " + tagName + " parameter", location);
            String name = qname.getLocalPart();
            ElementAction<T> action = actions.get(name);
            if (action == null)
                throw new XMLStreamException("Unknown " + tagName + " property " + name, location);
            action.set(obj, event.asStartElement(), reader);
        }
        obj.validate(startLocation);
        return obj;
    }

    private static boolean isClose(XMLEvent event, String namespace, String elementName) {
        if (event.isEndElement()) {
            QName qname = event.asEndElement().getName();
            if (namespace.equals(qname.getNamespaceURI()) && elementName.equals(qname.getLocalPart()))
                return true;
        }
        return false;
    }

    private static String readTextAndClose(StartElement open, XMLEventReader reader) throws XMLStreamException {
        StringBuilder buffer = new StringBuilder();
        XMLEvent event;
        for (;;) {
            event = reader.nextEvent();
            if (!event.isCharacters())
                break;
            buffer.append(event.asCharacters().getData());
        }
        if (event.isEndElement() && event.asEndElement().getName().equals(open.getName()))
            return buffer.toString();
        throw new XMLStreamException("Expected " + open.getName().getLocalPart() + " close", event.getLocation());
    }

    /**
     * Add an element consumer (simple value)
     *
     * @param name   name of the element
     * @param action a method that given an object returns a setter for said
     *               element value
     * @return this
     */
    public InputPartFactory<T> with(String name, Function<T, PropertySetter<String>> action) {
        return putAction(name, InputPartFactory::readTextAndClose, action);
    }

    /**
     * Add an element consumer (nested scope)
     *
     * @param <C>     Type of the nested element
     * @param name    name of the element
     * @param factory something that given StartElement and Reader provides the
     *                value as type C
     * @param action  a method that given an object returns a setter for said
     *                element value
     * @return this
     */
    public <C extends InputPart> InputPartFactory<T> with(String name, InputPartFactory<C> factory, Function<T, PropertySetter<C>> action) {
        return putAction(name, factory::from, action);
    }

    private <C> InputPartFactory<T> putAction(String name, ValueFromXML<C> valueSupplier, Function<T, PropertySetter<C>> action) {
        ElementAction<T> setterAction = (T obj, StartElement open, XMLEventReader reader) ->
                action.apply(obj).set(valueSupplier.get(open, reader), open.getLocation());
        ElementAction<T> oldValue = actions.put(name, setterAction);
        if (oldValue != null)
            throw new IllegalStateException("Element " + name + " is already defined");
        return this;
    }

}
