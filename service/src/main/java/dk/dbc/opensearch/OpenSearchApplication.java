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
package dk.dbc.opensearch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@ApplicationPath("/")
public class OpenSearchApplication extends Application {

    private static final Set<Class<?>> BEANS = new HashSet<>(Arrays.asList(
            ServiceBean.class
    ));

    @Override
    public Set<Class<?>> getClasses() {
        return BEANS;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> props = new HashMap<>();

        props.put("jersey.config.server.disableMoxyJson", true);

        return props;
    }


}
