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
import dk.dbc.opensearch.input.badgerfish.BadgerFishReader;
import dk.dbc.opensearch.solr.config.FieldSpec;
import dk.dbc.opensearch.solr.flatquery.FlatQueryOr;
import dk.dbc.opensearch.solr.flatquery.FlatQuerySearch;
import dk.dbc.opensearch.solr.SolrRules;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Profiles implements Serializable {

    private static final long serialVersionUID = 9097313033801884638L;

    public static Profiles from(SolrRules solrRules, String json) {
        try (ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            return Profiles.from(solrRules, is);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot parse open agency response", ex);
        }
    }

    public static Profiles from(SolrRules solrRules, InputStream is) {
        try {
            OAProfileResponse response = BadgerFishReader.O.readValue(is, OAProfileResponse.class);
            return new Profiles(solrRules, response);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot parse open agency response", ex);
        }
    }

    // This probably shouldn't be hardcoded
    private static final String COLLECTION_IDENTIFIER = "rec.collectionIdentifier";
    private static final CQLException.Position PROFILE_EXCEPTION_LOCATION = new CQLException.Position("FROM PROFILE", 0);

    transient private final Map<String, Profile> profiles;
    private final FieldSpec collectionIdentifierSpec;
    private final String collectionIdentifierIndex;
    private final Map<String, Entry> profileSpecs;

    private Profiles(SolrRules solrRules, OAProfileResponse response) {
        // The field spec
        this.collectionIdentifierSpec = solrRules.fieldSpec(COLLECTION_IDENTIFIER, PROFILE_EXCEPTION_LOCATION);
        if (collectionIdentifierSpec == null) {
            throw new IllegalStateException("Field: " + COLLECTION_IDENTIFIER + " is undefined in solr-rules");
        }
        // the actual name of the field in SolR
        this.collectionIdentifierIndex = solrRules.indexName(COLLECTION_IDENTIFIER, PROFILE_EXCEPTION_LOCATION);
        this.profileSpecs = response.openSearchProfileResponse.profile
                .stream()
                .collect(Collectors.toMap(profile -> profile.profileName,
                                          this::makeProfileEntry));
        this.profiles = new HashMap<>();
    }

    /**
     * Given a profile object make an entry
     *
     * @param profile OpenAgency profile spec
     * @return {@link Entry}
     */
    private Entry makeProfileEntry(OAProfile profile) {
        Set<String> searchCollectionIdentifiers = profile.source.stream()
                .filter(s -> s.sourceSearchable)
                .map(s -> s.sourceIdentifier)
                .collect(Collectors.toSet());
        Map<String, Set<String>> allowedRelations = profile.source.stream()
                .filter(s -> s.relation != null && !s.relation.isEmpty())
                .collect(Collectors.toMap(s -> s.sourceIdentifier,
                                          s -> s.relation.stream()
                                                  .filter(r -> r.rdfLabel != null)
                                                  .map(r -> r.rdfLabel)
                                                  .collect(Collectors.toSet())));
        Set<String> relationCollectionIdentifiers = Stream
                .concat(searchCollectionIdentifiers.stream(),
                        allowedRelations.keySet().stream())
                .collect(Collectors.toSet());

        return new Entry(searchCollectionIdentifiers, relationCollectionIdentifiers, allowedRelations);
    }

    /**
     * Given a list of profile names, compute a profile for this combination
     *
     * @param names list of profile names
     * @return the profile covering all specs
     */
    public Profile getProfile(List<String> names) {
        String name = names.stream().sorted().collect(Collectors.joining(" "));
        return profiles.computeIfAbsent(name, s -> makeProfile(names));
    }

    /**
     * Map a list of names into a profile object
     *
     * @param names names of profiles to join
     * @return combined profile
     */
    private Profile makeProfile(List<String> names) {
        List<Entry> profileEntries = names.stream()
                .map(this::fetchProfileSpec)
                .collect(Collectors.toList());

        FlatQueryOr searchFilterQuery = flatQueryFromEntriesFor(
                profileEntries,
                Entry::getSearchCollectionIdentifiers);
        FlatQueryOr relationFilterQuery = flatQueryFromEntriesFor(
                profileEntries,
                Entry::getRelationCollectionIdentifiers);

        Map<String, Set<String>> allRelations = new HashMap<>();
        profileEntries.forEach(entry -> {
            entry.getAllowedRelations().forEach((collection, relations) -> {
                allRelations.computeIfAbsent(collection, c -> new HashSet<>())
                        .addAll(relations);
            });
        });

        return new Profile(searchFilterQuery, relationFilterQuery, allRelations);
    }

    /**
     * Construct a flatquery or'ing all collection identifiers
     *
     * @param profileEntries all the entries that are in this
     * @param selector       accessor for the entry object
     * @return an or node with all collection identifiers
     */
    private FlatQueryOr flatQueryFromEntriesFor(List<Entry> profileEntries, Function<Entry, Set<String>> selector) {
        FlatQueryOr flatQuery = new FlatQueryOr();
        profileEntries.stream()
                .map(selector) // Extract the right set of collectionIdentifiers
                .flatMap(Set::stream)
                .collect(Collectors.toSet()).stream() //UNIQ
                .map(collectionIdentifier -> new FlatQuerySearch(
                        PROFILE_EXCEPTION_LOCATION, collectionIdentifierIndex,
                        collectionIdentifier, collectionIdentifierSpec))
                .forEach(flatQuery::addOr);
        return flatQuery;
    }

    /**
     * Get profile efntry or Fail if profile is unknown
     *
     * @param name name of profile
     * @return entry
     */
    private Entry fetchProfileSpec(String name) {
        Entry entry = profileSpecs.get(name);
        if (entry == null)
            throw new IllegalArgumentException("Unknown profile: " + name);
        return entry;
    }

    private static class Entry implements Serializable {

        private static final long serialVersionUID = -4130851548622871707L;

        private final Set<String> searchCollectionIdentifiers;
        private final Set<String> relationCollectionIdentifiers;
        private final Map<String, Set<String>> allowedRelations;

        /**
         * Entry
         *
         * @param searchCollectionIdentifiers   Collections that are allowed for
         *                                      simple search
         * @param relationCollectionIdentifiers Collections that are allowed
         *                                      when requesting relations
         * @param allowedRelations              which relations that are allowed
         *                                      for a given collection
         */
        public Entry(Set<String> searchCollectionIdentifiers, Set<String> relationCollectionIdentifiers, Map<String, Set<String>> allowedRelations) {
            this.searchCollectionIdentifiers = searchCollectionIdentifiers;
            this.relationCollectionIdentifiers = relationCollectionIdentifiers;
            this.allowedRelations = allowedRelations;
        }

        public Map<String, Set<String>> getAllowedRelations() {
            return allowedRelations;
        }

        public Set<String> getRelationCollectionIdentifiers() {
            return relationCollectionIdentifiers;
        }

        public Set<String> getSearchCollectionIdentifiers() {
            return searchCollectionIdentifiers;
        }

        @Override
        public String toString() {
            return "{" + "search=" + searchCollectionIdentifiers + ", relation=" + relationCollectionIdentifiers + ", allowed=" + allowedRelations + '}';
        }
    }

}
