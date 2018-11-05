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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;

import static java.util.Collections.*;

/**
 * Mapping for a tags single entry sub tags
 * <p>
 * This can be a global/singleton object (actually it's preferred)
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class BadgerFishSingle {

    private static final ObjectMapper O = new YAMLMapper();
    private static final Set<String> EMPTY_TAG_SET = EMPTY_SET;
    private static final Map<String, Set<String>> EMPTY_NS_MAP = EMPTY_MAP;

    private final Mapping mapping;
    private final ConcurrentHashMap<String, // ENCLOSING NS URI
            ConcurrentHashMap<String, // ENCLOSING TAG NAME
            ConcurrentSkipListSet<String>>> single; // TAG NAME

    public BadgerFishSingle() {
        this.mapping = new Mapping(EMPTY_SET, EMPTY_MAP);
        this.single = new ConcurrentHashMap<>();
    }

    private BadgerFishSingle(Mapping mapping) {
        this.mapping = mapping;
        this.single = new ConcurrentHashMap<>();
    }

    /**
     * For a given tag (context), are we sure e nested tag name, only will occur
     * once
     *
     * @param enclosing the context, the tag, that contains the tag name
     * @param tag       name of a tag to enquire
     * @return single entry or not
     */
    public boolean isSingle(StartElement enclosing, String tag) {
        QName name = enclosing.getName();
        return single
                .computeIfAbsent(name.getNamespaceURI(), n -> new ConcurrentHashMap<>())
                .computeIfAbsent(name.getLocalPart(), n -> {
                             ConcurrentSkipListSet<String> map =
                                     new ConcurrentSkipListSet<>();
                             if(mapping.all != null)
                                 map.addAll(mapping.all);
                             map.addAll(mapping.ns
                                     .getOrDefault(name.getNamespaceURI(), EMPTY_NS_MAP)
                                     .getOrDefault(name.getLocalPart(), EMPTY_TAG_SET));
                             return map;
                         })
                .contains(tag);
    }

    /**
     * Produce a BadgerFishSingle instance from an YAML input stream
     *
     * @param is YAML context see {@link Mapping}
     * @return BadgerFishSingle instance
     * @throws IOException If it's invalid YAML
     */
    public static BadgerFishSingle from(InputStream is) throws IOException {
        Mapping mapping = O.readValue(is, Mapping.class);
        return new BadgerFishSingle(mapping);
    }

    public static class Mapping {

        public Set<String> all;
        public Map<String, Map<String, Set<String>>> ns;

        public Mapping() {
        }

        public Mapping(Set<String> all, Map<String, Map<String, Set<String>>> ns) {
            this.all = all;
            this.ns = ns;
        }
    }
}
