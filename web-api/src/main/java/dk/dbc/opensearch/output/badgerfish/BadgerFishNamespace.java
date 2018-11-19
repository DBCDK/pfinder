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

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

/**
 * Namespace cache, that can output the badgerfish top level '@namespace' tag
 *
 * @author DBC {@literal <dbc.dk>}
 */
class BadgerFishNamespace {

    private final JsonGenerator generator;
    private final Map<String, String> defaults;
    private int unresolvedNamespaceNumber;

    private final HashMap<String, String> mappings = new HashMap<>();

    public static final Map DEFAULT_NAMESPACE_MAP = makeDefaultNamespaceMap();

    /**
     * Build a mapper
     *
     * @param generator output stream
     */
    BadgerFishNamespace(JsonGenerator generator) {
        this(generator, DEFAULT_NAMESPACE_MAP);
    }

    /**
     * Build a mapper
     *
     * @param generator output stream
     * @param defaults  default mapping from namespace uri to symbolic name
     */
    BadgerFishNamespace(JsonGenerator generator, Map<String, String> defaults) {
        this.generator = generator;
        this.defaults = defaults;
        this.unresolvedNamespaceNumber = 1;
    }

    /**
     * Output a given name as namespace annotation
     * <p>
     * If the namespace isn't known make a symbolic name for it
     *
     * @param name the XMLEvent name type
     * @throws IOException If JSON couldn't be produced
     */
    void outputNamespace(QName name) throws IOException {
        String uri = name.getNamespaceURI();
        if (uri.isEmpty())
            return;
        String mapping = mappings.computeIfAbsent(uri, s -> {
                                              String ret = defaults.get(s);
                                              if (ret == null)
                                                  ret = "ns" + unresolvedNamespaceNumber++;
                                              return ret;
                                          });
        generator.writeStringField("@", mapping);
    }

    /**
     * Output the namespace mapping for the used namespaces in this JSON
     *
     * @throws IOException If JSON couldn't be produced
     */
    void outputNamespaceMapping() throws IOException {
        if (mappings.isEmpty())
            return;
        generator.writeObjectFieldStart("@namespaces");
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            generator.writeStringField(entry.getValue(), entry.getKey());
        }
        generator.writeEndObject();
    }

    private static Map<String, String> makeDefaultNamespaceMap() {
        Map<String, String> map = new HashMap<>();
        map.put("ac", "http://biblstandard.dk/ac/namespace/");
        map.put("dbcaddi", "http://oss.dbc.dk/rdf/dbcaddi#");
        map.put("dbcbib", "http://oss.dbc.dk/rdf/dbcbib#");
        map.put("dc", "http://purl.org/dc/elements/1.1/");
        map.put("dcmitype", "http://purl.org/dc/dcmitype/");
        map.put("dcterms", "http://purl.org/dc/terms/");
        map.put("dkabm", "http://biblstandard.dk/abm/namespace/dkabm/");
        map.put("dkdcplus", "http://biblstandard.dk/abm/namespace/dkdcplus/");
        map.put("docbook", "http://docbook.org/ns/docbook");
        map.put("kml", "http://www.opengis.net/kml/2.2");
        map.put("marcx", "info:lc/xmlns/marcxchange-v1");
        map.put("mx", "http://www.loc.gov/MARC21/slim");
        map.put("of", "http://oss.dbc.dk/ns/openformat");
        map.put("ofo", "http://oss.dbc.dk/ns/openformatoutput");
        map.put("os", "http://oss.dbc.dk/ns/opensearch");
        map.put("oso", "http://oss.dbc.dk/ns/opensearchobjects");
        map.put("oss", "http://oss.dbc.dk/ns/osstypes");
        map.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        map.put("xs", "http://www.w3.org/2001/XMLSchema");
        map.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        return Collections.unmodifiableMap(map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, // Invert map
                                          Map.Entry::getKey)));
    }

}
