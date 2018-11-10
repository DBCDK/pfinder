/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-xsd-maven-plugin
 *
 * opensearch-xsd-maven-plugin is free software: you can redistribute it with/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-xsd-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.xsd.codegenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class OutputRoot {

    private static final Ini ROOT_INI = new Ini("root.ini");

    private final Context cxt;
    private final List<String> bases;
    protected final Replace replace;

    private final Set<QName> tags;

    OutputRoot(Context cxt, List<String> bases) {
        this.cxt = cxt;
        this.bases = bases;
        this.replace = cxt.replacer()
                .with("class", cxt.getRootClass());
        this.tags = new LinkedHashSet<>();
    }

    public void build() throws IOException {
        try (JavaFileOutputStream os = new JavaFileOutputStream(cxt, cxt.getRootClass())) {
            ROOT_INI.segment(os, "TOP", replace);
            outputEntryPoints(os);
            outputNamespaces(os);
            outputTags(os, tags);
            ROOT_INI.segment(os, "BOTTOM", replace);
        }
    }

    private void outputNamespaces(final JavaFileOutputStream os) {
        cxt.getInverseNamespaces().entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    try {
                        replace.with("uri", entry.getKey())
                                .with("name", entry.getValue())
                                .with("prefix", entry.getValue().toLowerCase(Locale.ROOT));
                        ROOT_INI.segment(os, "NAMESPACE", replace);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }

    private void outputEntryPoints(JavaFileOutputStream os) throws IOException {
        ROOT_INI.segment(os, "ENTRYPOINTS_START", replace);
        for (String base : bases) {
            QName name = cxt.name(base);
            tags.add(name);

            replace.with("method", name.getName())
                    .with("type", cxt.camelcase(name))
                    .with("method_upper", cxt.constName(name));
            String documentation = cxt.getDoc(name);

            if (documentation != null) {
                replace.with("doc", documentation);
                ROOT_INI.segment(os, "ENTRYPOINT_COMMENT", replace);
            }
            ROOT_INI.segment(os, "ENTRYPOINT", replace);
        }
        ROOT_INI.segment(os, "ENTRYPOINTS_END", replace);
    }

    public void outputTags(OutputStream os, Set<QName> tags) throws IOException {
        String allNamespaces = cxt.getInverseNamespaces().values().stream()
                .sorted()
                .map(s -> "NS_" + s)
                .collect(Collectors.joining(", "));
        ROOT_INI.segment(os, "TAGS_START", replace);
        for (QName tag : tags) {
            replace.with("tagname", tag.getName())
                    .with("tagname_upper", cxt.constName(tag))
                    .with("prefix", cxt.prefix(tag))
                    .with("extra_ns", allNamespaces);
            ROOT_INI.segment(os, "TAG", replace);
        }
        ROOT_INI.segment(os, "TAGS_END", replace);
    }

}
