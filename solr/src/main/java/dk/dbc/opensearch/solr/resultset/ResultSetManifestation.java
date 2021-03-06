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
package dk.dbc.opensearch.solr.resultset;

import dk.dbc.opensearch.solr.SolrQueryFields;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * This is a special case where a work is generated for each unit
 * <p>
 * This is achieved by using the unit is as work id too.
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class ResultSetManifestation extends ResultSet {

    private static final long serialVersionUID = 5462049649430313069L;

    private static final Collection<String> REQUIRED_FIELDS =
            Collections.unmodifiableCollection(
                    Arrays.asList(ID, UNIT_ID, MANIFESTATION_ID));

    public ResultSetManifestation(SolrQueryFields solrQuery, boolean allObjects, boolean queryDebug) {
        super(solrQuery, allObjects, queryDebug);
    }

    @Override
    protected String nameOfIdField() {
        return ID;
    }

    @Override
    protected String nameOfWorkField() {
        return UNIT_ID;
    }

    @Override
    protected String nameOfUnitField() {
        return UNIT_ID;
    }

    @Override
    protected String nameOfManifestationField() {
        return MANIFESTATION_ID;
    }

    @Override
    protected Collection<String> namesOfFieldsRequired() {
        return REQUIRED_FIELDS;
    }

}
