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
package dk.dbc.opensearch.output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import org.junit.Test;

import static javax.xml.stream.XMLStreamConstants.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class ResponseTest {

    private static final XMLInputFactory I = makeXMLInputFactory();

    private static final int EVENT_FILTER = ( 1 << PROCESSING_INSTRUCTION ) | ( 1 << COMMENT ) | ( 1 << SPACE ) |
                                            ( 1 << START_DOCUMENT ) | ( 1 << END_DOCUMENT ) |
                                            ( 1 << ENTITY_REFERENCE ) | ( 1 << ATTRIBUTE ) |
                                            ( 1 << DTD ) | ( 1 << NAMESPACE ) | ( 1 << NOTATION_DECLARATION ) |
                                            ( 1 << ENTITY_DECLARATION );

    @Test(timeout = 2_000L)
    public void testOutput() throws Exception {
        System.out.println("testOutput");
        ByteArrayInputStream is = new ByteArrayInputStream(
                "<myrecord></myrecord>".getBytes(StandardCharsets.UTF_8));
        XMLEventReader reader = I.createXMLEventReader(is);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        EventOutput x = new EventOutput(os);
        x.soapEnvelopeSearchResponse(() -> {
            x.result(() -> {
                x.resultPosition(1);
                x.agency(100);
                x.callback("my_cb");
                x.hitCount(10);
                x.formattedCollection(() -> {
                    x.stream(I.createFilteredReader(
                            reader,
                            e -> ( EVENT_FILTER & ( 1 << e.getEventType() ) ) == 0
                    ));
                });
                x.identifier("foo-bar:bug");
            });

        });

        String string = new String(os.toByteArray(), StandardCharsets.UTF_8);
        System.out.println("string = " + string);

        assertThat(string, containsString("formattedCollection><myrecord></myrecord></"));
        assertThat(string, containsString("identifier>foo-bar:bug</"));
        assertThat(string, containsString("agency>000100</"));
    }

    private static XMLInputFactory makeXMLInputFactory() {
        synchronized (XMLInputFactory.class) {
            return XMLInputFactory.newInstance();
        }
    }

}
