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

import java.util.Map;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class QNameBuilder {

    private final Map<String, String> namespaces;
    private final String targetNamespace;

    public QNameBuilder(Map<String, String> namespaces, String targetNamespace) {
        this.namespaces = namespaces;
        this.targetNamespace = targetNamespace;
    }

    public QName from(String name) {
        return new QName(name, namespaces, targetNamespace);
    }
}
