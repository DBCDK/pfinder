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
package dk.dbc.opensearch.solr;

import dk.dbc.opensearch.solr.config.FieldSpec;
import dk.dbc.opensearch.cql.CQLException.Position;
import java.io.Serializable;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public interface SolrRules extends Serializable {

    String indexName(String index, Position pos);

    FieldSpec fieldSpec(String index, Position pos);
}
