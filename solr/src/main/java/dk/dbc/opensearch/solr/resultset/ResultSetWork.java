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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class ResultSetWork extends ResultSet {

    private static final long serialVersionUID = -4788723847583007565L;

    private static final Logger log = LoggerFactory.getLogger(ResultSetWork.class);

    public ResultSetWork(SolrQueryFields solrQuery, boolean allObjects) {
        super(solrQuery, allObjects);
    }

    @Override
    protected String nameOfWorkField() {
        return WORK_ID;
    }

    @Override
    protected String nameOfUnitField() {
        return UNIT_ID;
    }

    @Override
    protected String nameOfManifestationField() {
        return MANIFESTATION_ID;
    }

}