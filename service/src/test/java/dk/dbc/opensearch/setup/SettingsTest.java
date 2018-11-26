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
package dk.dbc.opensearch.setup;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dk.dbc.opensearch.setup.yaml.EnvExpander;
import dk.dbc.opensearch.setup.yaml.ExpandingDeserializer;
import java.util.HashMap;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class SettingsTest {

    public SettingsTest() {
    }

    @Test(timeout = 2_000L)
    public void loadSettings() throws Exception {
        System.out.println("loadSettings");

        HashMap<String, String> env = mapOf("DEFAULT_REPOSITORY=foo",
                                            "X_FORWARDED_FOR=",
                                            "OPEN_AGENCY_URL=http://localhost/oa",
                                            "COREPO_SOLR_URL=http://localhost/co/so");

        YAMLMapper mapper = ExpandingDeserializer.objectMapperOf(new EnvExpander() {
            @Override
            protected String resolveVariable(String variable) {
                String value = env.get(variable);
                if (value == null)
                    throw new IllegalArgumentException("Environment variable unset: " + variable);
                return value;
            }
        }, new YAMLMapper());

        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        mapper.setPropertyNamingStrategy(new Config.DashedPropertyNamingStrategy());
        Settings configuration = mapper.readValue(getClass().getClassLoader().getResourceAsStream("settings.yaml"),
                                                  Settings.class);
        configuration.validateAndProcess();
        System.out.println("OK");
    }

    private HashMap<String, String> mapOf(String... keyValues) {
        HashMap<String, String> map = new HashMap<>();
        for (String keyValue : keyValues) {
            String[] parts = keyValue.split("=", 2);
            map.put(parts[0], parts[1]);
        }
        return map;
    }
}
