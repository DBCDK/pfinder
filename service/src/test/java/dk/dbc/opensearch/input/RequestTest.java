/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-service
 *
 * opensearch-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.input;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Comment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@RunWith(Parameterized.class)
public class RequestTest {

    private static final XMLInputFactory I = makeXMLInputFactory();

    private final String name;
    private final String fileName;

    public RequestTest(String name, String fileName) {
        this.name = name;
        this.fileName = fileName;
    }

    @Test(timeout = 2_000L)
    public void testCase() throws Exception {
        System.out.println(name);
        String actual = "";
        try {
            Request request = new Request(new FileInputStream(fileName));
            if (request.isSearchRequest())
                actual = request.asSearchRequest().toString();
        } catch (Exception ex) {
            actual = ex.getMessage();
        }
        System.out.println(actual);
        XMLEventReader reader = I.createFilteredReader(
                I.createXMLEventReader(new FileInputStream(fileName)),
                e -> e.getEventType() == COMMENT);
        while (reader.hasNext()) {
            Comment comment = (Comment) reader.nextEvent();
            String text = comment.getText().trim();
            if (text.startsWith("!")) {
                assertThat(actual, not(containsString(text.substring(1))));
            } else {
                assertThat(actual, containsString(text));
            }
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> tests() throws Exception {
        File base = new File(RequestTest.class.getClassLoader().getResource("input").toURI());
        Path basePath = base.toPath();
        return Arrays.stream(base.listFiles())
                .filter(f -> f.getName().endsWith(".xml"))
                .sorted()
                .map(f -> testFromFile(f, basePath))
                .collect(Collectors.toList());
    }

    private static Object[] testFromFile(File file, Path base) {
        String fileName = file.getName();
        String testName = fileName.substring(0, fileName.length() - 4);
        return test(testName, base.resolve(fileName).toString());
    }

    private static Object[] test(Object... objs) {
        return objs;
    }

    private static XMLInputFactory makeXMLInputFactory() {
        synchronized (XMLInputFactory.class) {
            return XMLInputFactory.newInstance();
        }
    }

}
