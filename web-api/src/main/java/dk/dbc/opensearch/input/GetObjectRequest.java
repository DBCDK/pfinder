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
public class GetObjectRequest extends CommonRequest {

    public static final InputPartFactory<GetObjectRequest> FACTORY =
            new InputPartFactory<>(GetObjectRequest::new)
                    // Base
                    .with("agency", obj -> obj::putAgency)
                    .with("profile", obj -> obj::addProfile)
                    .with("callback", obj -> obj::putCallback)
                    .with("outputType", obj -> obj::putOutputType)
                    .with("trackingId", obj -> obj::putTrackingId)
                    // Common
                    .with("showAgency", obj -> obj::putShowAgency)
                    .with("authentication", Authentication.FACTORY, obj -> obj::putAuthentication)
                    .with("includeHoldingsCount", obj -> obj::putIncludeHoldingsCount)
                    .with("relationData", obj -> obj::putRelationData)
                    .with("repository", obj -> obj::putRepository)
                    // Local
                    .with("identifier", obj -> obj::addIdentifier)
                    .with("localIdentifier", obj -> obj::addLocalIdentifier)
                    .with("agencyAndLocalIdentifier", AgencyAndLocalIdentifier.READER, obj -> obj::addAgencyAndLocalIdentifier)
                    .with("objectFormat", obj -> obj::addObjectFormat);

    private List<String> identifier = null;
    private List<String> localIdentifier = null;
    private List<AgencyAndLocalIdentifier> agencyAndLocalIdentifier = null;
    private List<String> objectFormat = new ArrayList<>();

    public GetObjectRequest() {
    }

    @Override
    public void validate(Location location) throws XMLStreamException {
        super.validate(location);
        if (identifier == null ||
            localIdentifier == null ||
            agencyAndLocalIdentifier == null)
            throw new XMLStreamException("one of identifier, localIdentifier or agencyAndLocalIdentifier is required");
        if (identifier != null && ( localIdentifier != null || agencyAndLocalIdentifier != null ) ||
            ( localIdentifier != null && agencyAndLocalIdentifier != null ))
            throw new XMLStreamException("only one of identifier, localIdentifier or agencyAndLocalIdentifier is allowed");
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

    public void setIdentifier(List<String> identifier) {
        this.identifier = identifier;
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

    public void setLocalIdentifier(List<String> localIdentifier) {
        this.localIdentifier = localIdentifier;
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

    public void setAgencyAndLocalIdentifier(List<AgencyAndLocalIdentifier> agencyAndLocalIdentifier) {
        this.agencyAndLocalIdentifier = agencyAndLocalIdentifier;
    }

    public void addObjectFormat(String content, Location location) throws XMLStreamException {
        this.objectFormat.add(get("objectFormat", content, location,
                                  s -> trimNotEmptyOneWord(s)));
    }

    public List<String> getObjectFormat() {
        return objectFormat;
    }

    public void setObjectFormat(List<String> objectFormat) {
        this.objectFormat = objectFormat;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return "GetObjectRequest{" +
               "identifier=" + identifier + ", localIdentifier=" + localIdentifier + ", agencyAndLocalIdentifier=" + agencyAndLocalIdentifier + ", objectFormat=" + objectFormat +
               s.substring(s.indexOf('{') + 1);
    }

}
