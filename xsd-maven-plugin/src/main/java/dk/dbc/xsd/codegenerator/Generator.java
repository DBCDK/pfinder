/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-io-code-generator
 *
 * opensearch-io-code-generator is free software: you can redistribute it and/or modify
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

import dk.dbc.xsd.mapping.Element;
import dk.dbc.xsd.mapping.Schema;
import dk.dbc.xsd.mapping.SimpleType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Generator {

    public static final String XS = "http://www.w3.org/2001/XMLSchema";

    private final File sourceFile;
    private final String packageName;
    private final List<String> bases;
    private final File targetFolder;
    private final String rootClass;
    private final List<String> skipNamespaces;

    private Schema schema;
    private final HashMap<QName, Element> elements;
    private final HashMap<QName, SimpleType> simpleTypes;
    private final HashMap<QName, String> types;
    private final HashMap<String, String> namespaces;
    private Map<String, String> inverseNamespaces;
    private QNameBuilder nameBuilder;

    public Generator(File sourceFile, String packageName, List<String> bases, File targetFolder, String rootClass, List<String> skipNamespaces) {
        this.sourceFile = sourceFile;
        this.packageName = packageName;
        this.bases = bases;
        this.targetFolder = targetFolder;
        this.rootClass = rootClass;
        this.skipNamespaces = skipNamespaces;
        elements = new HashMap<>();
        simpleTypes = new HashMap<>();
        types = new HashMap<>();
        namespaces = new HashMap<>();
    }

    public void run() throws Exception {

        unmarshall();
        this.nameBuilder = new QNameBuilder(namespaces, schema.targetNamespace);
        inverseNamespaces = namespaces.entrySet().stream()
                .filter(e -> !skipNamespaces.contains(e.getValue()))
                .collect(Collectors.toMap(e -> e.getValue(),
                                          e -> e.getKey().toUpperCase()));

        elementsMap();
        simpleTypesMap();

        simpleTypes.values().stream()
                .filter(e -> e.restriction != null)
                .forEach(this::buildSimpleType);
        elements.values().stream()
                .filter(e -> e.type != null)
                .forEach(this::buildSimpleType);
        elementAliasTypes();

        elements.values().stream()
                .filter(el -> {
                    E e = new E(el.complexType);
                    return e.isSequence() &&
                           e.asSequence().size() == 1 &&
                           e.asSequence().get(0).isAny();
                })
                .forEach(e -> types.put(name(e.name), "ANY"));

        Map<QName, String> doc = elements.values().stream()
                .filter(e -> e.annotation != null && e.annotation.documentation != null && !e.annotation.documentation.isEmpty())
                .collect(Collectors.toMap(e -> name(e.name),
                                          e -> e.annotation.documentation.get(0)));

        OutputRoot outputRoot = new OutputRoot(nameBuilder, targetFolder, packageName, rootClass, inverseNamespaces, types, doc, bases);
        outputRoot.build();

        HashSet<QName> seen = new HashSet<>();
        HashSet<QName> wanted = new HashSet<>();
        for (String base : bases) {
            QName qName = name(base);
            seen.add(qName);
            Element element = elements.get(qName);
            Set<QName> referred = buildComplexType(element, doc);
            wanted.addAll(referred);
        }
        wanted.removeAll(seen);
        while (!wanted.isEmpty()) {
            QName name = wanted.iterator().next();
            String simpleType = types.get(name);
            seen.add(name);
            Element element = elements.get(name);
            if (simpleType != null) {
                if (simpleType.startsWith("enum:")) {
                    SimpleType s = simpleTypes.get(name(element.type));
                    EnumBuilder.build(nameBuilder, s, targetFolder, packageName, rootClass, simpleType.substring(5));
                }
            } else {
                Set<QName> referred = buildComplexType(element, doc);
                wanted.addAll(referred);
            }
            wanted.removeAll(seen);
        }
    }

    private void elementAliasTypes() throws AssertionError {
        HashSet<Element> simpleNonXsTypes = new HashSet<>();
        elements.values().stream()
                .filter(e -> e.type != null && !XS.equals(name(e.type).getNamespace()))
                .forEach(simpleNonXsTypes::add);
        while (!simpleNonXsTypes.isEmpty()) {
            int cnt = simpleNonXsTypes.size();
            for (Iterator<Element> iterator = simpleNonXsTypes.iterator() ; iterator.hasNext() ;) {
                Element next = iterator.next();
                QName qName = name(next.type);
                String target = types.get(qName);
                if (target != null) {
                    types.put(name(next.name), target);
                    iterator.remove();
                }
            }
            if (cnt == simpleNonXsTypes.size()) {
                throw new AssertionError("Cannot consumer any of: " + simpleNonXsTypes);
            }
        }
    }

    private void buildSimpleType(Element type) {
        QName name = name(type.name);
        QName typeName = name(type.type);
        if (XS.equals(typeName.getNamespace())) {
            switch (typeName.getName()) {
                case "string":
                case "anyURI":
                    types.put(name, "String");
                    break;
                case "positiveInteger":
                case "integer":
                case "int":
                    types.put(name, "int");
                    break;
                case "decimal":
                    types.put(name, "double");
                    break;
                case "float":
                    types.put(name, "float");
                    break;
                case "date":
                    types.put(name, "special:DATE");
                    break;
                case "boolean":
                    types.put(name, "boolean");
                    break;
                default:
                    System.err.println("type = " + type);
                    throw new AssertionError(name.toString());
            }
        }
    }

    private void buildSimpleType(SimpleType type) {
        QName name = name(type.name);
        if (type.restriction != null) {
            QName base = name(type.restriction.base);
            switch (base.getName()) {
                case "string":
                    if (type.restriction.enumeration != null) {
                        types.put(name, "enum:" + camelcase(name.getName()).replaceAll("Type$", ""));
                    } else {
                        types.put(name, "String");
                    }
                    break;
                default:
                    throw new AssertionError(base);
            }
        }
    }

    private Set<QName> buildComplexType(Element type, Map<QName, String> doc) throws Exception {
        return ClassBuilder.build(nameBuilder, targetFolder, packageName, rootClass, type, inverseNamespaces, types, doc);
    }

    private void simpleTypesMap() {
        for (SimpleType simpleType : schema.simpleType) {
            simpleTypes.put(name(simpleType.name), simpleType);
        }
    }

    private void elementsMap() {
        for (Element element : schema.element) {
            elements.put(name(element.name), element);
        }
    }

    private void unmarshall() throws JAXBException, IOException, XMLStreamException {
        JAXBContext jc = JAXBContext.newInstance(Schema.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        try (FileInputStream is = new FileInputStream(sourceFile)) {
            XMLInputFactory factory = makeXMLInputFactory();
            XMLEventReader reader = factory.createFilteredReader(
                    factory.createXMLEventReader(is),
                    (XMLEvent event) -> {
                if (event.isStartElement()) {
                    Iterator<Namespace> n = event.asStartElement().getNamespaces();
                    while (n.hasNext()) {
                        Namespace namespace = n.next();
                        String prefix = namespace.getPrefix();
                        String uri = namespace.getNamespaceURI();
                        String stored = namespaces.computeIfAbsent(prefix, p -> uri);
                        if (!stored.equals(uri)) {
                            throw new RuntimeException(
                                    new XMLStreamException("Namespace prefix: " + prefix + " redefined",
                                                           event.getLocation()));
                        }
                    }
                }
                return true;
            });
            this.schema = (Schema) unmarshaller.unmarshal(reader);
        }
    }

    private static XMLInputFactory makeXMLInputFactory() {
        synchronized (XMLInputFactory.class) {
            return XMLInputFactory.newInstance();
        }
    }

    private String camelcase(String s) {
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private QName name(String s) {
        return nameBuilder.from(s);
    }
}
