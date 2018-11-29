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
package dk.dbc.opensearch.repository;

import java.util.List;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public interface RecordContent {

    /**
     * For user output
     *
     * @return date string "yyyy-mm-dd" for record creation
     */
    String getCreationDate();

    /**
     * Given a format supply a XMLEventReader that contains the format
     * <p>
     * This is only for the formats from the content-service directly
     *
     * @param format name of format
     * @return XMLEventReader type content stream
     */
    XMLScope getRawFormat(String format);

    /**
     * Given a format supply a XMLEventReader that contains the format
     * <p>
     * This is only for the formats from openFormat service
     *
     * @param format name of format
     * @return XMLEventReader type content stream
     */
    XMLScope getFormattedRecord(String format);

    List<String> getFormatsAvailable();

    /**
     * List of format names for formats available in
     * {@link #getRawFormat(java.lang.String)}
     *
     * @return List of names
     */
    List<String> getObjectsAvailable();

    /**
     * Get the primary object identifier for a work, for user output
     *
     * @return identifier
     */
    String getPrimaryObjectIdentifier();

    /**
     * Get liveliness of record
     *
     * @return string containing "active" or "deleted"
     */
    String getRecordStatus();

}
