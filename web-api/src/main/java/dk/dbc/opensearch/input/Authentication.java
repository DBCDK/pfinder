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
public class Authentication implements InputPart {

    public static final InputPartFactory<Authentication> FACTORY =
            new InputPartFactory<>(Authentication::new)
                    .with("groupIdAut", obj -> obj::setGroupIdAut)
                    .with("passwordAut", obj -> obj::setPasswordAut)
                    .with("userIdAut", obj -> obj::setUserIdAut);

    private String groupIdAut = null;
    private String passwordAut = null;
    private String userIdAut = null;

    public Authentication() {
    }

    public void validate(Location location) throws XMLStreamException {
        if (groupIdAut == null)
            throw new XMLStreamException("groupIdAut is a required property of authentication", location);
        if (passwordAut == null)
            throw new XMLStreamException("passwordAut is a required property of authentication", location);
        if (userIdAut == null)
            throw new XMLStreamException("userIdAut is a required property of authentication", location);
    }

    public void setGroupIdAut(String content, Location location) throws XMLStreamException {
        groupIdAut = get("groupIdAut", groupIdAut, content, location,
                         s -> trimNotEmpty(s));
    }

    public String getGroupIdAut() {
        return groupIdAut;
    }

    public void setPasswordAut(String content, Location location) throws XMLStreamException {
        passwordAut = get("passwordAut", passwordAut, content, location);
    }

    public String getPasswordAut() {
        return passwordAut;
    }

    public void setUserIdAut(String content, Location location) throws XMLStreamException {
        userIdAut = get("userIdAut", userIdAut, content, location,
                        s -> trimNotEmpty(s));
    }

    public String getUserIdAut() {
        return userIdAut;
    }

    @Override
    public String toString() {
        return "Authentication{" + "groupIdAut=" + groupIdAut + ", passwordAut=********, userIdAut=" + userIdAut + '}';
    }

}
