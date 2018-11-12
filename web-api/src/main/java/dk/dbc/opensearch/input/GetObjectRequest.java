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
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import static dk.dbc.opensearch.input.RequestHelpers.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class GetObjectRequest extends CommonRequest {

    private List<String> identifier = null;
    private List<String> localIdentifier = null;
    private List<AgencyAndLocalIdentifier> agencyAndLocalIdentifier = null;
    private List<String> objectFormat = new ArrayList<>();

    public GetObjectRequest() {
    }

    @Override
    public void validate() throws XMLStreamException {
        super.validate();
        if (identifier == null ||
            localIdentifier == null ||
            agencyAndLocalIdentifier == null)
            throw new XMLStreamException("one of identifier, localIdentifier or agencyAndLocalIdentifier iw required");
    }

    //
    // Setters and getters
    //
    public void addIdentifier(String content, Location location) throws XMLStreamException {
        if (localIdentifier != null)
            throw new XMLStreamException("Cannot have both localIdentifier and identifier", location);
        if (agencyAndLocalIdentifier != null)
            throw new XMLStreamException("Cannot have both agencyAndLocalIdentifier and identifier", location);
        if (identifier == null)
            identifier = new ArrayList<>();
        this.identifier.add(get("identifier", content, location,
                                s -> trimNotEmpty(s)));
    }

    public List<String> getIdentifier() {
        return identifier;
    }

    public void addLocalIdentifier(String content, Location location) throws XMLStreamException {
        if (identifier != null)
            throw new XMLStreamException("Cannot have both identifier and localIdentifier", location);
        if (agencyAndLocalIdentifier != null)
            throw new XMLStreamException("Cannot have both agencyAndLocalIdentifier and localIdentifier", location);
        if (localIdentifier == null)
            localIdentifier = new ArrayList<>();
        this.localIdentifier.add(get("identifier", content, location,
                                     s -> trimNotEmpty(s)));
    }

    public List<String> getLocalIdentifier() {
        return localIdentifier;
    }

    public void addAgencyAndLocalIdentifier(AgencyAndLocalIdentifier content, Location location) throws XMLStreamException {
        if (identifier != null)
            throw new XMLStreamException("Cannot have both identifier and agencyAndLocalIdentifier", location);
        if (localIdentifier != null)
            throw new XMLStreamException("Cannot have both localIdentifier and agencyAndLocalIdentifier", location);
        if (agencyAndLocalIdentifier == null)
            agencyAndLocalIdentifier = new ArrayList<>();
        this.agencyAndLocalIdentifier.add(content);
    }

    public List<AgencyAndLocalIdentifier> getAgencyAndLocalIdentifier() {
        return agencyAndLocalIdentifier;
    }

    public void addObjectFormat(String content, Location location) throws XMLStreamException {
        this.objectFormat.add(get("objectFormat", content, location,
                                  s -> trimNotEmptyOneWord(s)));
    }

    public List<String> getObjectFormat() {
        return objectFormat;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return "GetObjectRequest{" +
               "identifier=" + identifier + ", localIdentifier=" + localIdentifier + ", agencyAndLocalIdentifier=" + agencyAndLocalIdentifier + ", objectFormat=" + objectFormat +
               s.substring(s.indexOf('{') + 1);
    }

}
