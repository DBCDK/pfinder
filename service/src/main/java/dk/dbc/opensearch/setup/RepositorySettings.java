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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dk.dbc.opensearch.solr.Solr;
import dk.dbc.opensearch.solr.SolrRules;
import dk.dbc.opensearch.solr.config.SolrConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.ejb.EJBException;
import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dbc.opensearch.setup.Config.openInputStream;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class RepositorySettings {

    private static final Logger log = LoggerFactory.getLogger(RepositorySettings.class);

    private static final ConcurrentHashMap<String, SolrClient> SOLR_CLIENTS = new ConcurrentHashMap<>();

    private Set<String> aliases;
    private String solrUrl;
    private String solrRulesLocation;

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public String getSolrUrl() {
        return solrUrl;
    }

    public void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
    }

    public SolrClient solrClient() {
        return SOLR_CLIENTS.get(solrUrl);
    }

    public String getSolrRulesLocation() {
        return solrRulesLocation;
    }

    public void setSolrRulesLocation(String solrRulesLocation) {
        this.solrRulesLocation = solrRulesLocation;
    }

    /*
     *      _____________   ____________  ___  ________________
     *     / ____/ ____/ | / / ____/ __ \/   |/_  __/ ____/ __ \
     *    / / __/ __/ /  |/ / __/ / /_/ / /| | / / / __/ / / / /
     *   / /_/ / /___/ /|  / /___/ _, _/ ___ |/ / / /___/ /_/ /
     *   \____/_____/_/ |_/_____/_/ |_/_/  |_/_/ /_____/_____/
     *
     */
    private SolrRules solrRules;
    private String name;

    public SolrRules getSolrRules() {
        return solrRules;
    }

    public String getName() {
        return name;
    }

    void validateAndProcess(String name) {
        this.name = name;
        if (solrUrl == null || solrUrl.isEmpty())
            throw new IllegalArgumentException("Required parameter solrUrl is missing from configuration.yaml (" + name + ")");
        if (aliases == null || aliases.isEmpty())
            throw new IllegalArgumentException("Required parameter aliases is missing from configuration.yaml (" + name + ")");
        if (solrRulesLocation == null || solrRulesLocation.isEmpty())
            throw new IllegalArgumentException("Required parameter solrRules is missing from configuration.yaml (" + name + ")");

        SOLR_CLIENTS.computeIfAbsent(solrUrl, Solr::client);

        this.solrRules = makeSolrRules(solrRulesLocation, "classpath:solr-rules-" + name + ".yaml");
    }

    private SolrRules makeSolrRules(String... paths) {
        try (InputStream is = openInputStream(paths)) {
            ObjectMapper o = new YAMLMapper();
            SolrConfig config = o.readValue(is, SolrConfig.class);
            return config.makeSolrRules();
        } catch (IOException ex) {
            log.error("Error loading solr-rules file: {}", ex.getMessage());
            log.debug("Error loading solr-rules file: ", ex);
            throw new EJBException("Error loading solr-rules file: ");
        }
    }

}
