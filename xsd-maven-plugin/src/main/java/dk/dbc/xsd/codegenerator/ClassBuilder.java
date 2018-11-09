/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-io-code-generator
 *
 * opensearch-io-code-generator is free software: you can redistribute it with/or modify
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
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class ClassBuilder {

    private static final Ini CLASS_INI = new Ini("class.ini");

    public static Set<QName> build(QNameBuilder qNameBuilder, File targetFolder, String packageName, String rootClass, Element element, Map<String, String> inverseNamespaces, Map<QName, String> types, Map<QName, String> doc) throws Exception {
        return new ClassBuilder(qNameBuilder, targetFolder, packageName, rootClass, element, inverseNamespaces, types, doc)
                .build();
    }

    protected final String packageName;
    protected final Map<String, String> inverseNamespaces;
    protected final Map<QName, String> types;
    protected final Map<QName, String> doc;
    private final QNameBuilder qNameBuilder;
    private final File targetFolder;
    private final Element element;

    protected final String className;
    protected final Replace replace;
    private final Set<QName> referredNames;

    public ClassBuilder(QNameBuilder qNameBuilder, File targetFolder, String packageName, String rootClass, Element element, Map<String, String> inverseNamespaces, Map<QName, String> types, Map<QName, String> doc) {
        this.packageName = packageName;
        this.inverseNamespaces = inverseNamespaces;
        this.types = types;
        this.doc = doc;
        this.qNameBuilder = qNameBuilder;
        this.targetFolder = targetFolder;
        this.element = element;
        this.className = camelcase(qNameBuilder.from(element.name).getName());
        this.replace = Replace.of("class", className)
                .with("root", rootClass)
                .with("package", packageName)
                .with("indent", "");

        this.referredNames = new HashSet<>();
    }

    private Set<QName> build() throws Exception {
        System.out.println("Building: " + className);
        E e = new E(element.complexType);
        Map<QName, String> returnValue = new LinkedHashMap<>();
        Map<String, Set<QName>> methodsInStage = new LinkedHashMap<>();
        Set<String> stages = new LinkedHashSet<>();
        Set<QName> tags = new LinkedHashSet<>();
        stages.add(className);
        methodsInStage.put(className, new LinkedHashSet<>());
        buildTree(e, className, returnValue, methodsInStage, stages, tags);

        if (false) {
            System.out.println("returnValue = ");
            returnValue.entrySet().forEach(System.out::println);
            System.out.println("methodsInStage = ");
            methodsInStage.entrySet().forEach(System.out::println);
            System.out.println("tags = ");
            tags.forEach(System.out::println);
        }

        try (JavaFileOutputStream os = new JavaFileOutputStream(targetFolder, packageName, className)) {
            output(os, returnValue, methodsInStage, tags);
        }
        return referredNames;
    }

    private void buildTree(E complex, String currentStage, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage, Set<String> stages, Set<QName> tags) {
        if (complex.isSequence()) {
            buildTreeSequence(complex, currentStage, returnValue, methodsInStage, stages, tags);
        } else if (complex.isChoice()) {
            buildTreeChoice(complex, true, returnValue, methodsInStage, stages, tags);
        }
    }

    private void buildTreeSequence(E complex, String currentStage, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage, Set<String> stages, Set<QName> tags) throws IllegalStateException {
        List<E> seq = complex.asSequence();
        for (Iterator<E> i = seq.iterator() ; i.hasNext() ;) {
            E e = i.next();
            if (e.isSequence()) {
                throw new IllegalStateException("xs:sequence nested in xs:sequence is unsupported");
            } else if (e.isChoice()) {
                buildTreeChoice(e, !i.hasNext(), returnValue, methodsInStage, stages, tags);
            } else {
                QName ref;
                if (e.isElement()) {
                    ref = name(e.asElement().ref);
                    tags.add(ref);
                } else if (e.isAny()) {
                    ref = name("_any");
                } else {
                    throw new IllegalStateException("Don't know: " + e);
                }
                for (String stage : stages) {
                    methodsInStage.computeIfAbsent(stage, s -> new LinkedHashSet<>())
                            .add(ref);
                }
                if (i.hasNext()) {
                    currentStage = "Stage." + camelcase(ref.getName());
                } else {
                    currentStage = null;
                }
                if (e.isRepeatable() && currentStage != null) {
                    methodsInStage.computeIfAbsent(currentStage, s -> new LinkedHashSet<>())
                            .add(ref);
                }
                returnValue.put(ref, currentStage);
                if (!e.isOptional())
                    stages.clear();
                stages.add(currentStage);
            }
        }
    }

    private void buildTreeChoice(E complex, boolean lastElement, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage, Set<String> stages, Set<QName> tags) throws IllegalStateException {
        List<E> choice = complex.asChoice();
        boolean required = true;
        Set<String> exitStages = new LinkedHashSet<>();
        for (Iterator<E> i = choice.iterator() ; i.hasNext() ;) {
            E e = i.next();
            if (e.isSequence())
                throw new IllegalStateException("xs:sequence nested in xs:choice is unsupported");
            if (e.isChoice())
                throw new IllegalStateException("xs:choice nested in xs:choice is unsupported");
            if (e.isOptional())
                required = false;
            QName ref;
            if (e.isElement()) {
                ref = name(e.asElement().ref);
                tags.add(ref);
            } else if (e.isAny()) {
                ref = name("_any");
            } else {
                throw new IllegalStateException("Don't know: " + e);
            }
            for (String stage : stages) {
                methodsInStage.computeIfAbsent(stage, s -> new LinkedHashSet<>())
                        .add(ref);
            }
            if (e.isRepeatable()) {
                String stage = "Stage._Choice" + camelcase(ref.getName());
                exitStages.add(stage);
                returnValue.put(ref, stage);
                methodsInStage.computeIfAbsent(stage, s -> new LinkedHashSet<>())
                        .add(ref);
            } else if (lastElement) {
                returnValue.put(ref, null);
            } else {
                String stage = "Stage._ChoiceExit";
                exitStages.add(stage);
                returnValue.put(ref, stage);
            }
        }
        if (required)
            stages.clear();
        stages.addAll(exitStages);
    }

    private void output(OutputStream os, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage, Set<QName> tags) throws IOException {
        CLASS_INI.segment(os, "TOP", replace);
        outputRootClass(os, returnValue, methodsInStage);
        CLASS_INI.segment(os, "SCOPE_CLASSES_START", replace);
        outputStageClass(os, returnValue, methodsInStage);
        CLASS_INI.segment(os, "SCOPE_CLASSES_END", replace);
        outputTags(os, tags);
        CLASS_INI.segment(os, "BOTTOM", replace);
    }

    private void outputRootClass(OutputStream os, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage) throws IOException {
        replace.with("indent", "");
        for (QName method : methodsInStage.get(className)) {
            outputMethod(os, className, method, returnValue.get(method));
        }
    }

    private void outputStageClass(OutputStream os, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage) throws IOException {
        replace.with("indent", "        ");
        for (String stage : methodsInStage.keySet()) {
            if (stage.equals(className))
                continue;
            replace.with("scope", stage.substring(stage.indexOf('.') + 1));
            CLASS_INI.segment(os, "SCOPE_CLASS_START", replace);
            for (QName method : methodsInStage.get(stage)) {
                outputMethod(os, stage, method, returnValue.get(method));
            }
            CLASS_INI.segment(os, "SCOPE_CLASS_END", replace);
        }
    }

    public void outputMethod(OutputStream os, String scope, QName method, String returnScope) throws IOException {
        boolean isVoid = returnScope == null;
        if (returnScope == null)
            returnScope = "void";

        replace.with("return", returnScope)
                .with("method", method.getName())
                .with("method_upper", constName(method));
        String documentation = doc.get(method);

        String type = types.get(method);
        if (type == null) {
            if (method.getName().equals("_any")) {
                outputJavaDoc(os, "METHOD_COMMENT_ANY", documentation, isVoid);
                if (!isVoid)
                    CLASS_INI.segment(os, "METHOD_RETURNS", replace);
                CLASS_INI.segment(os, "METHOD_ANY_NO_SCOPE", replace);
            } else {
                replace.with("type", camelcase(method.getName()));
                outputJavaDoc(os, "METHOD_COMMENT_SCOPE", documentation, isVoid);
                if (!isVoid)
                    CLASS_INI.segment(os, "METHOD_RETURNS", replace);
                CLASS_INI.segment(os, "METHOD_SCOPE", replace);
                referredNames.add(method);
            }
        } else if (type.equals("String")) {
            outputJavaDoc(os, "METHOD_COMMENT", documentation, isVoid);
            if (!isVoid)
                CLASS_INI.segment(os, "METHOD_RETURNS", replace);
            CLASS_INI.segment(os, "METHOD_STRING", replace);
        } else if (type.equals("ANY")) {
            outputJavaDoc(os, "METHOD_COMMENT_ANY", documentation, isVoid);
            if (!isVoid)
                CLASS_INI.segment(os, "METHOD_RETURNS", replace);
            CLASS_INI.segment(os, "METHOD_ANY", replace);
        } else if (type.startsWith("special:")) {
            outputJavaDoc(os, "METHOD_COMMENT", documentation, isVoid);
            String special = type.substring(8);
            if (!isVoid)
                CLASS_INI.segment(os, "METHOD_RETURNS", replace);
            CLASS_INI.segment(os, "METHOD_SPECIAL_" + special, replace);
            referredNames.add(method);
        } else if (type.startsWith("enum:")) {
            replace.with("type", type.substring(5));
            outputJavaDoc(os, "METHOD_COMMENT", documentation, isVoid);
            if (!isVoid)
                CLASS_INI.segment(os, "METHOD_RETURNS", replace);
            CLASS_INI.segment(os, "METHOD_SIMPLE", replace);
            referredNames.add(method);
        } else {
            replace.with("type", type);
            outputJavaDoc(os, "METHOD_COMMENT", documentation, isVoid);
            if (!isVoid)
                CLASS_INI.segment(os, "METHOD_RETURNS", replace);
            CLASS_INI.segment(os, "METHOD_SIMPLE", replace);
        }
        if (!isVoid) {
            if (returnScope.equals(scope)) {
                CLASS_INI.segment(os, "METHOD_RETURN_THIS", replace);
            } else {
                CLASS_INI.segment(os, "METHOD_RETURN", replace);
            }
        }
        CLASS_INI.segment(os, "METHOD_END", replace);
    }

    private void outputJavaDoc(OutputStream os, String segment, String documentation, boolean noreturn) throws IOException {
        if (documentation != null) {
            replace.with("doc", documentation);
            CLASS_INI.segment(os, segment + ( noreturn ? "_NORETURN" : "" ), replace);
        }
    }

    public void outputTags(OutputStream os, Set<QName> tags) throws IOException {
        CLASS_INI.segment(os, "TAGS_START", replace);
        for (QName tag : tags) {
            replace.with("tagname", tag.getName())
                    .with("tagname_upper", constName(tag))
                    .with("prefix", inverseNamespaces.getOrDefault(tag.getNamespace(), "XXX"));
            CLASS_INI.segment(os, "TAG", replace);
        }
        CLASS_INI.segment(os, "TAGS_END", replace);
    }

    private QName name(String name) {
        return qNameBuilder.from(name);
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
