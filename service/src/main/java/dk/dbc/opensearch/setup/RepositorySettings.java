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
import dk.dbc.opensearch.repository.RepositoryAbstraction;
import dk.dbc.opensearch.repository.CorepoRepositoryAbstraction;
import dk.dbc.opensearch.solr.SolrRules;
import dk.dbc.opensearch.solr.config.SolrConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJBException;
import javax.xml.namespace.QName;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dbc.opensearch.setup.Config.openInputStream;
import static java.util.Collections.EMPTY_MAP;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class RepositorySettings {

    private static final Logger log = LoggerFactory.getLogger(RepositorySettings.class);

    private Set<String> aliases;
    private String contentServiceUrl;
    private String solrUrl;
    private String solrRulesLocation;
    private Map<String, String> rawFormats;
    private Map<String, Object> solrFormats;

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public String getContentServiceUrl() {
        return contentServiceUrl;
    }

    public void setContentServiceUrl(String contentServiceUrl) {
        this.contentServiceUrl = contentServiceUrl;
    }

    public Map<String, String> getRawFormats() {
        return rawFormats;
    }

    public Map<String, String> getRawFormatsOrDefault() {
        return rawFormats == null ? EMPTY_MAP : rawFormats;
    }

    public void setRawFormats(Map<String, String> rawFormats) {
        this.rawFormats = rawFormats;
    }

    public Map<String, Object> getSolrFormats() {
        return solrFormats;
    }

    public Map<String, Object> getSolrFormatsOrDefault() {
        return solrFormats == null ? EMPTY_MAP : solrFormats;
    }

    public void setSolrFormats(Map<String, Object> solrFormats) {
        this.solrFormats = solrFormats;
    }

    public String getSolrUrl() {
        return solrUrl;
    }

    public void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
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
    private HashMap<QName, String> knownFormatsProcessed;
    private RepositoryAbstraction repositoryAbstraction;

    public SolrRules getSolrRules() {
        return solrRules;
    }

    public String getName() {
        return name;
    }

    public Map<QName, String> getKnownFormatsByQName() {
        return knownFormatsProcessed;
    }

    public RepositoryAbstraction abstraction() {
        return repositoryAbstraction;
    }

    void validateAndProcess(Settings settings, String name) {
        this.name = name;
        if (aliases == null || aliases.isEmpty())
            throw new IllegalArgumentException("Required parameter aliases is missing from configuration.yaml (" + name + ")");
        if (contentServiceUrl == null || contentServiceUrl.isEmpty())
            throw new IllegalArgumentException("Required parameter contentServiceUrl is missing from configuration.yaml (" + name + ")");
        if (solrUrl == null || solrUrl.isEmpty())
            throw new IllegalArgumentException("Required parameter solrUrl is missing from configuration.yaml (" + name + ")");
        if (solrRulesLocation == null || solrRulesLocation.isEmpty())
            throw new IllegalArgumentException("Required parameter solrRules is missing from configuration.yaml (" + name + ")");

        this.solrRules = makeSolrRules(solrRulesLocation, "classpath:solr-rules-" + name + ".yaml");

        knownFormatsProcessed = new HashMap<>();
        if (rawFormats != null) {
            rawFormats.forEach((formatName, formatRootElement) -> {
                int i = formatRootElement.lastIndexOf(' ');
                QName qname = new QName(formatRootElement.substring(0, i), formatRootElement.substring(i + 1));
                knownFormatsProcessed.put(qname, formatName);
            });
        }

        try {
            repositoryAbstraction = new CorepoRepositoryAbstraction(settings.getDefaultPrefix(), this);
        } catch (SolrServerException | IOException ex) {
            log.error("Error creating repositoryAbstraction: {}", ex.getMessage());
            throw new EJBException("Error creating repositoryAbstraction: ", ex);
        }
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
