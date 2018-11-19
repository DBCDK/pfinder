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

import dk.dbc.opensearch.tools.UserException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Settings {

    private String defaultRepository;
    private String badgerfishRulesLocation;
    private Map<String, Set<String>> repositoryNames;
    private Map<String, String> jCache;
    private Map<String, String> defaultNamespaces;
    private String xForwardedFor;

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

    public Map<String, Set<String>> getRepositoryNames() {
        return repositoryNames;
    }

    public void setRepositoryNames(Map<String, Set<String>> repositoryNames) {
        this.repositoryNames = repositoryNames;
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

    /*
     *      _____________   ____________  ___  ________________
     *     / ____/ ____/ | / / ____/ __ \/   |/_  __/ ____/ __ \
     *    / / __/ __/ /  |/ / __/ / /_/ / /| | / / / __/ / / / /
     *   / /_/ / /___/ /|  / /___/ _, _/ ___ |/ / / /___/ /_/ /
     *   \____/_____/_/ |_/_____/_/ |_/_/  |_/_/ /_____/_____/
     *
     */
    private Map<String, String> defaultNamespacesInverse;
    private Map<String, String> repositoryNamesInverse;

    /**
     * Given a repo name (from the request) get the configured repo name
     *
     * @param symbolicRepoName repo alias
     * @return real name
     * @throws UserException if repo is unknown
     */
    public String lookupRealRepoName(String symbolicRepoName) throws UserException {
        String realRepoName = repositoryNamesInverse.get(symbolicRepoName);
        if (realRepoName == null)
            throw new UserException("Unknown repository: " + symbolicRepoName);
        return realRepoName;
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
            prefix = generatedNamespaces.computeIfAbsent(prefix,
                                                         p -> "ns" + ( generatedNamespaces.size() + 1 ));
        }
        return prefix;
    }

    void validateAndProcess() {
        defaultRepository = defaultRepository.trim();
        if (defaultRepository == null || defaultRepository.isEmpty())
            throw new IllegalArgumentException("Required parameter defaultRepository is missing from configuration.yaml");
        if (repositoryNames == null || repositoryNames.isEmpty())
            throw new IllegalArgumentException("Required parameter repositoryNames is missing from configuration.yaml");
        generateRepositoryNamesInverse();
        generateDefaultNamespacesInverse();
        if (jCache == null)
            throw new IllegalArgumentException("Required parameter jCache is missing from configuration.yaml");
    }

    private void generateRepositoryNamesInverse() {
        repositoryNamesInverse = new HashMap<>();
        repositoryNames.forEach((realRepo, set) -> {
            set.forEach(repoName -> {
                if (repositoryNamesInverse.put(repoName, realRepo) != null) {
                    throw new IllegalArgumentException("repository name: " + repoName + " is set for more than one repo");
                }
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
        return "Configuration{" + "defaultRepository=" + defaultRepository + ",\n badgerfishRulesLocation=" + badgerfishRulesLocation + ",\n repositoryNames=" + repositoryNames + ",\n jCache=" + jCache + ",\n defaultNamespaces=" + defaultNamespaces + '}';
    }

}