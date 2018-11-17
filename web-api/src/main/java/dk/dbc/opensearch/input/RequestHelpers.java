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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
class RequestHelpers {

    static <T> T get(String name, T content, Location location) throws XMLStreamException {
        return get(name, null, content, location);
    }

    static <T> T get(String name, Object oldValue, T content, Location location) throws XMLStreamException {
        if (oldValue != null) {
            throw new XMLStreamException(name + " has already been set", location);
        }
        return content;
    }

    static <T> T get(String name, String content, Location location, Function<String, T> mapper) throws XMLStreamException {
        return get(name, null, content, location, mapper);
    }

    static <T> T get(String name, T oldValue, String content, Location location, Function<String, T> mapper) throws XMLStreamException {
        try {
            return mapper.apply(get(name, oldValue, content, location));
        } catch (RuntimeException ex) {
            throw new XMLStreamException("Invalid value for " + name + ". " + ex.getMessage(), location);
        }
    }

    static Function<String, String> makeTrimRegexMatch(String regex, String message) {
        Pattern pattern = Pattern.compile(regex);
        return s -> {
            if (pattern.matcher(s).matches()) {
                return s;
            }
            throw new IllegalArgumentException(message);
        };
    }

    static Function<String, String> makeTrimOneOf(String name, String... values) {
        HashSet<String> options = new HashSet<>(Arrays.asList(values));
        String all = Arrays.stream(Arrays.copyOfRange(values, 0, values.length - 1))
                .collect(Collectors.joining("', '"));
        String message;
        if (all.isEmpty()) {
            message = "supported value for " + name + " is: '" + values[values.length - 1] + "'";
        } else {
            message = "supported values for " + name + " are: '" + all + "' and '" + values[values.length - 1] + "'";
        }
        return (String t) -> {
            String trimmed = t.trim();
            if (options.contains(trimmed))
                return trimmed;
            throw new IllegalArgumentException(message);
        };
    }

    static <T> Function<String, T> mapTo(Function<String, String> func, Function<String, T> map) {
        return (String t) -> map.apply(func.apply(t));
    }

    static String trimNotEmpty(String content) {
        content = content.trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Value should have content");
        }
        return content;
    }

    static String trimNotEmptyOneWord(String content) {
        content = trimNotEmpty(content);
        if (content.split("\\s+").length != 1) {
            throw new IllegalArgumentException("Value should be single word content");
        }
        return content;
    }

    static String trimOneOf(String content, Collection<String> choices, String errorList) {
        content = content.trim();
        if (choices.contains(content)) {
            return content;
        }
        throw new IllegalArgumentException("Value should be one of: " + errorList);
    }

    static boolean bool(String content) {
        switch (content.toLowerCase(Locale.ROOT)) {
            case "0":
            case "no":
            case "false":
                return false;
            case "1":
            case "yes":
            case "true":
                return true;
            default:
        throw new IllegalArgumentException("Value should be one of: 0, 1, yes, no, true or false");
        }

    }

}
