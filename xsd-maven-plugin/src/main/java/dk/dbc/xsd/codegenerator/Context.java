/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-xsd-maven-plugin
 *
 * opensearch-xsd-maven-plugin is free software: you can redistribute it and/or modify
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

import dk.dbc.xsd.mapping.Element;
import dk.dbc.xsd.mapping.SimpleType;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Context {

    private final File targetFolder;
    private final String packageName;
    private final String rootClass;

    private final HashMap<QName, Element> elements;
    private final HashMap<QName, SimpleType> simpleTypes;
    private final HashMap<QName, String> types;
    private final HashMap<String, String> namespaces;
    private Map<String, String> inverseNamespaces;
    private Map<QName, String> doc;
    private QNameBuilder nameBuilder;

    public Context(File targetFolder, String packageName, String rootClass) {
        this.targetFolder = targetFolder;
        this.packageName = packageName;
        this.rootClass = rootClass;
        elements = new HashMap<>();
        simpleTypes = new HashMap<>();
        types = new HashMap<>();
        namespaces = new HashMap<>();
        doc = new HashMap<>();
    }

    public File getTargetFolder() {
        return targetFolder;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getRootClass() {
        return rootClass;
    }

    public HashMap<QName, Element> getElements() {
        return elements;
    }

    public HashMap<QName, SimpleType> getSimpleTypes() {
        return simpleTypes;
    }

    public HashMap<QName, String> getTypes() {
        return types;
    }

    public HashMap<String, String> getNamespaces() {
        return namespaces;
    }

    public Map<String, String> getInverseNamespaces() {
        return inverseNamespaces;
    }

    public Map<QName, String> getDoc() {
        return doc;
    }

    public QNameBuilder getNameBuilder() {
        return nameBuilder;
    }

    public Replace replacer() {
        return Replace.of("root", rootClass)
                .with("package", packageName)
                .with("indent", "");
    }

    public void createNameBuilder(String targetNamespace) {
        this.nameBuilder = new QNameBuilder(namespaces, targetNamespace);
    }

    public void createInverseNamespaces(List<String> skipNamespaces) {
        this.inverseNamespaces = inverseNamespaces = namespaces.entrySet().stream()
                .filter(e -> !skipNamespaces.contains(e.getValue()))
                .collect(Collectors.toMap(e -> e.getValue(),
                                          e -> e.getKey().toUpperCase()));
    }

    public void createDoc() {
        this.doc = elements.values().stream()
                .filter(e -> e.annotation != null && e.annotation.documentation != null && !e.annotation.documentation.isEmpty())
                .collect(Collectors.toMap(e -> name(e.name),
                                          e -> e.annotation.documentation.get(0)));

    }

    public QName name(String s) {
        return nameBuilder.from(s);
    }

    public String camelcase(String s) {
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    public String constcase(String s) {
        return s.replaceAll("(?<=[a-z])(?=[A-Z])", "_")
                .replaceAll("_+", "_")
                .toUpperCase(Locale.ROOT);
    }

    public String prefix(QName ref) {
        String prefix = inverseNamespaces.get(ref.getNamespace());
        return prefix;
    }

    public String constName(QName ref) {
        String prefix = prefix(ref);
        if (prefix == null)
            throw new IllegalArgumentException("Cannot get symbolic namespace prefix for: " + ref);
        return prefix.toUpperCase(Locale.ROOT) + "_" + constcase(ref.getName());
    }

}
