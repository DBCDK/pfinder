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

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import static dk.dbc.opensearch.input.RequestHelpers.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class UserDefinedRanking implements InputPart {

    public static final InputPartFactory<UserDefinedRanking> FACTORY =
            new InputPartFactory<>(UserDefinedRanking::new)
                    .with("tieValue", obj -> obj::setTieValue)
                    .with("rankField", RankField.FACTORY, obj -> obj::addRankField);

    private Double tieValue = null;
    private final List<RankField> rankField = new ArrayList<>();

    public UserDefinedRanking() {
    }

    public void validate(Location location) throws XMLStreamException {
        if (tieValue == null)
            throw new XMLStreamException("tieValue is a required property of userDefinedRanking", location);
        if (rankField.isEmpty())
            throw new XMLStreamException("rankField is a required property of userDefinedRanking", location);
    }

    public void addRankField(RankField content, Location location) throws XMLStreamException {
        rankField.add(content);
    }

    public List<RankField> getRankField() {
        return rankField;
    }

    public void setTieValue(String content, Location location) throws XMLStreamException {
        tieValue = get("tieValue", tieValue, content, location, s -> Double.parseDouble(trimNotEmpty(s)));
    }

    public Double getTieValue() {
        return tieValue;
    }

    @Override
    public String toString() {
        return "UserDefinedRanking{" + "tieValue=" + tieValue + ", rankField=" + rankField + '}';
    }

}
