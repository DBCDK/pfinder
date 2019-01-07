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

import dk.dbc.xsd.mapping.Element;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.EMPTY_SET;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class ClassBuilder {

    private static final Ini CLASS_INI = new Ini("class.ini");
    private final Context cxt;

    private final Element element;

    protected final String className;
    protected final Replace replace;
    private final Set<QName> referredNames;
    private final Set<QName> optionalMethods;

    public ClassBuilder(Context cxt, Element element) {
        this.element = element;
        this.cxt = cxt;
        this.className = cxt.camelcase(cxt.name(element.name));
        this.replace = cxt.replacer()
                .with("class", className);
        this.referredNames = new HashSet<>();
        this.optionalMethods = new HashSet<>();
    }

    public Set<QName> build() throws IOException {
        cxt.info("Building: " + className);
        E e = new E(element.complexType);
        Map<QName, String> returnValue = new LinkedHashMap<>();
        Map<String, Set<QName>> methodsInStage = new LinkedHashMap<>();
        Set<String> stages = new LinkedHashSet<>();
        Set<QName> tags = new LinkedHashSet<>();
        stages.add(className);
        methodsInStage.put(className, new LinkedHashSet<>());
        Set<QName> terminalFunctions = buildTree(e, className, returnValue, methodsInStage, stages, tags);

        cxt.debug("returnValue = ");
        returnValue.entrySet().forEach(d -> cxt.debug(d.toString()));
        cxt.debug("methodsInStage = ");
        methodsInStage.entrySet().forEach(d -> cxt.debug(d.toString()));
        cxt.debug("tags = ");
        tags.forEach(d -> cxt.debug(d.toString()));
        cxt.debug("terminalFunctions = ");
        terminalFunctions.forEach(d -> cxt.debug(d.toString()));

        try (JavaFileOutputStream os = new JavaFileOutputStream(cxt, className)) {
            output(os, returnValue, methodsInStage, tags, terminalFunctions);
        }

        return referredNames;
    }

    private Set<QName> buildTree(E complex, String currentStage, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage, Set<String> stages, Set<QName> tags) {
        if (complex.isSequence()) {
            return buildTreeSequence(complex, currentStage, returnValue, methodsInStage, stages, tags);
        } else if (complex.isChoice()) {
            return buildTreeChoice(complex, true, returnValue, methodsInStage, stages, tags);
        }
        return EMPTY_SET;
    }

    private Set<QName> buildTreeSequence(E complex, String currentStage, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage, Set<String> stages, Set<QName> tags) throws IllegalStateException {
        Set<QName> terminalFunctions = new LinkedHashSet<>();
        List<E> seq = complex.asSequence();
        boolean firstStage = true;
        for (Iterator<E> i = seq.iterator() ; i.hasNext() ; firstStage = false) {
            E e = i.next();
            if (e.isSequence()) {
                throw new IllegalStateException("xs:sequence nested in xs:sequence is unsupported");
            } else if (e.isChoice()) {
                terminalFunctions.addAll(buildTreeChoice(e, !i.hasNext(), returnValue, methodsInStage, stages, tags));
            } else {
                QName method = nameOfE(e, tags);
                for (String stage : stages) {
                    methodsInStage.computeIfAbsent(stage, s -> new LinkedHashSet<>())
                            .add(method);
                }
                if (e.isRepeatable()) {
                    if (!firstStage)
                        currentStage = "Stage." + cxt.camelcase(method);
                } else if (i.hasNext()) {
                    currentStage = "Stage." + cxt.camelcase(method);
                } else {
                    currentStage = null;
                }
                if (e.isRepeatable() && currentStage != null) {
                    methodsInStage.computeIfAbsent(currentStage, s -> new LinkedHashSet<>())
                            .add(method);
                }
                returnValue.put(method, currentStage);
                if (e.isRepeatable()) {
                    QName ref = method;
                    QName ref1 = ref.withNewName(ref.getName() + "_repeated");
                    returnValue.put(ref1, currentStage);
                    methodsInStage.computeIfAbsent(ref1.getName(), s -> new LinkedHashSet<>())
                            .add(ref);
                    stages.forEach(s -> methodsInStage.get(s).add(ref1));

                }

                if (e.isOptional()) {
                    optionalMethods.add(method);
                } else {
                    stages.clear();
                    terminalFunctions.clear();
                }
                stages.add(currentStage);
                terminalFunctions.add(method);
            }
        }
        return terminalFunctions;
    }

    private Set<QName> buildTreeChoice(E complex, boolean lastElement, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage, Set<String> stages, Set<QName> tags) throws IllegalStateException {
        Set<QName> terminalFunctions = new LinkedHashSet<>();
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
            QName ref = nameOfE(e, tags);
            for (String stage : stages) {
                methodsInStage.computeIfAbsent(stage, s -> new LinkedHashSet<>())
                        .add(ref);
            }
            if (e.isRepeatable()) {
                String stage = "Stage._Choice" + cxt.camelcase(ref);
                exitStages.add(stage);
                returnValue.put(ref, stage);
                methodsInStage.computeIfAbsent(stage, s -> new LinkedHashSet<>())
                        .add(ref);
                QName ref1 = ref.withNewName(ref.getName() + "_repeated");
                returnValue.put(ref1, stage);
                methodsInStage.computeIfAbsent(ref1.getName(), s -> new LinkedHashSet<>())
                        .add(ref);
                stages.forEach(s -> methodsInStage.get(s).add(ref1));
            } else if (lastElement) {
                returnValue.put(ref, null);
            } else {
                String stage = "Stage._ChoiceExit";
                exitStages.add(stage);
                returnValue.put(ref, stage);
            }
            terminalFunctions.add(ref);
        }
        if (required)
            stages.clear();
        stages.addAll(exitStages);
        return terminalFunctions;
    }

    private QName nameOfE(E e, Set<QName> tags) throws IllegalStateException {
        if (e.isElement()) {
            QName ref = cxt.name(e.asElement().ref);
            tags.add(ref);
            return ref;
        } else if (e.isAny()) {
            return cxt.name("_any");
        } else {
            throw new IllegalStateException("Don't know: " + e);
        }
    }

    private void output(OutputStream os, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage, Set<QName> tags, Set<QName> terminalFunctions) throws IOException {
        CLASS_INI.segment(os, "TOP", replace);
        outputRootClass(os, returnValue, methodsInStage, terminalFunctions);
        CLASS_INI.segment(os, "ROOT_CLASS_END", replace);
        if (methodsInStage.get(className).size() > 1 ||
            !terminalFunctions.contains(methodsInStage.get(className).iterator().next()))
            CLASS_INI.segment(os, "ROOT_CLASS_END_NOT_VOID", replace);
        if (methodsInStage.size() > 1) {
            CLASS_INI.segment(os, "SCOPE_CLASSES_START", replace);
            outputStageClass(os, returnValue, methodsInStage, terminalFunctions);
            CLASS_INI.segment(os, "SCOPE_CLASSES_END", replace);
        }
        outputTags(os, tags);
        CLASS_INI.segment(os, "BOTTOM", replace);
    }

    private void outputRootClass(OutputStream os, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage, Set<QName> terminalFunctions) throws IOException {
        replace.with("indent", "");
        for (QName method : methodsInStage.get(className)) {
            outputMethod(os, className, method, returnValue.get(method), terminalFunctions.contains(method));
        }
    }

    private void outputStageClass(OutputStream os, Map<QName, String> returnValue, Map<String, Set<QName>> methodsInStage, Set<QName> terminalFunctions) throws IOException {
        replace.with("indent", "        ");
        for (String stage : methodsInStage.keySet()) {
            if (stage.equals(className))
                continue;
            replace.with("scope", stage.substring(stage.indexOf('.') + 1));
            CLASS_INI.segment(os, "SCOPE_CLASS_START", replace);
            for (QName method : methodsInStage.get(stage)) {
                outputMethod(os, stage, method, returnValue.get(method),
                             terminalFunctions.contains(method) || stage.endsWith("_repeated"));
            }
            if (!stage.endsWith("_repeated")) {
                CLASS_INI.segment(os, "SCOPE_CLASS_DELEGATE", replace);
                if (methodsInStage.get(stage).size() > 1 ||
                    !terminalFunctions.contains(methodsInStage.get(stage).iterator().next()))
                    CLASS_INI.segment(os, "SCOPE_CLASS_DELEGATE_NOT_VOID", replace);
            }
            CLASS_INI.segment(os, "SCOPE_CLASS_END", replace);
        }
    }

    public void outputMethod(OutputStream os, String scope, QName method, String returnScope, boolean isTerminal) throws IOException {
        boolean isVoid = returnScope == null;
        if (returnScope == null)
            returnScope = "void";
        boolean isSkip = !isVoid && optionalMethods.contains(method);

        replace.with("return", returnScope)
                .with("method", method.getName())
                .with("method_camelcase", cxt.camelcase(method))
                .with("method_upper", cxt.constName(method));
        String documentation = cxt.getDoc(method);

        boolean isRepeated = method.getName().endsWith("_repeated");

        String type = cxt.getType(method);
        if (type == null) {
            if (method.getName().equals("_any")) {
                outputMethod(os, "METHOD_COMMENT_ANY", documentation, isVoid, isTerminal, "METHOD_ANY_NO_SCOPE");
            } else if (isRepeated) {
                replace.with("type", method.getName());
                outputMethod(os, "METHOD_COMMENT_SCOPE", documentation, isVoid, isTerminal, "METHOD_SCOPE_REPEATED");
            } else {
                replace.with("type", cxt.camelcase(method));
                outputMethod(os, "METHOD_COMMENT_SCOPE", documentation, isVoid, isTerminal, "METHOD_SCOPE");
                referredNames.add(method);
            }
        } else if (type.equals("String")) {
            outputMethod(os, "METHOD_COMMENT", documentation, isVoid, isTerminal, "METHOD_STRING");
        } else if (type.equals("ANY")) {
            outputMethod(os, "METHOD_COMMENT_ANY", documentation, isVoid, isTerminal, "METHOD_ANY");
        } else if (type.startsWith("special:")) {
            String special = type.substring(8);
            outputMethod(os, "METHOD_COMMENT", documentation, isVoid, isTerminal, "METHOD_SPECIAL_" + special);
        } else if (type.startsWith("enum:")) {
            replace.with("type", type.substring(5));
            outputMethod(os, "METHOD_COMMENT", documentation, isVoid, isTerminal, "METHOD_SIMPLE");
            referredNames.add(method);
        } else {
            replace.with("type", type);
            outputMethod(os, "METHOD_COMMENT", documentation, isVoid, isTerminal, "METHOD_SIMPLE");
        }
        if (!isVoid) {
            if (returnScope.equals(scope)) {
                CLASS_INI.segment(os, "METHOD_RETURN_THIS", replace);
            } else {
                CLASS_INI.segment(os, "METHOD_RETURN", replace);
            }
        }
        CLASS_INI.segment(os, "METHOD_END", replace);
        if (isSkip)
            CLASS_INI.segment(os, "METHOD_SKIP", replace);
    }

    private void outputMethod(OutputStream os, String docSegment, String doc, boolean isVoid, boolean isTerminal, String methodSegment) throws IOException {
        replace.with("doc", doc == null || doc.isEmpty() ? "No doc, please update xsd" : doc);
        CLASS_INI.segment(os, docSegment + ( isVoid ? "_NORETURN" : "" ), replace);
        if (!isTerminal)
            CLASS_INI.segment(os, "METHOD_CHECK_RESULT", replace);
        CLASS_INI.segment(os, methodSegment, replace);
    }

    public void outputTags(OutputStream os, Set<QName> tags) throws IOException {
        CLASS_INI.segment(os, "TAGS_START", replace);
        for (QName tag : tags) {
            replace.with("tagname", tag.getName())
                    .with("tagname_upper", cxt.constName(tag))
                    .with("prefix", cxt.prefix(tag));
            CLASS_INI.segment(os, "TAG", replace);
        }
        CLASS_INI.segment(os, "TAGS_END", replace);
    }

}
