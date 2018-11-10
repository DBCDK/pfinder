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

import dk.dbc.xsd.mapping.Enumeration;
import dk.dbc.xsd.mapping.SimpleType;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class EnumBuilder {

    private static final Ini INI = new Ini("enum.ini");

    public static void build(QNameBuilder qNameBuilder, SimpleType type, File targetFolder, String packageName, String rootClass, String className) throws IOException {
        new EnumBuilder(qNameBuilder, type, rootClass, className)
                .build(targetFolder, packageName);
    }

    private final QNameBuilder qNameBuilder;
    private final SimpleType type;
    private final String className;
    private final String rootClass;
    private final Replace replace;

    public EnumBuilder(QNameBuilder qNameBuilder, SimpleType type, String rootClass, String className) {
        this.qNameBuilder = qNameBuilder;
        this.type = type;
        this.rootClass = rootClass;
        this.className = className;
        this.replace = Replace.of("class", className)
                .with("root", rootClass);
    }

    private void build(File targetFolder, String packageName) throws IOException {
        replace.with("package", packageName);
        try (JavaFileOutputStream os = new JavaFileOutputStream(targetFolder, packageName, className)) {
            output(os);
        }
    }

    private void output(OutputStream os) throws IOException {
        String names = type.restriction.enumeration.stream()
                .map(s -> enumName(s.value))
                .collect(Collectors.joining(", "));
        replace.with("names", names);
        INI.segment(os, "TOP", replace);
        for (Enumeration enumeration : type.restriction.enumeration) {
            replace.with("name", enumName(enumeration.value))
                    .with("name_lower", enumeration.value.toLowerCase(Locale.ROOT));
            INI.segment(os, "VALUE", replace);
        }
        INI.segment(os, "BOTTOM", replace);
    }

    private String enumName(String s) {
        return s.replaceAll("[^0-9a-zA-Z_]", "").toUpperCase(Locale.ROOT);
    }

}
