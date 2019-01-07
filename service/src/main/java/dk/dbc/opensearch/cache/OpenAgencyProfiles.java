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
import dk.dbc.opensearch.solr.SolrRules;
import dk.dbc.opensearch.solr.profile.Profiles;
import dk.dbc.opensearch.utils.StatisticsRecorder;
import dk.dbc.opensearch.utils.Timing;
import dk.dbc.opensearch.utils.UserMessage;
import dk.dbc.opensearch.utils.UserMessageException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Stateless
public class OpenAgencyProfiles {

    private static final Logger log = LoggerFactory.getLogger(OpenAgencyProfiles.class);

    @Inject
    Settings settings;

    @Inject
    HttpFetcher fetcher;

    /**
     * Cached open agency profile
     *
     * @param agencyId   Agency to fetch profiles for
     * @param repository SolR rules are different for each repository (cache
     *                   key)
     * @param stats      record HTTP timing
     * @param trackingId Track which HTTP requests are produced by an enduser
     *                   request
     * @param solrRules  Rules for the SolR identified by "repository"
     * @return Profiles object for the agency
     */
    @CacheResult(cacheName = "profile",
                 exceptionCacheName = "profile_error",
                 cachedExceptions = {ClientErrorException.class,
                                     ServerErrorException.class,
                                     UserMessageException.class})
    public Profiles getProfileFor(@CacheKey int agencyId, @CacheKey String repository,
                                  StatisticsRecorder stats, String trackingId, SolrRules solrRules) {
        try (InputStream is = fetcher.get(settings.getOpenagencyProfileUrl(), trackingId)
                .with("agencyId", String.format(Locale.ROOT, "%06d", agencyId))
                .request(stats, "openagency-profile-fetch")) {
            try (Timing timer = stats.timer("openagency-profile-build")) {
                return Profiles.from(solrRules, is);
            }
        } catch (IOException ex) {
            log.error("Cannot handle profile response for: {}: {}", agencyId, ex.getMessage());
            log.debug("Cannot handle profile response for: {}: ", agencyId, ex);
            throw new UserMessageException(UserMessage.BAD_RESPONSE);
        }
    }

}
