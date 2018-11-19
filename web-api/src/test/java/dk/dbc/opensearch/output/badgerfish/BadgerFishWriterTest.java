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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@RunWith(Parameterized.class)
public class BadgerFishWriterTest {

    private static final ObjectMapper O = makeObjectMapper();
    private static final XMLInputFactory I = makeXMLInputFactory();

    private static ObjectMapper makeObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    private static XMLInputFactory makeXMLInputFactory() {
        synchronized (XMLInputFactory.class) {
            return XMLInputFactory.newInstance();
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> tests() throws Exception {
        URL base = BadgerFishWriterTest.class.getClassLoader().getResource("badgerfish");
        return Arrays.stream(
                new File(base.toURI()).listFiles())
                .filter(f -> f.getName().endsWith(".json"))
                .sorted()
                .map(f -> test(f))
                .collect(Collectors.toList());
    }

    public static Object[] test(File f) {
        String s = f.getName();
        Path path = f.getAbsoluteFile().toPath().getParent();
        return test(s.substring(0, s.length() - 5), path);
    }

    public static Object[] test(Object... o) {
        return o;
    }

    private final String name;
    private final Path path;

    public BadgerFishWriterTest(String name, Path path) {
        this.name = name;
        this.path = path;
    }

    @Test(timeout = 2_000L)
    public void testCase() throws Exception {
        System.out.println(name);
        System.out.println(path);
        File json = path.resolve(name + ".json").toFile();
        File xml = path.resolve(name + ".xml").toFile();
        File yaml = path.resolve(name + ".yaml").toFile();

        BadgerFishSingle repeated = new BadgerFishSingle();
        if (yaml.exists()) {
            try (FileInputStream fisYaml = new FileInputStream(yaml)) {
                repeated = BadgerFishSingle.from(fisYaml);
            }
        }

        try (FileInputStream fisXml = new FileInputStream(xml) ;
             FileInputStream fisJson = new FileInputStream(json)) {
            String expected = O.writeValueAsString(O.readTree(fisJson));
            String actual;

            XMLEventReader reader = I.createXMLEventReader(fisXml);
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                BadgerFishWriter writer = new BadgerFishWriter(bos, repeated);
                writer.add(reader);
                writer.close();
                System.out.println(bos);
                ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                actual = O.writeValueAsString(O.readTree(bis));
            } catch (IOException | XMLStreamException ex) {
                System.out.println(ex);
                actual = O.writeValueAsString(O.createObjectNode().put("exception", ex.getMessage()));
                throw ex;
            }
            assertThat(actual, is(expected));
        }
    }
}
