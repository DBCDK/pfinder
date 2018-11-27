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

import dk.dbc.opensearch.input.CollectionType;
import dk.dbc.opensearch.input.SearchRequest;
import dk.dbc.opensearch.input.UserDefinedBoost;
import dk.dbc.opensearch.input.UserDefinedRanking;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class ResultSetKey implements Serializable {

    private static final long serialVersionUID = 7388581403133101704L;

    private final Integer agencyId;
    private final Boolean allObjects;
    private final CollectionType collectionType;
    private final List<String> profiles;
    private final String query;
    private final String queryLanguage;
    private final String repository;
    private final Integer showAgencyId;
    private final List<String> sort;
    private final List<UserDefinedBoost> userDefinedBoost;
    private final List<UserDefinedRanking> userDefinedRanking;

    public static ResultSetKey of(SearchRequest req) {
        return new ResultSetKey(req);
    }

    private ResultSetKey(SearchRequest req) {
        this.agencyId = req.getAgency();
        this.allObjects = req.getAllObjectsOrDefault();
        this.collectionType = req.getCollectionTypeOrDefault();
        this.profiles = req.getProfilesOrDefault();
        this.query = req.getQuery();
        this.queryLanguage = req.getQueryLanguageOrDefault();
        this.repository = req.getRepository();
        this.showAgencyId = req.getShowAgency();
        this.sort = req.getSort();
        this.userDefinedBoost = req.getUserDefinedBoost();
        this.userDefinedRanking = req.getUserDefinedRanking();
    }

    public Integer getAgencyId() {
        return agencyId;
    }

    public Boolean getAllObjects() {
        return allObjects;
    }

    public CollectionType getCollectionType() {
        return collectionType;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public String getQuery() {
        return query;
    }

    public String getQueryLanguage() {
        return queryLanguage;
    }

    public String getRepository() {
        return repository;
    }

    public Integer getShowAgencyId() {
        return showAgencyId;
    }

    public List<String> getSort() {
        return sort;
    }

    public List<UserDefinedBoost> getUserDefinedBoost() {
        return userDefinedBoost;
    }

    public List<UserDefinedRanking> getUserDefinedRanking() {
        return userDefinedRanking;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.agencyId);
        hash = 59 * hash + Objects.hashCode(this.allObjects);
        hash = 59 * hash + Objects.hashCode(this.collectionType);
        hash = 59 * hash + Objects.hashCode(this.profiles);
        hash = 59 * hash + Objects.hashCode(this.query);
        hash = 59 * hash + Objects.hashCode(this.queryLanguage);
        hash = 59 * hash + Objects.hashCode(this.repository);
        hash = 59 * hash + Objects.hashCode(this.showAgencyId);
        hash = 59 * hash + Objects.hashCode(this.sort);
        hash = 59 * hash + Objects.hashCode(this.userDefinedBoost);
        hash = 59 * hash + Objects.hashCode(this.userDefinedRanking);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final ResultSetKey other = (ResultSetKey) obj;
        return Objects.equals(this.agencyId, other.agencyId) &&
               Objects.equals(this.allObjects, other.allObjects) &&
               Objects.equals(this.collectionType, other.collectionType) &&
               Objects.equals(this.profiles, other.profiles) &&
               Objects.equals(this.query, other.query) &&
               Objects.equals(this.queryLanguage, other.queryLanguage) &&
               Objects.equals(this.repository, other.repository) &&
               Objects.equals(this.showAgencyId, other.showAgencyId) &&
               Objects.equals(this.sort, other.sort) &&
               Objects.equals(this.userDefinedBoost, other.userDefinedBoost) &&
               Objects.equals(this.userDefinedRanking, other.userDefinedRanking);
    }

}
