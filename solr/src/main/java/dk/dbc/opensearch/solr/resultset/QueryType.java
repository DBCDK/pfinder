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

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public enum QueryType {
    BUILD_WORK("build work", "solrWorkBuild"),
    EXPAND_WORK("expand work", "solrWorkExpand");

    private final String logName;
    private final String timingName;

    private QueryType(String logName, String timingName) {
        this.logName = logName;
        this.timingName = timingName;
    }

    public String getTimingName() {
        return timingName;
    }

    public String getLogName() {
        return logName;
    }
}
