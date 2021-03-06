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
package dk.dbc.opensearch;

import dk.dbc.opensearch.output.IndexHtmlBean;
import dk.dbc.opensearch.input.RemoteIPAddress;
import dk.dbc.opensearch.input.RequestParser;
import dk.dbc.opensearch.input.RequestParserJSON;
import dk.dbc.opensearch.input.RequestParserXML;
import dk.dbc.opensearch.output.ResourceStreamingOutput;
import dk.dbc.opensearch.output.OpenSearchResponse;
import dk.dbc.opensearch.output.OpenSearchResponseJSON;
import dk.dbc.opensearch.output.OpenSearchResponseSOAP;
import dk.dbc.opensearch.output.OpenSearchResponseXML;
import dk.dbc.opensearch.output.Root;
import dk.dbc.opensearch.output.Root.EntryPoint;
import dk.dbc.opensearch.output.Root.Scope;
import dk.dbc.opensearch.output.badgerfish.BadgerFishSingle;
import dk.dbc.opensearch.reponse.GetObjectProcessorBean;
import dk.dbc.opensearch.reponse.InfoProcessorBean;
import dk.dbc.opensearch.reponse.SearchProcessorBean;
import dk.dbc.opensearch.setup.Settings;
import dk.dbc.opensearch.utils.MDCLog;
import dk.dbc.opensearch.utils.StatisticsRecorder;
import dk.dbc.opensearch.utils.Timing;
import dk.dbc.opensearch.utils.UserMessageException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.Response.Status.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
@Path("/")
public class ServiceBean {

    private static final Logger log = LoggerFactory.getLogger(ServiceBean.class);

    @Inject
    BadgerFishSingle badgerFishSingle;

    @Inject
    Settings settings;

    @Inject
    RemoteIPAddress remoteIPAddress;

    @Inject
    IndexHtmlBean indexHtml;

    @Inject
    GetObjectProcessorBean getObjectProcessor;

    @Inject
    InfoProcessorBean infoProcessor;

    @Inject
    SearchProcessorBean searchProcessor;

    @Resource(type = ManagedExecutorService.class)
    ExecutorService mes;

    @GET
    public Response index(@Context UriInfo context, @Context HttpHeaders headers, @Context HttpServletRequest httpRequest) {
        String peer = remoteIPAddress.ip(headers, httpRequest);
        try (MDCLog mdc = MDCLog.mdc()
                .withPeer(peer)) {
            MultivaluedMap<String, String> params = context.getQueryParameters();
            if (params.isEmpty()) {
                CacheControl cacheControl = new CacheControl();
                cacheControl.setMaxAge(600);
                return Response.ok(indexHtml.getBytes())
                        .type(MediaType.TEXT_HTML_TYPE)
                        .cacheControl(cacheControl)
                        .build();
            } else {
                return Response.status(BAD_REQUEST).entity("Support for uri parameters not implemented yet").build();
            }
        }
    }

