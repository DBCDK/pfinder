/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-web-api
 *
 * opensearch-web-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-web-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.opensearch.input;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class RequestParserJSON extends RequestParser {

    private static final ObjectMapper O = makeObjectMapper();

    public RequestParserJSON(InputStream is) throws XMLStreamException {
        super(readRequest(is));
    }

    /**
     * Read a JSON request and validate it
     *
     * @param is input stream containing JSON
     * @return base request (geObject, info or search)
     * @throws XMLStreamException If request is invalid
     */
    private static BaseRequest readRequest(InputStream is) throws XMLStreamException {
        try {
            Base base = O.readValue(is, Base.class);
            BaseRequest baseRequest = base.asBaseRequest();
            baseRequest.validate(JSON_LOCATION);
            return baseRequest;
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    /**
     * Mapper that is case insensitive about enums and arrays vs single values
     *
     * @return mapper
     */
    private static ObjectMapper makeObjectMapper() {
        return new ObjectMapper()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .registerModule(new SimpleModule()
                        .addDeserializer(CollectionType.class, new StdDeserializer<CollectionType>(CollectionType.class) {
                                     private static final long serialVersionUID = 7173963836276661255L;

                                     @Override
                                     public CollectionType deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
                                         return CollectionType.from(parser.getText());
                                     }
                                 })
                        .addDeserializer(FacetSortType.class, new StdDeserializer<FacetSortType>(FacetSortType.class) {
                                     private static final long serialVersionUID = 9159680166757354134L;

                                     @Override
                                     public FacetSortType deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
                                         return FacetSortType.from(parser.getText());
                                     }
                                 })
                        .addDeserializer(OutputType.class, new StdDeserializer<OutputType>(OutputType.class) {
                                     private static final long serialVersionUID = 5056319345982661134L;

                                     @Override
                                     public OutputType deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
                                         return OutputType.from(parser.getText());
                                     }
                                 })
                        .addDeserializer(RelationDataType.class, new StdDeserializer<RelationDataType>(RelationDataType.class) {
                                     private static final long serialVersionUID = -4334128694483252864L;

                                     @Override
                                     public RelationDataType deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
                                         return RelationDataType.from(parser.getText());
                                     }
                                 }));
    }

    /**
     * Unknown location for validation error
     */
    private static final Location JSON_LOCATION = new Location() {
        @Override
        public int getLineNumber() {
            return 0;
        }

        @Override
        public int getColumnNumber() {
            return 0;
        }

        @Override
        public int getCharacterOffset() {
            return 0;
        }

        @Override
        public String getPublicId() {
            return "";
        }

        @Override
        public String getSystemId() {
            return "";
        }
    };

    /**
     * Class representing a JSON reques with a single field names getObject,
     * info or search
     */
    public static class Base {

        private GetObjectRequest getObject;
        private InfoRequest info;
        private SearchRequest search;

        public GetObjectRequest getGetObject() {
            return getObject;
        }

        public void setGetObject(GetObjectRequest getObjectRequest) {
            this.getObject = getObjectRequest;
        }

        public InfoRequest getInfo() {
            return info;
        }

        public void setInfo(InfoRequest infoRequest) {
            this.info = infoRequest;
        }

        public SearchRequest getSearch() {
            return search;
        }

        public void setSearch(SearchRequest searchRequest) {
            this.search = searchRequest;
        }

        public BaseRequest asBaseRequest() throws XMLStreamException {
            if (getObject != null)
                return getObject;
            if (info != null)
                return info;
            if (search != null)
                return search;
            throw new XMLStreamException("No JSON request defined");
        }
    }
}
