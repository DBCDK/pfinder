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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Singleton
public class IndexHtmlBean {

    private static final Pattern COMMENT = Pattern.compile("\\s*[#;].*");
    private static final XMLEventFactory E = makeXMLEventFactory();
    private static final XMLOutputFactory O = makeXMLOutputFactory();

    public static final IndexHtmlBean INDEX_HTML = new IndexHtmlBean();

    private byte[] bytes;

    public IndexHtmlBean() {
    }

    @PostConstruct
    public void init() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Iterator<String> index = Arrays.asList(readFile("example_client.html").split("\\s*@OPTIONS@\\s*")).iterator();
            if (index.hasNext()) {
                bos.write(index.next().getBytes(UTF_8));
                while (index.hasNext()) {
                    buildOptions(bos);
                    bos.write(index.next().getBytes(UTF_8));
                }
            }
            this.bytes = bos.toByteArray();
        } catch (IOException | XMLStreamException ex) {
            throw new EJBException("Error creating index.html", ex);
        }
    }

    public byte[] getBytes() {
        return bytes;
    }

    private void buildOptions(ByteArrayOutputStream bos) throws IOException, XMLStreamException {
        Iterator<String> ini = Arrays.asList(readFile("example_requests.ini").split("(?ms:(?=^))")).iterator();

        StringBuilder sb = new StringBuilder();
        String title = nextTitle(ini, sb);
        while (title != null) {
            sb = new StringBuilder();
            String nextTitle = nextTitle(ini, sb);
            XMLEventWriter writer = O.createXMLEventWriter(bos);
            writer.add(E.createStartElement("", "", "option"));
            String content = sb.toString().trim() + "\n";
            writer.add(E.createAttribute("value", content));
            writer.add(E.createCharacters(title));
            writer.add(E.createEndElement("", "", "option"));
            writer.close();
            title = nextTitle;
        }
    }

    private String nextTitle(Iterator<String> ini, StringBuilder sb) {
        while (ini.hasNext()) {
            String line = ini.next();
            if (COMMENT.matcher(line).matches())
                continue;
            if (line.startsWith("[") && line.trim().endsWith("]"))
                return line.substring(line.indexOf('[') + 1, line.lastIndexOf(']'));
            sb.append(line);
        }
        return null;
    }

    private String readFile(String path) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
             InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            byte[] buffer = new byte[4096];
            for (int readBytes = in.read(buffer) ; readBytes > 0 ; readBytes = in.read(buffer)) {
                bos.write(buffer, 0, readBytes);
            }
            return new String(bos.toByteArray(), UTF_8);
        }
    }

    private static XMLEventFactory makeXMLEventFactory() {
        synchronized (XMLEventFactory.class) {
            return XMLEventFactory.newInstance();
        }
    }

    private static XMLOutputFactory makeXMLOutputFactory() {
        synchronized (XMLOutputFactory.class) {
            return XMLOutputFactory.newInstance();
        }
    }

}
