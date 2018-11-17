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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import static dk.dbc.opensearch.input.RequestHelpers.*;
import static java.util.Collections.EMPTY_LIST;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Facets implements InputPart {

    public static final InputPartFactory<Facets> FACTORY =
            new InputPartFactory<>(Facets::new)
                    .with("numberOfTerms", obj -> obj::putNumberOfTerms)
                    .with("facetSort", obj -> obj::putFacetSort)
                    .with("facetMinCount", obj -> obj::putFacetMinCount)
                    .with("facetName", obj -> obj::addFacetName)
                    .with("facetOffset", obj -> obj::putFacetOffset);

    private static final Function<String, FacetSortType> FACET_SORT_TYPES = mapTo(makeTrimOneOf("facetSort", "count", "index"),
                                                                                  FacetSortType::from);

    private Integer numberOfTerms = null;
    private FacetSortType facetSort = null;
    private Integer facetMinCount = null;
    private List<String> facetName = null;
    private Integer facetOffset = null;

    public Facets() {
    }

    @Override
    public void validate(Location location) {
    }

    public void putFacetMinCount(String content, Location location) throws XMLStreamException {
        facetMinCount = get("facetMinCount", facetMinCount, content, location,
                            s -> Integer.parseUnsignedInt(trimNotEmpty(s)));
    }

    public Integer getFacetMinCount() {
        return facetMinCount;
    }

    public void setFacetMinCount(Integer facetMinCount) {
        this.facetMinCount = facetMinCount;
    }

    public void addFacetName(String content, Location location) throws XMLStreamException {
        if (facetName == null)
            facetName = new ArrayList<>();
        facetName.add(get("facetName", content, location, s -> trimNotEmptyOneWord(content)));
    }

    public List<String> getFacetNamesOrDefault() {
        return facetName == null ? EMPTY_LIST : facetName;
    }

    public List<String> getFacetNames() {
        return facetName;
    }

    public void setFacetName(List<String> facetName) {
        this.facetName = facetName;
    }

    public void putFacetOffset(String content, Location location) throws XMLStreamException {
        facetOffset = get("facetOffset", facetOffset, content, location,
                          s -> Integer.parseUnsignedInt(trimNotEmpty(s)));
    }

    public Integer getFacetOffset() {
        return facetOffset;
    }

    public void setFacetOffset(Integer facetOffset) {
        this.facetOffset = facetOffset;
    }

    public void putFacetSort(String content, Location location) throws XMLStreamException {
        facetSort = get("facetSort", facetSort, content, location,
                        FACET_SORT_TYPES);
    }

    public FacetSortType getFacetSort() {
        return facetSort;
    }

    public void setFacetSort(FacetSortType facetSort) {
        this.facetSort = facetSort;
    }

    public void putNumberOfTerms(String content, Location location) throws XMLStreamException {
        numberOfTerms = get("numberOfTerms", numberOfTerms, content, location,
                            s -> Integer.parseUnsignedInt(trimNotEmpty(s)));
    }

    public Integer getNumberOfTerms() {
        return numberOfTerms;
    }

    public void setNumberOfTerms(Integer numberOfTerms) {
        this.numberOfTerms = numberOfTerms;
    }

    @Override
    public String toString() {
        return "Facets{" + "numberOfTerms=" + numberOfTerms + ", facetSort=" + facetSort + ", facetMinCount=" + facetMinCount + ", facetName=" + facetName + ", facetOffset=" + facetOffset + '}';
    }

}
