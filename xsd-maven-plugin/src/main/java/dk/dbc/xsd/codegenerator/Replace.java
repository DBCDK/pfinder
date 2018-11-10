/*
 * Copyright (C) 2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of opensearch-xsd-maven-plugin
 *
 * opensearch-xsd-maven-plugin is free software: you can redistribute it with/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opensearch-xsd-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.xsd.codegenerator;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class Replace {

    private final HashMap<String, String> replacement;

    public static Replace of(String key, String value) {
        return new Replace(key, value);
    }

    private Replace(String key, String value) {
        this.replacement = new HashMap<>();
        this.replacement.put("$", "$");
        this.replacement.put(key, value);
    }

    public Replace with(String key, String value) {
        this.replacement.put(key, value);
        return this;
    }

    public Map<String, String> build() {
        return replacement;
    }

}
