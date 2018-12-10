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
package dk.dbc.opensearch.setup.yaml;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class EnvExpanderTest {

    @Test(timeout = 2_000L)
    public void testLong() throws Exception {
        System.out.println("testLong");

        InputStream is = getClass().getClassLoader().getResourceAsStream("env-expander-test.yaml");
        YAMLMapper yamlMapper = makeMapper("LONG_BAD=1",
                                           "SHORT=21",
                                           "LONG=12");

        Configuration config = yamlMapper.readValue(is, Configuration.class);

        assertThat(config.longMap, hasEntry("e", null));
        assertThat(config.longMap, hasEntry("n", null));
        assertThat(config.longMap, hasEntry("literal", 42L));
        assertThat(config.longMap, hasEntry("bad", 331L));
        assertThat(config.longMap, hasEntry("short", 21L));
        assertThat(config.longMap, hasEntry("long", 12L));
        assertThat(config.longMap, hasEntry("str", 21L));

        assertThat(config.longValue, notNullValue());
        assertThat(config.longValue, is(0L));
        assertThat(config.longValueObject, nullValue());

        assertThat(config.bn0, nullValue());
        assertThat(config.bn1, nullValue());
        assertThat(config.bt0, is(true));
        assertThat(config.bt1, is(true));
        assertThat(config.bt2, is(true));
        assertThat(config.bf0, is(false));
        assertThat(config.bf1, is(false));
        assertThat(config.bf2, is(false));
        assertThat(config.bf3, is(false));
        assertThat(config.bf4, is(false));
    }

    @Test(timeout = 2_000L, expected = Exception.class)
    public void testError() throws Exception {
        System.out.println("testError");

        InputStream is = getClass().getClassLoader().getResourceAsStream("env-expander-test.yaml");
        YAMLMapper yamlMapper = makeMapper(//"LONG_BAD=1",
                "SHORT=21",
                "LONG=12");
        yamlMapper.readValue(is, Configuration.class);
    }

    private YAMLMapper makeMapper(String... environment) {
        HashMap<String, String> env = new HashMap();
        for (String string : environment) {
            String[] part = string.split("=", 2);
            env.put(part[0], part[1]);
        }

        YAMLMapper yamlMapper = ExpandingDeserializer.objectMapperOf(new EnvExpander() {
            @Override
            protected String resolveVariable(String variable) {
                String value = env.get(variable);
                if (value == null)
                    throw new IllegalArgumentException("Environment variable unset: " + variable);
                return value;
            }
        }, new YAMLMapper());
        return yamlMapper;
    }

    public static class Configuration {

        public Map<String, Long> longMap;
        public long longValue;
        public Long longValueObject;

        public Boolean bn0;
        public Boolean bn1;
        public Boolean bt0;
        public Boolean bt1;
        public Boolean bt2;
        public Boolean bf0;
        public Boolean bf1;
        public Boolean bf2;
        public boolean bf3;
        public boolean bf4;

    }
}
