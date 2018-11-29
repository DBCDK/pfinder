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
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dk.dbc.opensearch.output.badgerfish.BadgerFishSingle;
import dk.dbc.opensearch.setup.yaml.EnvExpander;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@ApplicationScoped
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    @Resource(type = ManagedExecutorService.class)
    ExecutorService es;

    private BadgerFishSingle badgerFishSingle;
    private Settings config;
    private Client client;

    @PostConstruct
    public void init() {
        log.info("Creating Config");
        this.config = readConfiguration();
        this.badgerFishSingle = makeBadgerFishSingle(); // uses config
        this.client = ClientBuilder.newBuilder()
//                .connectTimeout(config.getHttpClient().connectTimeoutMS(), TimeUnit.MILLISECONDS)
//                .readTimeout(config.getHttpClient().readTimeoutMS(), TimeUnit.MILLISECONDS)
//                .executorService(es)
                .build();

    }

    @Produces
    @ApplicationScoped
    public Settings getConfiguration() {
        return config;
    }

    @Produces
    @ApplicationScoped
    public BadgerFishSingle getBadgerFishSingle() {
        return badgerFishSingle;
    }

    @Produces
    @ApplicationScoped
    public Client getClient() {
        return client;
    }

    private Settings readConfiguration() {
        try (InputStream is = openInputStream(System.getenv("CONFIG_FILE"),
                                              "classpath:settings.yaml")) {
            YAMLMapper mapper = EnvExpander.YAML_MAPPER.copy();
            mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
            mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
            mapper.setPropertyNamingStrategy(new DashedPropertyNamingStrategy());
            Settings configuration = mapper.readValue(is, Settings.class);

            configuration.validateAndProcess();

            log.debug("configuration = {}", configuration);
            return configuration;
        } catch (IOException | RuntimeException ex) {
            log.debug("Error loading configuration file: {}", ex.getMessage());
            log.debug("Error loading configuration file: ", ex);
            throw new EJBException("Error loading configuration file");
        }
    }

    private BadgerFishSingle makeBadgerFishSingle() {
        try (InputStream is = openInputStream(System.getenv("BADGERFISH_RULES_LOCATION"),
                                              config.getBadgerfishRulesLocation(),
                                              "classpath:opensearch-badgerfish-rules.yaml")) {
            BadgerFishSingle repeated = BadgerFishSingle.from(is);
            log.debug("repeated = {}", repeated);
            return repeated;
        } catch (IOException ex) {
            log.debug("Error loading badgerfish rules file: {}", ex.getMessage());
            log.debug("Error loading badgerfish rules file: ", ex);
            throw new EJBException("Error loading badgerfish rules file");
        }
    }

    static InputStream openInputStream(String... paths) throws FileNotFoundException {
        for (String path : paths) {
            if (path == null || path.isEmpty())
                continue;
            try {
                if (path.startsWith("classpath:")) {
                    String classPath = path.substring(10);
                    if (classPath.startsWith("/"))
                        classPath = classPath.substring(1);
                    InputStream is = Config.class.getClassLoader().getResourceAsStream(classPath);
                    if (is == null)
                        throw new FileNotFoundException("Cannot open classpath:" + classPath);
                    return is;
                }
                String filePath = path;
                if (filePath.startsWith("file:"))
                    filePath = filePath.substring(5);
                if (!filePath.startsWith("/"))
                    filePath = "/" + filePath;
                return new FileInputStream(filePath);
            } catch (FileNotFoundException ex) {
                log.debug("Tried: {} but couldn't open", path);
            }
        }
        throw new FileNotFoundException("Locate file from:" + Arrays.toString(paths));
    }

    public static class DashedPropertyNamingStrategy extends PropertyNamingStrategy {

        public DashedPropertyNamingStrategy() {
        }
        private static final long serialVersionUID = -4641394978853237259L;

        @Override
        public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            try {
                String name = method.getName();
                if (name.startsWith("get"))
                    name = name.substring(3);
                return name(name);
            } catch (IOException ex) {
                log.error("Error mapping YAML-name: {}", ex.getMessage());
                log.debug("Error mapping YAML-name: ", ex);
            }
            return super.nameForGetterMethod(config, method, defaultName);
        }

        @Override
        public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            try {
                String name = method.getName();
                if (name.startsWith("set"))
                    name = name.substring(3);
                return name(name);
            } catch (IOException ex) {
                log.error("Error mapping YAML-name: {}", ex.getMessage());
                log.debug("Error mapping YAML-name: ", ex);
            }
            return super.nameForSetterMethod(config, method, defaultName);
        }

        private String name(String name) throws IOException {
            StringReader r = new StringReader(name);
            StringWriter w = new StringWriter(name.length() + 5);
            int c = r.read();
            if (c != -1) {
                w.write(Character.toLowerCase(c)); // No dash before first letter
                for (;;) {
                    c = r.read();
                    if (c == -1)
                        break;
                    if (Character.isUpperCase(c))
                        w.write('-');
                    w.write(Character.toLowerCase(c));
                }
            }
            return w.toString();
        }
    }

}