    @GET
    @Path("{path: .+}")
    public Response docroot(@PathParam("path") String path, @Context HttpHeaders headers, @Context HttpServletRequest httpRequest) {
        String peer = remoteIPAddress.ip(headers, httpRequest);
        try (MDCLog mdc = MDCLog.mdc()
                .withPeer(peer)) {
            return ResourceStreamingOutput.docroot(path);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void post(@Suspended AsyncResponse response, @FormParam("req") String req, @Context HttpHeaders headers, @Context HttpServletRequest httpRequest) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(req.getBytes(StandardCharsets.UTF_8))) {
            try (BufferedInputStream is = new BufferedInputStream(bis, 1024)) {
                is.mark(1024);
                int c = is.read();
                while (Character.isWhitespace(0)) {
                    c = is.read();
                }
                if (c == -1)
                    throw new IOException("Empty request");
                is.reset();
                if (c == '{')
                    processInputStream(response, headers, httpRequest, is, RequestParserJSON::new);
                else
                    processInputStream(response, headers, httpRequest, is, RequestParserXML::new);
            }
        } catch (IOException ex) {
            log.error("post (form): {}", ex.getMessage());
            log.debug("post (form): ", ex);
            response.resume(Response.serverError().build());
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_XML,
               "application/soap+xml"})
    @Produces({MediaType.APPLICATION_XML,
               MediaType.APPLICATION_JSON})
    public void postXML(@Suspended AsyncResponse response, InputStream is, @Context HttpHeaders headers, @Context HttpServletRequest httpRequest) {
        processInputStream(response, headers, httpRequest, is, RequestParserXML::new);
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML,
               MediaType.APPLICATION_JSON})
    public void postJSON(@Suspended AsyncResponse response, InputStream is, @Context HttpHeaders headers, @Context HttpServletRequest httpRequest) {
        processInputStream(response, headers, httpRequest, is, RequestParserJSON::new);
    }

    /**
     *
     * @param resp            Suspended response
     * @param headers         Request headers (containing X-Forwarded-For)
     * @param httpRequest     Request information (containing remote IP)
     * @param is              The data to process
     * @param requestProvider The mapper from InputStream to RequestParser
     *                        object
     */
    private void processInputStream(AsyncResponse resp, HttpHeaders headers, HttpServletRequest httpRequest, InputStream is, RequestProvider requestProvider) {
        String peer = remoteIPAddress.ip(headers, httpRequest);
        try {
            MDCLog mdc = MDCLog.mdc()
                    .withPeer(peer);
            StatisticsRecorder statistics = new StatisticsRecorder();
            RequestParser request;
            try (Timing timerRequestParse = statistics.timer("requestParse")) {
                request = requestProvider.parse(is);
            }
            Root.Scope<Root.EntryPoint> builder = requestBuilder(request, statistics, mdc);
            if (builder == null)
                throw new BadRequestException("Don't know how to handle request");
            OpenSearchResponse response = responseWriter(request, builder, mdc);
            if (response == null)
                throw new BadRequestException("Don't know how to handle outputType");

            // This returns immetiately
            // Output writer runs in same thread after this methods has returned
            // This means we've got to delegate the output of statistics
            // to after output in completed, hence, passing the timer on to
            // the response processor
            mes.submit(() -> resp.resume(response.build(statistics)));
        } catch (BadRequestException ex) {
            resp.resume(Response.status(BAD_REQUEST).entity(ex.getMessage()).build());
        } catch (UserMessageException ex) {
            resp.resume(Response.status(INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
        } catch (RuntimeException ex) {
            log.error("Internal Error: {}", ex.getMessage());
            log.debug("Internal Error: ", ex);
            resp.resume(Response.status(INTERNAL_SERVER_ERROR).entity("Internal server error").build());
        } catch (XMLStreamException ex) {
            log.error("Error processing request: {}", ex.getMessage());
            log.debug("Error processing request: ", ex);
            resp.resume(Response.status(BAD_REQUEST).entity(ex.getMessage()).build());
        }
    }

    private Root.Scope<Root.EntryPoint> requestBuilder(RequestParser request, StatisticsRecorder statistics, MDCLog mdc) {
        if (request.isGetObjectRequest()) {
            mdc.withAction("getObject");
            return getObjectProcessor.builder(request.asGetObjectRequest(),
                                              statistics, mdc);
        } else if (request.isInfoRequest()) {
            mdc.withAction("info");
            return infoProcessor.builder(request.asInfoRequest(),
                                         statistics, mdc);
        } else if (request.isSearchRequest()) {
            mdc.withAction("search");
            return searchProcessor.builder(request.asSearchRequest(),
                                           statistics, mdc);
        }
        return null;
    }

    private OpenSearchResponse responseWriter(RequestParser request, Scope<EntryPoint> builder, MDCLog mdc) {
        switch (request.asBaseRequest().getOutputType()) {
            case JSON:
                mdc.withOutputType("json");
                return new OpenSearchResponseJSON(builder,
                                                  request.asBaseRequest().getCallback(),
                                                  badgerFishSingle);
            case XML:
                mdc.withOutputType("xml");
                return new OpenSearchResponseXML(builder);
            case SOAP:
                mdc.withOutputType("soap");
                return new OpenSearchResponseSOAP(builder);
            default:
                return null;
        }
    }

    private static class BadRequestException extends RuntimeException {

        private static final long serialVersionUID = -869485303588931966L;

        public BadRequestException(String string) {
            super(string);
        }
    }

    @FunctionalInterface
    private interface RequestProvider {

        RequestParser parse(InputStream is) throws XMLStreamException;
    }
}
