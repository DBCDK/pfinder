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

import dk.dbc.opensearch.output.badgerfish.BadgerFishSingle;
import dk.dbc.opensearch.output.badgerfish.BadgerFishWriter;
import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLStreamException;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OpenSearchResponseJSON extends OpenSearchResponse {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(OpenSearchResponseJSON.class);

    private final BadgerFishSingle badgerFishSingle;

    private final String callback;

    public OpenSearchResponseJSON(Root.Scope<Root.EntryPoint> entryPoint, String callback, BadgerFishSingle badgerFishSingle) {
        super(entryPoint);
        this.callback = callback == null || callback.isEmpty() ? null : callback;
        this.badgerFishSingle = badgerFishSingle;
    }

    @Override
    public void stream(OutputStream output) throws XMLStreamException, IOException, WebApplicationException {
        try {
            if (callback != null)
                output.write(( callback + " && " + callback + "(" ).getBytes(UTF_8));
            BadgerFishWriter writer = new BadgerFishWriter(output, badgerFishSingle);
            new Root(writer).noEnvelope(this::handle);
            if (callback != null)
                output.write(")".getBytes(UTF_8));
        } catch (XMLStreamException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public String mediaType() {
        return callback == null ? MediaType.APPLICATION_JSON : "application/javascript";
    }

}
