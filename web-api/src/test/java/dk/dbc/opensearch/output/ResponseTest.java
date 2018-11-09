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

import dk.dbc.opensearch.output.badgerfish.BadgerFishSingle;
import dk.dbc.opensearch.output.badgerfish.BadgerFishWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class ResponseTest {

    private static final XMLInputFactory I = makeXMLInputFactory();

    @Test(timeout = 1_000L)
    public void soap() throws Exception {
        System.out.println("soap");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new Root(os).soapEnvelope(this::outputBuilder);

        String string = new String(os.toByteArray(), StandardCharsets.UTF_8);
        System.out.println("string = " + string);
        assertThat(string, containsString("<xs:Envelope xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:Body><"));
        assertThat(string, containsString("more>false</"));
        assertThat(string, containsString("object><myrecord></myrecord><"));
        assertThat(string, containsString("identifier>foo-bar:bug</"));
    }

    @Test(timeout = 1_000L)
    public void xml() throws Exception {
        System.out.println("xml");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new Root(os).xmlEnvelope(this::outputBuilder);

        String string = new String(os.toByteArray(), StandardCharsets.UTF_8);
        System.out.println("string = " + string);
        assertThat(string, not(containsString("<xs:Envelope xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:Body><")));
        assertThat(string, containsString("more>false</"));
        assertThat(string, containsString("object><myrecord></myrecord><"));
        assertThat(string, containsString("identifier>foo-bar:bug</"));
    }

    @Test(timeout = 1_000L)
    public void json() throws Exception {
        System.out.println("json");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BadgerFishWriter w = new BadgerFishWriter(os, new BadgerFishSingle());
        new Root(w).noEnvelope(this::outputBuilder);

        String string = new String(os.toByteArray(), StandardCharsets.UTF_8);
        System.out.println("string = " + string);
        assertThat(string, not(containsString("Envelope")));
        assertThat(string, containsString("\"more\":[{\"$\":\"false\""));
        assertThat(string, containsString("\"myrecord\":[{\"$\":\""));
        assertThat(string, containsString("\"identifier\":[{\"$\":\"foo-bar:bug\""));
    }

    private void outputBuilder(Root.EntryPoint e) throws XMLStreamException, IOException {
        e.searchResponse(searchResponse -> searchResponse
                .result(result -> result
                        .hitCount(10)
                        .collectionCount(-1)
                        .more(false)
                        .searchResult(searchResult -> searchResult
                                .collection(collection -> collection
                                        .resultPosition(1)
                                        .numberOfObjects(1)
                                        .object(object -> object
                                                ._any(record())
                                                .identifier("foo-bar:bug")
                                                .creationDate(new Date())
                                        )
                                )
                        )
                )
        );
    }

    private XMLEventReader record() throws XMLStreamException {
        ByteArrayInputStream is = new ByteArrayInputStream(
                "<myrecord></myrecord>".getBytes(StandardCharsets.UTF_8));
        return I.createXMLEventReader(is);
    }

    private static XMLInputFactory makeXMLInputFactory() {
        synchronized (XMLInputFactory.class) {
            return XMLInputFactory.newInstance();
        }
    }

}
