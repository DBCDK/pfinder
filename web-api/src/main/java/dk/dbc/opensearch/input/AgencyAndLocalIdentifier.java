/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-web-api
 *
 * opensearch-web-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-web-api is distributed in the hope that it will be useful,
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

import static dk.dbc.opensearch.input.RequestHelpers.get;
import static dk.dbc.opensearch.input.RequestHelpers.trimNotEmpty;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class AgencyAndLocalIdentifier {

    private String agency = null;
    private String localIdentifier = null;

    public AgencyAndLocalIdentifier() {
    }

    public void validate(Location location) throws XMLStreamException {
        if (agency == null)
            throw new XMLStreamException("agency is a required property of agencyAndLocalIdentifier", location);
        if (localIdentifier == null)
            throw new XMLStreamException("localIdentifier is a required property of agencyAndLocalIdentifier", location);
    }

    public void setAgency(String content, Location location) throws XMLStreamException {
        agency = get("agency", agency, content, location,
                     s -> trimNotEmpty(s));
    }

    public String getAgency() {
        return agency;
    }

    public void setLocalIdentifier(String content, Location location) throws XMLStreamException {
        localIdentifier = get("localIdentifier", localIdentifier, content, location,
                              s -> trimNotEmpty(s));
    }

    public String getLocalIdentifier() {
        return localIdentifier;
    }

    @Override
    public String toString() {
        return "AgencyAndLocalIdentifier{" + "agency=" + agency + ", localIdentifier=" + localIdentifier + '}';
    }

}
