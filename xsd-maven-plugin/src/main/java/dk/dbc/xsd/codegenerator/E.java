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

import dk.dbc.xsd.mapping.Any;
import dk.dbc.xsd.mapping.Choice;
import dk.dbc.xsd.mapping.ComplexType;
import dk.dbc.xsd.mapping.Element;
import dk.dbc.xsd.mapping.Sequence;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class E {

    private final Object o;

    public E(Object o) {
        if (o instanceof ComplexType) {
            this.o = ( (ComplexType) o ).obj;
        } else {
            this.o = o;
        }
    }

    public boolean isAny() {
        return o instanceof Any;
    }

    public Any asAny() {
        return (Any) o;
    }

    public boolean isElement() {
        return o instanceof Element;
    }

    public Element asElement() {
        return (Element) o;
    }

    public boolean isSequence() {
        return o instanceof Sequence;
    }

    public List<E> asSequence() {
        return ( (Sequence) o ).sequence.stream().map(E::new).collect(Collectors.toList());
    }

    public boolean isChoice() {
        return o instanceof Choice;
    }

    public List<E> asChoice() {
        return ( (Choice) o ).choice.stream().map(E::new).collect(Collectors.toList());
    }

    private String minOccurs() {
        if (isAny())
            return asAny().minOccurs;
        if (isElement())
            return asElement().minOccurs;
        return null;
    }

    private String maxOccurs() {
        if (isAny())
            return asAny().maxOccurs;
        if (isElement())
            return asElement().maxOccurs;
        return null;
    }

    public boolean isOptional() {
        return "0".equals(minOccurs());
    }

    public boolean isRepeatable() {
        String maxOccurs = maxOccurs();
        return maxOccurs != null && !"1".equals(maxOccurs);
    }

    @Override
    public String toString() {
        return String.valueOf(o);
    }

}
