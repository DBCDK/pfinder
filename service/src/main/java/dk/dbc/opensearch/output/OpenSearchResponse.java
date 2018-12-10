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

import dk.dbc.opensearch.utils.StatisticsRecorder;
import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public abstract class OpenSearchResponse implements StreamingOutput {

    private final Root.Scope<Root.EntryPoint> entryPoint;
    protected StatisticsRecorder statistics;

    /**
     *
     * @param entryPoint Something that can consume an Root.EntryPoint and
     *                   produce some output
     */
    public OpenSearchResponse(Root.Scope<Root.EntryPoint> entryPoint) {
        this.entryPoint = entryPoint;
    }

    public abstract String mediaType();

    public Response build(StatisticsRecorder statistics) {
        this.statistics = statistics;
        return Response.ok(this)
                .type(mediaType())
                .build();
    }

    @Override
    public final void write(OutputStream out) throws IOException, WebApplicationException {
        try {
            stream(out);
        } catch (XMLStreamException ex) {
            throw new IOException(ex);
        } finally {
            statistics.log();
        }
    }

    public abstract void stream(OutputStream out) throws XMLStreamException, IOException, WebApplicationException;

    public void handle(Root.EntryPoint e) throws XMLStreamException, IOException {
        entryPoint.apply(e);
    }

}
