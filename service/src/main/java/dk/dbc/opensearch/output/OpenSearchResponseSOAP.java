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
package dk.dbc.opensearch.output;

import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.WebApplicationException;
import javax.xml.stream.XMLStreamException;

public class OpenSearchResponseSOAP extends OpenSearchResponse {

    public OpenSearchResponseSOAP(Root.Scope<Root.EntryPoint> entryPoint) {
        super(entryPoint);
    }

    @Override
    public void stream(OutputStream output) throws XMLStreamException, IOException, WebApplicationException {
        Root root = new Root(output);
        root.soapEnvelope(this::handle);
    }

    @Override
    public String mediaType() {
        return "application/xml";
    }

}
