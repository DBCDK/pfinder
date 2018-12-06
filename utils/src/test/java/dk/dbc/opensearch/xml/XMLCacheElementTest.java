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

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;
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
 * @author Morten Bøgeskov <mb@dbc.dk>
 */
public class XMLCacheElementTest {

    public XMLCacheElementTest() {
    }

    private Prefixes prefixes() {
        return new Prefixes();
    }

    private static class Prefixes {

        private final Map<String, String> map = new HashMap<>();

        public Prefixes() {
        }

        Prefixes with(String prefix, String uri) {
            map.put(prefix, uri);
            return this;
        }

        Map<String, String> build() {
            return map;
        }
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
    public void testNamespaces() throws Exception {
        System.out.println("testNamespaces");

        InputStream is = getClass().getClassLoader().getResourceAsStream("xml/input.xml");
        XMLEventReader reader = I.createXMLEventReader(is);
        XMLCacheElement cache = new XMLCacheElement(new DefaultPrefix(prefixes()
                .with("a", "info:1")
                .with("b", "info:2")
                .with("d", "info:4")
                .build()));
        cache.add(reader);
        XMLCacheReader processed = cache.toReader();
        StringWriter w = new StringWriter();
        O.createXMLEventWriter(w).add(processed);
        System.out.println(w);

        assertThat(w.toString(), containsString("xmlns:a=\"info:1\""));
        assertThat(w.toString(), containsString("xmlns:b=\"info:2\""));
        assertThat(w.toString(), containsString("xmlns:ns1=\"info:3\""));
        assertThat(w.toString(), not(containsString("xmlns:d=")));
        assertThat(w.toString(), containsString("<a:bar1"));
        assertThat(w.toString(), containsString(" b:attr2="));
        assertThat(w.toString(), containsString("<ns1:bar2"));


    }

}
