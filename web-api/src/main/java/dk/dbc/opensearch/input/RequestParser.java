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
package dk.dbc.opensearch.input;

import java.util.UUID;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public abstract class RequestParser {

    private final BaseRequest request;

    protected RequestParser(BaseRequest request, OutputType outputType) {
        this.request = request;
        setDefaultOutputType(outputType);
        setDefaultTrackingId();
    }

    public boolean isGetObjectRequest() {
        return request != null && request instanceof GetObjectRequest;
    }

    public GetObjectRequest asGetObjectRequest() {
        return (GetObjectRequest) request;
    }

    public boolean isInfoRequest() {
        return request != null && request instanceof InfoRequest;
    }

    public InfoRequest asInfoRequest() {
        return (InfoRequest) request;
    }

    public boolean isSearchRequest() {
        return request != null && request instanceof SearchRequest;
    }

    public SearchRequest asSearchRequest() {
        return (SearchRequest) request;
    }

    public CommonRequest asCommonRequest() {
        return (CommonRequest) request;
    }

    public BaseRequest asBaseRequest() {
        return request;
    }

    private void setDefaultOutputType(OutputType type) {
        OutputType outputType = request.getOutputType();
        if (outputType == null) {
            request.setOutputType(type);
        }
    }

    private void setDefaultTrackingId() {
        String trackingId = request.getTrackingId();
        if (trackingId == null) {
            request.setTrackingId(UUID.randomUUID().toString());
        }
    }
}
