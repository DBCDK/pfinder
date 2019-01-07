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
package dk.dbc.opensearch.output;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class ResourceStreamingOutput {

    private static final Logger log = LoggerFactory.getLogger(ResourceStreamingOutput.class);

    private static final ClassLoader CLASS_LOADER = ResourceStreamingOutput.class.getClassLoader();
    private static final HashMap<String, String> MIME_TYPES = makeMimetypes();

    public static Response docroot(String path) {
        InputStream is = CLASS_LOADER.getResourceAsStream("docroot/" + path);
        if (is == null) {
            log.info("GET /{} 404 - Not Found", path);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String extension = path.substring(path.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        log.trace("extension = {}", extension);
        log.info("GET /{}", path);
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(600);
        return Response.ok(is)
                .type(MIME_TYPES.getOrDefault(extension, MediaType.TEXT_PLAIN))
                .cacheControl(cacheControl)
                .build();
    }

    private static HashMap<String, String> makeMimetypes() {
        HashMap<String, String> map = new HashMap<>();
        map.put("html", MediaType.TEXT_HTML + "; charset=utf-8");
        map.put("js", "application/javascript; charset=utf-8");
        map.put("json", MediaType.APPLICATION_JSON);
        map.put("xml", MediaType.APPLICATION_XML);
        map.put("wsdl", MediaType.APPLICATION_XML);
        map.put("xsd", MediaType.APPLICATION_XML);
        map.put("css", "application/css; charset=utf-8");
        return map;
    }

}
