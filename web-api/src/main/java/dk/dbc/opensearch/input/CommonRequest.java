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

import java.util.function.Function;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import static dk.dbc.opensearch.input.RequestHelpers.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class CommonRequest extends BaseRequest {

    private static final Function<String, RelationDataType> RELATION_DATAS = mapTo(makeTrimOneOf("relationData", "type", "uri", "full"),
                                                                                   RelationDataType::from);

    private Integer showAgency = null;
    private Authentication authentication = null;
    private Boolean includeHoldingsCount = null;
    private RelationDataType relationData = null;
    private String repository = null;

    public CommonRequest() {
    }

    public void validate() throws XMLStreamException {
        super.validate();
    }

    //
    // Setters and getters
    //
    public void setAuthentication(Authentication content, Location location) throws XMLStreamException {
        authentication = get("authentication", authentication, content, location);
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setIncludeHoldingsCount(String content, Location location) throws XMLStreamException {
        includeHoldingsCount = get("includeHoldingsCount", includeHoldingsCount, content, location,
                                   s -> Boolean.parseBoolean(trimNotEmpty(s)));
    }

    public Boolean getIncludeHoldingsCount() {
        return includeHoldingsCount;
    }

    public void setRepository(String content, Location location) throws XMLStreamException {
        repository = get("repository", repository, content, location,
                         s -> trimNotEmptyOneWord(s));
    }

    public String getRepository() {
        return repository;
    }

    public void setRelationData(String content, Location location) throws XMLStreamException {
        relationData = get("relationData", relationData, content, location,
                           RELATION_DATAS);
    }

    public RelationDataType getRelationData() {
        return relationData;
    }

    public void setShowAgency(String content, Location location) throws XMLStreamException {
        showAgency = get("showAgency", showAgency, content, location,
                         s -> Integer.parseUnsignedInt(trimNotEmpty(s), 10));
    }

    public Integer getShowAgency() {
        return showAgency;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return "CommonRequest{" +
               "showAgency=" + showAgency + ", authentication=" + authentication + ", includeHoldingsCount=" + includeHoldingsCount + ", relationData=" + relationData + ", repository=" + repository +
               s.substring(s.indexOf('{'));
    }

}
