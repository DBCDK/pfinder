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
import org.apache.maven.plugin.logging.Log;

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

    public Generator(Log log, File sourceFile, String packageName, List<String> bases, File targetFolder, String rootClass, List<String> skipNamespaces) {
        this.sourceFile = sourceFile;
        this.bases = bases;
        this.skipNamespaces = skipNamespaces;
        this.cxt = new Context(log, targetFolder, packageName, rootClass);
    }

    public void run() throws Exception {

        unmarshall();
        cxt.createNameBuilder(schema.targetNamespace);

        registerAllSimpleTypesInContext();
        registerAllElementsInContext();

        registerSimpleTypesAsGenericTypes();
        registerElementWithXSTypesAsGenericTypes();
        registerElementsThatAreAliasesAsGenericTypes();
        registerElementsThatAreOnlyAnyAsGenericType();

        new OutputRoot(cxt, bases).build();

        HashSet<QName> seen = new HashSet<>();
        HashSet<QName> wanted = new HashSet<>();

        for (String base : bases) {
            QName name = cxt.name(base);
            seen.add(name);
            Element element = cxt.getElement(name);
            Set<QName> referred = new ClassBuilder(cxt, element).build();
            wanted.addAll(referred);
        }

        wanted.removeAll(seen);
        while (!wanted.isEmpty()) {
            QName name = wanted.iterator().next();
            String typeName = cxt.getType(name);
            seen.add(name);
            Element element = cxt.getElement(name);
            if (typeName != null) {
                if (typeName.startsWith("enum:")) {
                    typeName = typeName.substring(5);
                    SimpleType simpleType = cxt.getSimpleType(cxt.name(element.type));
                    new EnumBuilder(cxt, simpleType, typeName).build();
                } else {
                    throw new MojoExecutionException("Cannot generate SimpleType: " + typeName);
                }
            } else {
                Set<QName> referred = new ClassBuilder(cxt, element).build();
                wanted.addAll(referred);
            }
            wanted.removeAll(seen);
        }
    }

    private void unmarshall() throws JAXBException, IOException, XMLStreamException {
        JAXBContext jc = JAXBContext.newInstance(Schema.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        try (FileInputStream is = new FileInputStream(sourceFile)) {
            XMLEventReader reader = namespaceRecordingEventReader(is);
            this.schema = (Schema) unmarshaller.unmarshal(reader);
        }
    }

    private XMLEventReader namespaceRecordingEventReader(FileInputStream is) throws XMLStreamException {
        XMLInputFactory factory = makeXMLInputFactory();
        return factory.createFilteredReader(
                factory.createXMLEventReader(is),
                (XMLEvent event) -> {
            if (event.isStartElement()) {
                Iterator<Namespace> n = event.asStartElement().getNamespaces();
                while (n.hasNext()) {
                    Namespace namespace = n.next();
                    cxt.storeNamespace(namespace.getPrefix(), namespace.getNamespaceURI(), event);
                }
            }
            return true;
        });
    }

    private void registerAllSimpleTypesInContext() {
        schema.simpleType.forEach(cxt::addSimpleType);
    }

    private void registerAllElementsInContext() {
        schema.element.forEach(cxt::addElement);
    }

    private void registerSimpleTypesAsGenericTypes() {
        cxt.allSimpleTypes()
                .filter(e -> e.restriction != null)
                .forEach((SimpleType type) -> {
                    QName name = cxt.name(type.name);
                    if (type.restriction != null) {
                        QName base = cxt.name(type.restriction.base);
                        switch (base.getName()) {
                            case "string":
                                if (type.restriction.enumeration != null) {
                                    cxt.addType(name, "enum:" + cxt.camelcase(name).replaceAll("Type$", ""));
                                } else {
                                    cxt.addType(name, "String");
                                }
                                break;
                            default:
                                throw new AssertionError(base);
                        }
                    }
                });
    }

    private void registerElementWithXSTypesAsGenericTypes() {
        cxt.allElements()
                .filter(e -> e.type != null)
                .forEach((Element type) -> {
                    QName name = cxt.name(type.name);
                    QName typeName = cxt.name(type.type);
                    if (XS.equals(typeName.getNamespace())) {
                        switch (typeName.getName()) {
                            case "string":
                            case "anyURI":
                                cxt.addType(name, "String");
                                break;
                            case "positiveInteger":
                            case "integer":
                            case "int":
                                cxt.addType(name, "int");
                                break;
                            case "decimal":
                                cxt.addType(name, "double");
                                break;
                            case "float":
                                cxt.addType(name, "float");
                                break;
                            case "date":
                                cxt.addType(name, "special:DATE");
                                break;
                            case "boolean":
                                cxt.addType(name, "boolean");
                                break;
                            default:
                                System.err.println("type = " + type);
                                throw new AssertionError(name.toString());
                        }
                    }
                });
    }

    private void registerElementsThatAreAliasesAsGenericTypes() throws AssertionError {
        HashSet<Element> simpleNonXsTypes = new HashSet<>();
        cxt.allElements()
                .filter(e -> e.type != null && !XS.equals(cxt.name(e.type).getNamespace()))
                .forEach(simpleNonXsTypes::add);
        while (!simpleNonXsTypes.isEmpty()) {
            int cnt = simpleNonXsTypes.size();
            for (Iterator<Element> iterator = simpleNonXsTypes.iterator() ; iterator.hasNext() ;) {
                Element next = iterator.next();
                QName qName = cxt.name(next.type);
                String target = cxt.getType(qName);
                if (target != null) {
                    cxt.addType(cxt.name(next.name), target);
                    iterator.remove();
                }
            }
            if (cnt == simpleNonXsTypes.size()) {
                throw new AssertionError("Cannot convert any of: " + simpleNonXsTypes);
            }
        }
    }

    private void registerElementsThatAreOnlyAnyAsGenericType() {
        cxt.allElements()
                .filter(el -> {
                    E e = new E(el.complexType);
                    return e.isSequence() &&
                           e.asSequence().size() == 1 &&
                           e.asSequence().get(0).isAny();
                })
                .forEach(e -> cxt.addType(cxt.name(e.name), "ANY"));
    }

    private static XMLInputFactory makeXMLInputFactory() {
        synchronized (XMLInputFactory.class) {
            return XMLInputFactory.newInstance();
        }
    }

}
