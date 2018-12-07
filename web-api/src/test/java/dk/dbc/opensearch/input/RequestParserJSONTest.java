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

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
public class RequestParserJSONTest {

    private final String name;
    private final Path path;

    public RequestParserJSONTest(String name, Path path) {
        this.name = name;
        this.path = path;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> tests() throws Exception {
        ClassLoader classLoader = RequestParserJSONTest.class.getClassLoader();
        return Arrays.asList("input", "json").stream()
                .flatMap((String folder) -> {
                    URL resource = classLoader.getResource(folder);
                    File dir = new File(resource.getPath());
                    Path path = dir.toPath();
                    return Arrays.stream(dir.list((file, fileName) -> fileName.endsWith(".json")))
                            .map(s -> test(s.substring(0, s.length() - 5), path));
                })
                .collect(Collectors.toList());
    }

    public static Object[] test(Object... objs) {
        return objs;
    }

    @Test(timeout = 2_000L)
    public void testCase() throws Exception {
        System.out.println(name);
        try (FileInputStream json = new FileInputStream(path.resolve(name + ".json").toFile()) ;
             FileInputStream xml = new FileInputStream(path.resolve(name + ".xml").toFile())) {
            String jsonReq;
            try {
                jsonReq = new RequestParserJSON(json).asBaseRequest().toString();
                jsonReq = jsonReq.replaceFirst("trackingId=[^,]*", "trackingId=***");
            } catch (XMLStreamException ex) {
                jsonReq = ex.getMessage();
                jsonReq = jsonReq.substring(jsonReq.indexOf('\n') + 1); // Strip error location
            }
            String xmlReq;
            try {
                xmlReq = new RequestParserXML(xml).asBaseRequest().toString();
                xmlReq = xmlReq.replaceFirst("trackingId=[^,]*", "trackingId=***");
            } catch (XMLStreamException ex) {
                xmlReq = ex.getMessage();
                xmlReq = xmlReq.substring(xmlReq.indexOf('\n') + 1); // Strip error location
            }
            System.out.println("jsonReq = " + jsonReq);
            System.out.println("xmlReq  = " + xmlReq);
            assertThat(jsonReq, is(xmlReq));
        }

    }
}
