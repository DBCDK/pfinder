/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-solr
 *
 * opensearch-solr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-solr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.solr.profile;

import dk.dbc.opensearch.cql.CQLException;
import dk.dbc.opensearch.solr.config.FieldSpec;
import dk.dbc.opensearch.solr.flatquery.FlatQueryOr;
import dk.dbc.opensearch.solr.flatquery.FlatQuerySearch;
import dk.dbc.opensearch.solr.SolrRules;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Profiles {

    private static final String COLLECTION_IDENTIFIER = "rec.collectionIdentifier";

    public static Profiles from(SolrRules solrRules, String json) {
        try (ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            return Profiles.from(solrRules, is);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot parse open agency response", ex);
        }
    }

    public static Profiles from(SolrRules solrRules, InputStream is) {
        try {
            OAProfileResponse response = BadgerFish.O.readValue(is, OAProfileResponse.class);
            return Profiles.from(solrRules, response);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot parse open agency response", ex);
        }
    }

    public static Profiles from(SolrRules solrRules, OAProfileResponse base) {
        FieldSpec fieldSpec = solrRules.fieldSpec(COLLECTION_IDENTIFIER, PROFILE_POSITION);
        if(fieldSpec == null) {
            throw new IllegalStateException("Field: " + COLLECTION_IDENTIFIER + " is undefined in solr-rules");
        }
        String index = solrRules.indexName(COLLECTION_IDENTIFIER, PROFILE_POSITION);
        HashMap<String, Profile> profiles = new HashMap<>();
        for (OAProfile profile : base.openSearchProfileResponse.profile) {
            Set<String> searchIdentifiers = new HashSet<>();
            Set<String> relationIdentifiers = new HashSet<>();
            Map<String, Set<String>> relationLabels = new HashMap<>();
            if (profile.source != null) {
                for (OASource source : profile.source) {
                    if (source.relation != null && !source.relation.isEmpty()) {
                        Set<String> labels = source.relation.stream()
                                .map(r -> r.rdfLabel)
                                .collect(Collectors.toSet());
                        if (!labels.isEmpty()) {
                            relationLabels.put(source.sourceIdentifier, labels);
                            relationIdentifiers.add(source.sourceIdentifier);
                        }
                    }
                    if (source.sourceSearchable) {
                        searchIdentifiers.add(source.sourceIdentifier);
                        relationIdentifiers.add(source.sourceIdentifier);
                    }
                }
            }

            FlatQueryOr orSearch = new FlatQueryOr();
            searchIdentifiers.stream()
                    .sorted()
                    .map(search -> new FlatQuerySearch(PROFILE_POSITION, index, search, fieldSpec))
                    .forEach(orSearch::addOr);
            FlatQueryOr orRelation = new FlatQueryOr();
            relationIdentifiers.stream()
                    .sorted()
                    .map(search -> new FlatQuerySearch(PROFILE_POSITION, index, search, fieldSpec))
                    .forEach(orRelation::addOr);
            relationLabels = Collections.unmodifiableMap(relationLabels);
            profiles.put(profile.profileName, new Profile(orSearch, orRelation, relationLabels));
        }
        return new Profiles(Collections.unmodifiableMap(profiles));
    }
    private static final CQLException.Position PROFILE_POSITION = new CQLException.Position("FROM PROFILE", 0);

    private final Map<String, Profile> profiles;

    public Profiles(Map<String, Profile> profiles) {
        this.profiles = profiles;
    }

    public Profile getProfile(String name) {
        return profiles.get(name);
    }

    @Override
    public String toString() {
        return "Profiles{" + profiles + '}';
    }

}
