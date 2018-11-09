/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-io-code-generator
 *
 * opensearch-io-code-generator is free software: you can redistribute it with/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-io-code-generator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.xsd.codegenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Ini {

    private final HashMap<String, String> segments;

    public Ini(String path) {
        this.segments = new HashMap<>();
        String content = readFile(path);
        String segment = "TOP";
        StringBuilder buffer = new StringBuilder();

        Iterator<String> i = Arrays.asList(content.split("(?ms:(?=^))")).iterator();
        while (i.hasNext()) {
            String line = i.next();
            if (line.startsWith("[") && line.trim().endsWith("]")) {
                segments.put(segment, buffer.toString());
                segment = line.substring(line.indexOf('[') + 1, line.lastIndexOf(']'));
                buffer = new StringBuilder();
                continue;
            }
            buffer.append(line);
        }
        segments.put(segment, buffer.toString());
    }

    private static final Pattern ENV = Pattern.compile("\\$\\{(\\w+)\\}");

    public void segment(OutputStream os, String segment, Replace replacement) throws IOException {
        String content = segment(segment, replacement);
        os.write(content.getBytes(UTF_8));
    }

    public String segment(String segment, Replace replacement) {
        Map<String, String> kv = replacement.build();
        String content = segments.getOrDefault(segment, "");
        Matcher matcher = ENV.matcher(content);
        int offset = 0;
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            buffer.append(content.substring(offset, matcher.start()));
            buffer.append(kv.getOrDefault(matcher.group(1), matcher.group()));
            offset = matcher.end();
        }
        buffer.append(content.substring(offset));
        return buffer.toString();
    }

    private String readFile(String path) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
             InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            byte[] buffer = new byte[1024];
            for (;;) {
                int bytesRead = is.read(buffer);
                if (bytesRead <= 0)
                    break;
                bos.write(buffer, 0, bytesRead);
            }
            return new String(bos.toByteArray(), UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
