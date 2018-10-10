/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-service
 *
 * opensearch-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.input;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import static dk.dbc.opensearch.input.RequestHelpers.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class UserDefinedBoost {

    private String fieldName = null;
    private String fieldValue = null;
    private Double weight = null;

    public UserDefinedBoost() {
    }

    public void validate(Location location) throws XMLStreamException {
        if (fieldName == null)
            throw new XMLStreamException("fieldName is a required property of userDefinedBoost", location);
        if (fieldValue == null)
            throw new XMLStreamException("fieldValue is a required property of userDefinedBoost", location);
        if (weight == null)
            throw new XMLStreamException("weight is a required property of userDefinedBoost", location);
    }

    public void setFieldName(String content, Location location) throws XMLStreamException {
        fieldName = get("fieldName", fieldValue, content, location, s -> trimNotEmptyOneWord(s));
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldValue(String content, Location location) throws XMLStreamException {
        fieldValue = get("fieldValue", fieldValue, content, location);
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setWeight(String content, Location location) throws XMLStreamException {
        weight = get("weight", weight, content, location, s -> Double.parseDouble(trimNotEmpty(s)));
    }

    public Double getWeight() {
        return weight;
    }

}
