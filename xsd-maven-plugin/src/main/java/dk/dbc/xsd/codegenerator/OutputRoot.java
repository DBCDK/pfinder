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

import java.io.File;
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

    protected final String packageName;
    protected final Map<String, String> inverseNamespaces;
    protected final Map<QName, String> types;
    protected final Map<QName, String> doc;

    protected final String className;
    protected final Replace replace;

    private final QNameBuilder qNameBuilder;
    private final File targetFolder;
    private final List<String> bases;

    private final Set<QName> tags = new LinkedHashSet<>();

    public OutputRoot(QNameBuilder qNameBuilder, File targetFolder, String packageName, String rootClass, Map<String, String> inverseNamespaces, Map<QName, String> types, Map<QName, String> doc, List<String> bases) {
        this.packageName = packageName;
        this.inverseNamespaces = inverseNamespaces;
        this.types = types;
        this.doc = doc;
        this.className = rootClass;
        this.replace = Replace.of("class", className)
                .with("root", rootClass)
                .with("package", packageName)
                .with("indent", "");
        this.qNameBuilder = qNameBuilder;
        this.targetFolder = targetFolder;
        this.bases = bases;
    }

    public void build() throws IOException {
        try (JavaFileOutputStream os = new JavaFileOutputStream(targetFolder, packageName, className)) {
            ROOT_INI.segment(os, "TOP", replace);
            outputEntryPoints(os);
            outputNamespaces(os);
            outputTags(os, tags);
            ROOT_INI.segment(os, "BOTTOM", replace);
        }
    }

    private void outputNamespaces(final JavaFileOutputStream os) {
        inverseNamespaces.entrySet().stream()
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
            QName name = qNameBuilder.from(base);
            tags.add(name);

            replace.with("method", name.getName())
                    .with("type", camelcase(name.getName()))
                    .with("method_upper", constName(name));
            String documentation = doc.get(name);

            if (documentation != null) {
                replace.with("doc", documentation);
                ROOT_INI.segment(os, "ENTRYPOINT_COMMENT", replace);
            }
            ROOT_INI.segment(os, "ENTRYPOINT", replace);
        }
        ROOT_INI.segment(os, "ENTRYPOINTS_END", replace);
    }

    public void outputTags(OutputStream os, Set<QName> tags) throws IOException {
        String allNamespaces = inverseNamespaces.values().stream()
                .sorted()
                .map(s -> "NS_" + s)
                .collect(Collectors.joining(", "));
        ROOT_INI.segment(os, "TAGS_START", replace);
        for (QName tag : tags) {
            replace.with("tagname", tag.getName())
                    .with("tagname_upper", constName(tag))
                    .with("prefix", inverseNamespaces.getOrDefault(tag.getNamespace(), "XXX"))
                    .with("extra_ns", allNamespaces);
            ROOT_INI.segment(os, "TAG", replace);
        }
        ROOT_INI.segment(os, "TAGS_END", replace);
    }

    private String prefix(QName ref) {
        return inverseNamespaces.getOrDefault(ref.getNamespace(), "");
    }

    private String camelcase(String s) {
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private String constcase(String s) {
        return s.replaceAll("(?<=[a-z])(?=[A-Z])", "_")
                .replaceAll("_+", "_")
                .toUpperCase(Locale.ROOT);
    }

    private String constName(QName ref) {
        String prefix = prefix(ref);
        if (!prefix.isEmpty())
            prefix = prefix.toUpperCase(Locale.ROOT) + "_";
        return prefix + constcase(ref.getName());
    }

}
