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
package dk.dbc.opensearch.setup;

import dk.dbc.opensearch.utils.UserMessage;
import dk.dbc.opensearch.utils.UserMessageException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Settings {

    private static final Logger log = LoggerFactory.getLogger(Settings.class);

    private String defaultRepository;
    private String badgerfishRulesLocation;
    private String solrRulesLocation;
    private Map<String, String> jCache;
    private Map<String, String> defaultNamespaces;
    private String xForwardedFor;
    private HttpClient httpClient;
    private EnumMap<UserMessage, String> UserMessages;
    private String userAgent;
    private String openagencyProfileUrl;
    private Map<String, RepositorySettings> repositories;

    public Settings() {
        httpClient = new HttpClient();
    }

    public String getDefaultRepository() {
        return defaultRepository;
    }

    public void setDefaultRepository(String defaultRepository) {
        this.defaultRepository = defaultRepository;
    }

    public String getBadgerfishRulesLocation() {
        return badgerfishRulesLocation;
    }

    public void setBadgerfishRulesLocation(String badgerfishRulesLocation) {
        this.badgerfishRulesLocation = badgerfishRulesLocation;
    }

    public String getSolrRulesLocation() {
        return solrRulesLocation;
    }

    public void setSolrRulesLocation(String solrRulesLocation) {
        this.solrRulesLocation = solrRulesLocation;
    }

    public String getOpenagencyProfileUrl() {
        return openagencyProfileUrl;
    }

    public void setOpenagencyProfileUrl(String openagencyProfileUrl) {
        this.openagencyProfileUrl = openagencyProfileUrl;
    }

    public Map<String, String> getJCache() {
        return jCache;
    }

    public void setJCache(Map<String, String> jCache) {
        this.jCache = jCache;
    }

    public Map<String, String> getDefaultNamespaces() {
        return defaultNamespaces;
    }

    public void setDefaultNamespaces(Map<String, String> defaultNamespaces) {
        this.defaultNamespaces = defaultNamespaces;
    }

    public String getXForwardedFor() {
        return xForwardedFor;
    }

    public void setXForwardedFor(String xForwardedFor) {
        this.xForwardedFor = xForwardedFor;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public EnumMap<UserMessage, String> getUserMessages() {
        return UserMessages;
    }

    public void setUserMessages(EnumMap<UserMessage, String> UserMessages) {
        this.UserMessages = UserMessages;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgency) {
        this.userAgent = userAgency;
    }

    public String getUserAgentOrDrefault() {
        return userAgent != null ? userAgent : "OpenSearch (unknown/0.0)";
    }

    public Map<String, RepositorySettings> getRepositories() {
        return repositories;
    }

    public void setRepositories(Map<String, RepositorySettings> repositories) {
        this.repositories = repositories;
    }


    /*
    *      _____________   ____________  ___  ________________
    *     / ____/ ____/ | / / ____/ __ \/   |/_  __/ ____/ __ \
    *    / / __/ __/ /  |/ / __/ / /_/ / /| | / / / __/ / / / /
    *   / /_/ / /___/ /|  / /___/ _, _/ ___ |/ / / /___/ /_/ /
    *   \____/_____/_/ |_/_____/_/ |_/_/  |_/_/ /_____/_____/
    *
     */
    private Map<String, String> defaultNamespacesInverse;
    private Map<String, RepositorySettings> repositoryByAlias;

    /**
     * Given a repo name (from the request) get the configured repo name
     *
     * @param alias Name of repository as supplied by user
     * @return the settings for that repository
     */
    public RepositorySettings lookupRepositoryByAlias(String alias) {
        RepositorySettings settings = repositoryByAlias.get(alias);
        if (settings == null) {
            log.error("Error looking up repository: {}", alias);
            throw new UserMessageException(UserMessage.UNKNOWN_REPOSITORY);
        }
        return settings;
    }

    /**
     * Lookup a default prefix for a given namespace, or generate a prefix for
     * it
     *
     * @param namespace           the namespace uri
     * @param generatedNamespaces the map for not default namespaces
     * @return a prefix
     */
    public String lookupNamespacePrefix(String namespace, Map<String, String> generatedNamespaces) {
        String prefix = defaultNamespaces.get(namespace);
        if (prefix == null) {
            prefix = generatedNamespaces.computeIfAbsent(namespace,
                                                         p -> {
                                                     log.error("Registering unspecified namespace: {} - please fix settings.yaml", p);
                                                     return "ns" + ( generatedNamespaces.size() + 1 );
                                                 });
        }
        return prefix;
    }

    void validateAndProcess() {
        defaultRepository = defaultRepository.trim();
        if (defaultRepository == null || defaultRepository.isEmpty())
            throw new IllegalArgumentException("Required parameter defaultRepository is missing from configuration.yaml");
        if (repositories == null || repositories.isEmpty())
            throw new IllegalArgumentException("Required parameter repositories is missing from configuration.yaml");
        repositories.forEach((name, settings) -> settings.validateAndProcess(name));
        if (openagencyProfileUrl == null || openagencyProfileUrl.isEmpty())
            throw new IllegalArgumentException("Required parameter openAgencyProfileUrl is missing from configuration.yaml");
        generateRepositoryByAlias();
        generateDefaultNamespacesInverse();
        if (jCache == null)
            throw new IllegalArgumentException("Required parameter jCache is missing from configuration.yaml");
    }

    private void generateRepositoryByAlias() {
        repositoryByAlias = new HashMap<>();
        repositories.forEach((name, settings) -> {
            settings.getAliases().forEach(alias -> {
                repositoryByAlias.put(alias, settings);
            });
        });
    }

    private void generateDefaultNamespacesInverse() {
        defaultNamespacesInverse = new HashMap<>();
        defaultNamespaces.forEach((prefix, namespace) -> {
            if (defaultNamespacesInverse.put(namespace, prefix) != null)
                throw new IllegalArgumentException("defaultNamespace: " + namespace + " is set for more than one prefix");
        });
    }

    @Override
    public String toString() {
        return "Settings{" + "defaultRepository=" + defaultRepository + ", badgerfishRulesLocation=" + badgerfishRulesLocation + ", solrRulesLocation=" + solrRulesLocation + ", jCache=" + jCache + ", defaultNamespaces=" + defaultNamespaces + ", xForwardedFor=" + xForwardedFor + ", httpClient=" + httpClient + ", UserMessages=" + UserMessages + ", userAgent=" + userAgent + ", openagencyProfileUrl=" + openagencyProfileUrl + ", repositories=" + repositories + '}';
    }

}
