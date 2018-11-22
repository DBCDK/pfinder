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
import java.util.function.Function;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import static dk.dbc.opensearch.input.RequestHelpers.*;
import static java.util.Collections.EMPTY_LIST;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class BaseRequest implements InputPart {

    private static final Function<String, OutputType> OUTPUT_TYPES = mapTo(makeTrimOneOf("outputType", "xml", "json", "soap"),
                                                                           OutputType::from);

    private Integer agency = null;
    private List<String> profile;
    private String callback = null;
    private OutputType outputType = null;
    private String trackingId = null;

    public BaseRequest() {
    }

    @Override
    public void validate(Location location) throws XMLStreamException {
        if (agency == null)
            throw new XMLStreamException("property 'agency' is required in a searchRequest");
        if (profile == null || profile.isEmpty())
            throw new XMLStreamException("property 'profile' is required in a searchRequest");
    }

    //
    // Setters and getters
    //
    public void putAgency(String content, Location location) throws XMLStreamException {
        agency = get("agency", agency, content, location,
                     s -> Integer.parseUnsignedInt(trimNotEmpty(s), 10));
    }

    public Integer getAgency() {
        return agency;
    }

    public void setAgency(Integer agency) {
        this.agency = agency;
    }

    public void putCallback(String content, Location location) throws XMLStreamException {
        callback = get("callback", callback, content, location);
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public void putOutputType(String content, Location location) throws XMLStreamException {
        outputType = get("outputType", outputType, content, location,
                         OUTPUT_TYPES);
    }

    public OutputType getOutputType() {
        return outputType;
    }

    public void setOutputType(OutputType outputType) {
        this.outputType = outputType;
    }

    public void addProfile(String content, Location location) throws XMLStreamException {
        if (profile == null)
            profile = new ArrayList<>();
        profile.add(get("profile", content, location,
                        s -> trimNotEmptyOneWord(s)));
    }

    public List<String> getProfile() {
        return profile;
    }

    public final List<String> getProfilesOrDefault() {
        return profile == null ? EMPTY_LIST : profile;
    }

    public void setProfile(List<String> profiles) {
        this.profile = profiles;
    }

    public void putTrackingId(String content, Location location) throws XMLStreamException {
        trackingId = get("trackingId", trackingId, content, location);
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    @Override
    public String toString() {
        return "BaseRequest{" + "agency=" + agency + ", profile=" + getProfilesOrDefault() + ", callback=" + callback + ", outputType=" + outputType + ", trackingId=" + trackingId + '}';
    }

}
