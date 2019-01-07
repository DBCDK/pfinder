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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static dk.dbc.opensearch.xml.XMLEventFactories.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class XMLCacheReaderTest {

    public XMLCacheReaderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test(timeout = 2_000L)
    public void testCase() throws Exception {
        System.out.println("testCase");

        DefaultPrefix defaultPrefix = new DefaultPrefix(map()
                .with("foo", "info:v1")
                .with("bar", "info:v2")
                .build());

        Reader r = new StringReader("<xml><b:f1 xmlns=\"info:v1\" xmlns:b=\"info:v2\"><f2>abc</f2></b:f1></xml>");
        XMLEventReader xr = I.createXMLEventReader(r);
        XMLCacheReader reader = XMLCacheElement.of((StartElement) xr.nextTag(), xr, defaultPrefix).toReader();

        String before = xmlToString(reader);
        System.out.println("before = " + before);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(reader);
        oos.close();
        bos.close();

        byte[] bytes = bos.toByteArray();
        System.out.println("bytes.length = " + bytes.length);

        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        ois.close();
        bis.close();

        reader = (XMLCacheReader) ois.readObject();
        String after = xmlToString(reader);
        System.out.println("after = " + after);

        assertThat(after, is(before));
    }

    private String xmlToString(XMLCacheReader reader) throws XMLStreamException {
        StringWriter w = new StringWriter();
        XMLEventWriter ow = O.createXMLEventWriter(w);
        ow.add(reader);
        ow.close();
        return w.toString();
    }

    public MapBuilder<String, String> map() {
        return new MapBuilder<>();
    }

    private static class MapBuilder<K, V> {

        private final HashMap<K, V> map;

        public MapBuilder() {
            this.map = new HashMap<>();
        }

        public MapBuilder<K, V> with(K k, V v) {
            map.put(k, v);
            return this;
        }

        public HashMap<K, V> build() {
            return map;
        }

    }

}
