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

import java.util.Map;
import java.util.Objects;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class QName {

    private final String prefix;
    private final String name;
    private final String namespace;

    public QName(String in, Map<String, String> namespaces, String targetNamespace) {
        String[] a = in.split(":", 2);
        if (a.length == 1) {
            prefix = "";
            name = in;
        } else {
            prefix = a[0];
            name = a[1];
        }
        namespace = namespaces.getOrDefault(prefix, targetNamespace);
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.namespace);
        hash = 29 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final QName other = (QName) obj;
        if (!Objects.equals(this.namespace, other.namespace))
            return false;
        if (!Objects.equals(this.name, other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return prefix + ":" + name;
    }

}
