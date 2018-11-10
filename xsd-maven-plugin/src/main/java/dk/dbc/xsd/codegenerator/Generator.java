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
import dk.dbc.xsd.mapping.Schema;
import dk.dbc.xsd.mapping.SimpleType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;
import org.apache.maven.plugin.MojoExecutionException;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Generator {

    public static final String XS = "http://www.w3.org/2001/XMLSchema";

    private final File sourceFile;
    private final List<String> bases;
    private final List<String> skipNamespaces;

    private Schema schema;
    private final Context cxt;

    public Generator(File sourceFile, String packageName, List<String> bases, File targetFolder, String rootClass, List<String> skipNamespaces) {
        this.sourceFile = sourceFile;
        this.bases = bases;
        this.skipNamespaces = skipNamespaces;
        this.cxt = new Context(targetFolder, packageName, rootClass);
    }

    public void run() throws Exception {

        unmarshall();
        cxt.createNameBuilder(schema.targetNamespace);
        cxt.createInverseNamespaces(skipNamespaces);

        elementsMap();
        simpleTypesMap();

        cxt.getSimpleTypes().values().stream()
                .filter(e -> e.restriction != null)
                .forEach(this::buildSimpleType);

        cxt.getElements().values().stream()
                .filter(e -> e.type != null)
                .forEach(this::buildSimpleType);
        elementAliasTypes();
//        System.out.println("cxt.getSimpleTypes() = " + cxt.getSimpleTypes());

        cxt.getElements().values().stream()
                .filter(el -> {
                    E e = new E(el.complexType);
                    return e.isSequence() &&
                           e.asSequence().size() == 1 &&
                           e.asSequence().get(0).isAny();
                })
                .forEach(e -> cxt.getTypes().put(cxt.name(e.name), "ANY"));

        cxt.createDoc();

        OutputRoot outputRoot = new OutputRoot(cxt, bases);
        outputRoot.build();

        HashSet<QName> seen = new HashSet<>();
        HashSet<QName> wanted = new HashSet<>();
        for (String base : bases) {
            QName qName = cxt.name(base);
            seen.add(qName);
            Element element = cxt.getElements().get(qName);
            Set<QName> referred = new ClassBuilder(cxt, element).build();
            wanted.addAll(referred);
        }
        wanted.removeAll(seen);
        while (!wanted.isEmpty()) {
            QName name = wanted.iterator().next();
            String simpleTypeName = cxt.getTypes().get(name);
            seen.add(name);
            Element element = cxt.getElements().get(name);
            if (simpleTypeName != null) {
                if (simpleTypeName.startsWith("enum:")) {
                    simpleTypeName = simpleTypeName.substring(5);
                    SimpleType simpleType = cxt.getSimpleTypes().get(cxt.name(element.type));
                    new EnumBuilder(cxt, simpleType, simpleTypeName).build();
                } else {
                    throw new MojoExecutionException("Cannot generate SimpleType: " + simpleTypeName);
                }
            } else {
                Set<QName> referred = new ClassBuilder(cxt, element).build();
                wanted.addAll(referred);
            }
            wanted.removeAll(seen);
        }
    }

    private void elementsMap() {
        for (Element element : schema.element) {
            cxt.getElements().put(cxt.name(element.name), element);
        }
    }

    private void simpleTypesMap() {
        for (SimpleType simpleType : schema.simpleType) {
            cxt.getSimpleTypes().put(cxt.name(simpleType.name), simpleType);
        }
    }

    private void elementAliasTypes() throws AssertionError {
        HashSet<Element> simpleNonXsTypes = new HashSet<>();
        cxt.getElements().values().stream()
                .filter(e -> e.type != null && !XS.equals(cxt.name(e.type).getNamespace()))
                .forEach(simpleNonXsTypes::add);
        while (!simpleNonXsTypes.isEmpty()) {
            int cnt = simpleNonXsTypes.size();
            for (Iterator<Element> iterator = simpleNonXsTypes.iterator() ; iterator.hasNext() ;) {
                Element next = iterator.next();
                QName qName = cxt.name(next.type);
                String target = cxt.getTypes().get(qName);
                if (target != null) {
                    cxt.getTypes().put(cxt.name(next.name), target);
                    iterator.remove();
                }
            }
            if (cnt == simpleNonXsTypes.size()) {
                throw new AssertionError("Cannot consumer any of: " + simpleNonXsTypes);
            }
        }
    }

    private void buildSimpleType(Element type) {
        QName name = cxt.name(type.name);
        QName typeName = cxt.name(type.type);
        if (XS.equals(typeName.getNamespace())) {
            switch (typeName.getName()) {
                case "string":
                case "anyURI":
                    cxt.getTypes().put(name, "String");
                    break;
                case "positiveInteger":
                case "integer":
                case "int":
                    cxt.getTypes().put(name, "int");
                    break;
                case "decimal":
                    cxt.getTypes().put(name, "double");
                    break;
                case "float":
                    cxt.getTypes().put(name, "float");
                    break;
                case "date":
                    cxt.getTypes().put(name, "special:DATE");
                    break;
                case "boolean":
                    cxt.getTypes().put(name, "boolean");
                    break;
                default:
                    System.err.println("type = " + type);
                    throw new AssertionError(name.toString());
            }
        }
    }

    private void buildSimpleType(SimpleType type) {
        QName name = cxt.name(type.name);
        if (type.restriction != null) {
            QName base = cxt.name(type.restriction.base);
            switch (base.getName()) {
                case "string":
                    if (type.restriction.enumeration != null) {
                        cxt.getTypes().put(name, "enum:" + cxt.camelcase(name.getName()).replaceAll("Type$", ""));
                    } else {
                        cxt.getTypes().put(name, "String");
                    }
                    break;
                default:
                    throw new AssertionError(base);
            }
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
                        String stored = cxt.getNamespaces().computeIfAbsent(prefix, p -> uri);
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

}
