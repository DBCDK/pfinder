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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Context {

    private final Log log;
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

    public Context(Log log, File targetFolder, String packageName, String rootClass) {
        this.log = log;
        this.targetFolder = targetFolder;
        this.packageName = packageName;
        this.rootClass = rootClass;
        elements = new HashMap<>();
        simpleTypes = new HashMap<>();
        types = new HashMap<>();
        namespaces = new HashMap<>();
        inverseNamespaces = new HashMap<>();
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

    public void addElement(Element element) {
        QName name = name(element.name);
        elements.put(name, element);
        if (element.annotation != null &&
            element.annotation.documentation != null &&
            !element.annotation.documentation.isEmpty()) {
            doc.put(name, element.annotation.documentation.get(0));
        }
    }

    public Element getElement(QName name) {
        return elements.get(name);
    }

    public Stream<Element> allElements() {
        return elements.values().stream();
    }

    public void addSimpleType(SimpleType simpleType) {
        QName name = name(simpleType.name);
        simpleTypes.put(name, simpleType);
    }

    public SimpleType getSimpleType(QName name) {
        return simpleTypes.get(name);
    }

    public Stream<SimpleType> allSimpleTypes() {
        return simpleTypes.values().stream();
    }

    public String getType(QName name) {
        return types.get(name);
    }

    public void addType(QName name, String type) {
        types.put(name, type);
    }

    /**
     * Places the namespace in a map, and returns the first namespace this
     * prefix has been given
     *
     * @param prefix wanted prefix
     * @param uri
     */
    public void storeNamespace(String prefix, String uri, XMLEvent event) {
        String old = namespaces.put(prefix, uri);
        if (old != null) {
            throw new RuntimeException(
                    new XMLStreamException("Namespace prefix: " + prefix + " redefined",
                                           event.getLocation()));
        }
        inverseNamespaces.put(uri, prefix);
    }

    public Map<String, String> getInverseNamespaces() {
        return inverseNamespaces;
    }

    public String getDoc(QName name) {
        return doc.getOrDefault(name, "");
    }

    public void addDoc(QName name, String text) {
        doc.put(name, text);
    }

    public Replace replacer() {
        return Replace.of("root", rootClass)
                .with("package", packageName)
                .with("indent", "");
    }

    public void createNameBuilder(String targetNamespace) {
        this.nameBuilder = new QNameBuilder(namespaces, targetNamespace);
    }

    public QName name(String s) {
        return nameBuilder.from(s);
    }

    public String camelcase(QName name) {
        String s = name.getName();
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

    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    public void debug(CharSequence cs) {
        log.debug(cs);
    }

    public void debug(CharSequence cs, Throwable thrwbl) {
        log.debug(cs, thrwbl);
    }

    public void debug(Throwable thrwbl) {
        log.debug(thrwbl);
    }

    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    public void info(CharSequence cs) {
        log.info(cs);
    }

    public void info(CharSequence cs, Throwable thrwbl) {
        log.info(cs, thrwbl);
    }

    public void info(Throwable thrwbl) {
        log.info(thrwbl);
    }

    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    public void warn(CharSequence cs) {
        log.warn(cs);
    }

    public void warn(CharSequence cs, Throwable thrwbl) {
        log.warn(cs, thrwbl);
    }

    public void warn(Throwable thrwbl) {
        log.warn(thrwbl);
    }

    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    public void error(CharSequence cs) {
        log.error(cs);
    }

    public void error(CharSequence cs, Throwable thrwbl) {
        log.error(cs, thrwbl);
    }

    public void error(Throwable thrwbl) {
        log.error(thrwbl);
    }
}
