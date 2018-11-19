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
package dk.dbc.opensearch.output.badgerfish;

import com.fasterxml.jackson.core.JsonGenerator;
import java.util.Map;

/**
 * Context for BadgerFish JSON, all needed fields to carry round for building
 * JSON
 *
 * @author DBC {@literal <dbc.dk>}
 */
class Context {

    final JsonGenerator out;
    final BadgerFishStack stack;
    final BadgerFishSingle single;
    final BadgerFishNamespace ns;

    Context(JsonGenerator out, BadgerFishStack stack, BadgerFishSingle single, Map<String, String> defaultNsMapping) {
        this.out = out;
        this.stack = stack;
        this.single = single;
        this.ns = new BadgerFishNamespace(out, defaultNsMapping);
    }

}
