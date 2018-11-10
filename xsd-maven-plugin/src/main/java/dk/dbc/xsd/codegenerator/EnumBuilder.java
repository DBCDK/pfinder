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

    private final SimpleType simpleType;
    private final String className;
    private final Replace replace;
    private final Context cxt;

    public EnumBuilder(Context cxt, SimpleType simpleType, String className) {
        this.cxt = cxt;
        this.simpleType = simpleType;
        this.className = className;
        this.replace = cxt.replacer()
                .with("class", className);
    }

    public void build() throws IOException {
        System.out.println("Building: " + className);
        try (JavaFileOutputStream os = new JavaFileOutputStream(cxt, className)) {
            output(os);
        }
    }

    private void output(OutputStream os) throws IOException {
        String names = simpleType.restriction.enumeration.stream()
                .map(s -> enumValueName(s.value))
                .collect(Collectors.joining(", "));
        replace.with("names", names);
        INI.segment(os, "TOP", replace);
        for (Enumeration enumeration : simpleType.restriction.enumeration) {
            replace.with("name", enumValueName(enumeration.value))
                    .with("name_lower", enumeration.value.toLowerCase(Locale.ROOT));
            INI.segment(os, "VALUE", replace);
        }
        INI.segment(os, "BOTTOM", replace);
    }

    private String enumValueName(String s) {
        return s.replaceAll("[^0-9a-zA-Z_]", "").toUpperCase(Locale.ROOT);
    }

}
