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

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class RecordKey implements Serializable {

    private static final long serialVersionUID = 1960180554493359233L;

    private final String repository;
    private final int showAgencyId;
    private final Set<String> manifestations;

    public RecordKey(String repository, int showAgencyId, Set<String> manifestations) {
        this.repository = repository;
        this.showAgencyId = showAgencyId;
        this.manifestations = manifestations;
    }

    public String getRepository() {
        return repository;
    }

    public int getShowAgencyId() {
        return showAgencyId;
    }

    public Set<String> getManifestations() {
        return manifestations;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.repository);
        hash = 59 * hash + Objects.hashCode(this.showAgencyId);
        hash = 59 * hash + Objects.hashCode(this.manifestations);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final RecordKey other = (RecordKey) obj;
        return Objects.equals(this.repository, other.repository) &&
               Objects.equals(this.showAgencyId, other.showAgencyId) &&
               Objects.equals(this.manifestations, other.manifestations);
    }

    @Override
    public String toString() {
        return "RecordKey{" + repository + "/" + showAgencyId + manifestations + '}';
    }
}
