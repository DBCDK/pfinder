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
package dk.dbc.opensearch.cache;

import dk.dbc.opensearch.setup.Settings;
import dk.dbc.opensearch.utils.StatisticsRecorder;
import dk.dbc.opensearch.utils.Timing;
import dk.dbc.opensearch.utils.UserMessage;
import dk.dbc.opensearch.utils.UserMessageException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
public class HttpFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpFetcher.class);

    @Inject
    Client client;

    @Inject
    Settings settings;

    /**
     * Make a post request
     *
     * @param uriTemplate Template for substituting in
     * @param trackingId  tracking id, to add to requests
     * @return Request context for adding values
     */
    public PostContext post(String uriTemplate, String trackingId) {
        UriBuilder uriBuilder = UriBuilder.fromUri(uriTemplate);
        return new PostContext(client, uriBuilder, settings.getUserAgentOrDrefault(), trackingId);
    }

    /**
     * Make a get request
     *
     * @param uriTemplate Template for substituting in
     * @param trackingId  tracking id, to add to requests
     * @return Request context for adding values
     */
    public GetContext get(String uriTemplate, String trackingId) {
        UriBuilder uriBuilder = UriBuilder.fromUri(uriTemplate);
        return new GetContext(client, uriBuilder, settings.getUserAgentOrDrefault(), trackingId);
    }

    public final static class GetContext {

        private final Client client;
        private final UriBuilder uriBuilder;
        private final HashMap<String, String> values;
        private final String userAgent;

        private GetContext(Client client, UriBuilder uriBuilder, String userAgent, String trackingId) {
            this.client = client;
            this.uriBuilder = uriBuilder;
            this.values = new HashMap<>();
            this.userAgent = userAgent;
            this.values.put("trackingId", trackingId);
        }

        /**
         * Set path parameters (substitution)
         *
         * @param key   name of the parameter
         * @param value what to place instead
         * @return self for chaining
         */
        public GetContext with(String key, String value) {
            values.put(key, value);
            return this;
        }

        /**
         * Get the request as an InputStream
         *
         * @param stats Statistics module
         * @param name  request name for statistics
         * @return InputStream and throws runtime exception in case of an error
         */
        public InputStream request(StatisticsRecorder stats, String name) {
            return request(MediaType.APPLICATION_JSON_TYPE, stats, name);
        }

        /**
         * Get the request as an InputStream
         *
         * @param acceptType the content type wanted from the remote server
         * @param stats      Statistics module
         * @param name       request name for statistics
         * @return InputStream and throws runtime exception in case of an error
         */
        public InputStream request(MediaType acceptType, StatisticsRecorder stats, String name) {
            InputStream is = null;
            URI uri = uriBuilder.buildFromMap(values);
            log.debug("Fetching: (GET) {}", uri);
            try (final Timing timer = stats.timer(name)) {
                is = client.target(uri)
                        .request().accept(acceptType).header("User-Agent", userAgent).get(InputStream.class);
            } catch (ClientErrorException | ServerErrorException ex) {
                log.error("Error fetching resource: {}: {}", uri, ex.getMessage());
                log.debug("Error fetching resource: ", ex);
                throw new UserMessageException(UserMessage.BAD_RESPONSE);
            }
            if (is == null) {
                log.error("Error fetching resource: {}: No content");
                throw new UserMessageException(UserMessage.BAD_RESPONSE);
            }
            return is;
        }
    }

    public final static class PostContext {

        private final Client client;
        private final UriBuilder uriBuilder;
        private final HashMap<String, String> values;
        private final String userAgent;

        private PostContext(Client client, UriBuilder uriBuilder, String userAgent, String trackingId) {
            this.client = client;
            this.uriBuilder = uriBuilder;
            this.values = new HashMap<>();
            this.userAgent = userAgent;
            this.values.put("trackingId", trackingId);
        }

        /**
         * Set path parameters (substitution)
         *
         * @param key   name of the parameter
         * @param value what to place instead
         * @return self for chaining
         */
        public PostContext with(String key, String value) {
            values.put(key, value);
            return this;
        }

        /**
         * Get the request as an InputStream
         *
         * @param entity the entity to post
         * @param stats  Statistics module
         * @param name   request name for statistics
         * @return InputStream and throws runtime exception in case of an error
         */
        public InputStream request(Entity entity, StatisticsRecorder stats, String name) {
            return request(entity, MediaType.APPLICATION_JSON_TYPE, stats, name);
        }

        /**
         * Get the request as an InputStream
         *
         * @param entity     the entity to post
         * @param acceptType the content type wanted from the remote server
         * @param stats      Statistics module
         * @param name       request name for statistics
         * @return InputStream and throws runtime exception in case of an error
         */
        public InputStream request(Entity entity, MediaType acceptType, StatisticsRecorder stats, String name) {
            InputStream is = null;
            URI uri = uriBuilder.buildFromMap(values);
            log.debug("Fetching: (POST) {}", uri);
            try (final Timing timer = stats.timer(name)) {
                is = client.target(uri)
                        .request()
                        .accept(acceptType)
                        .header("User-Agent", userAgent)
                        .post(entity, InputStream.class);
            } catch (ClientErrorException | ServerErrorException ex) {
                log.error("Error fetching resource: {}: {}", uri, ex.getMessage());
                log.debug("Error fetching resource: ", ex);
                throw new UserMessageException(UserMessage.BAD_RESPONSE);
            }
            if (is == null) {
                log.error("Error fetching resource: {}: No content");
                throw new UserMessageException(UserMessage.BAD_RESPONSE);
            }
            return is;
        }
    }
}
