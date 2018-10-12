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
package dk.dbc.opensearch.solr.config;

import java.util.Objects;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class SolrConfigField {

    public String solrName;
    public SolrConfigFieldType type;
    public String nestedGroup;
    public String filterQuery;
    public Boolean internal;

    public SolrConfigField() {
    }

    public SolrConfigField(SolrConfigField field) {
        this.solrName = "###-SYNTHETIC-###";
        this.type = firstOf(field.type);
        this.nestedGroup = firstOf(field.nestedGroup);
        this.filterQuery = firstOf(field.filterQuery);
        this.internal = firstOf(field.internal, false);
    }

    public SolrConfigField(SolrConfigField exactField, SolrConfigField prefixField) {
        this.solrName = "###-SYNTHETIC-###";
        this.type = firstOf(exactField.type, prefixField.type);
        this.nestedGroup = firstOf(exactField.nestedGroup, prefixField.nestedGroup);
        this.filterQuery = firstOf(exactField.filterQuery, prefixField.filterQuery);
        this.internal = firstOf(exactField.internal, prefixField.internal, false);
    }

    private static <T> T firstOf(T... ts) {
        for (T t : ts) {
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    public boolean isPhrase() {
        return type == SolrConfigFieldType.PHRASE;
    }

    //
    // HASH/EQUALS used for making a map from this type to a computed value
    //
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.solrName);
        hash = 59 * hash + Objects.hashCode(this.type);
        hash = 59 * hash + Objects.hashCode(this.nestedGroup);
        hash = 59 * hash + Objects.hashCode(this.filterQuery);
        hash = 59 * hash + Objects.hashCode(this.internal);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SolrConfigField other = (SolrConfigField) obj;
        return Objects.equals(this.solrName, other.solrName) &&
               Objects.equals(this.type, other.type) &&
               Objects.equals(this.nestedGroup, other.nestedGroup) &&
               Objects.equals(this.filterQuery, other.filterQuery) &&
               Objects.equals(this.internal, other.internal);
    }

    @Override
    public String toString() {
        return "SolrConfigField{" + "solrName=" + solrName + ", type=" + type + ", nestedGroup=" + nestedGroup + ", filterQuery=" + filterQuery + ", internal=" + internal + '}';
    }

}
