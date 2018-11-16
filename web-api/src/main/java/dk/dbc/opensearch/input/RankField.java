/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-solr
 *
 * opensearch-solr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-solr is distributed in the hope that it will be useful,
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
public class RankField implements InputPart {

    public static final InputPartFactory<RankField> FACTORY =
            new InputPartFactory<>(RankField::new)
                    .with("fieldName", obj -> obj::setFieldName)
                    .with("fieldType", obj -> obj::setFieldType)
                    .with("weight", obj -> obj::setWeight);

    private String fieldName = null;
    private String fieldType = null;
    private Double weight = null;

    public RankField() {
    }

    public void validate(Location location) throws XMLStreamException {
        if (fieldName == null)
            throw new XMLStreamException("fieldName is a required property of rankField", location);
        if (fieldType == null)
            throw new XMLStreamException("fieldType is a required property of rankField", location);
        if (weight == null)
            throw new XMLStreamException("weight is a required property of rankField", location);
    }

    public void setFieldName(String content, Location location) throws XMLStreamException {
        fieldName = get("fieldName", fieldName, content, location, s -> trimNotEmptyOneWord(s));
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldType(String content, Location location) throws XMLStreamException {
        fieldType = get("fieldType", fieldType, content, location, s -> trimNotEmptyOneWord(s));
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setWeight(String content, Location location) throws XMLStreamException {
        weight = get("weight", weight, content, location, s -> Double.parseDouble(trimNotEmpty(s)));
    }

    public Double getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "RankField{" + "fieldName=" + fieldName + ", fieldType=" + fieldType + ", weight=" + weight + '}';
    }

}
