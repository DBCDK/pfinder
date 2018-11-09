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
package dk.dbc.xsd.mapping;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@XmlType
public class Any {

    @XmlAttribute
    public String minOccurs;

    @XmlAttribute
    public String maxOccurs;

    @Override
    public String toString() {
        return "Any{" + "minOccurs=" + minOccurs + ", maxOccurs=" + maxOccurs + '}';
    }

}
