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

import com.hazelcast.cache.HazelcastCacheManager;
import com.hazelcast.cache.impl.HazelcastServerCacheManager;
import com.hazelcast.config.Config;
import com.hazelcast.config.CacheSimpleConfig;
import com.hazelcast.config.CacheSimpleConfig.ExpiryPolicyFactoryConfig;
import com.hazelcast.config.CacheSimpleConfig.ExpiryPolicyFactoryConfig.DurationConfig;
import com.hazelcast.config.CacheSimpleConfig.ExpiryPolicyFactoryConfig.TimedExpiryPolicyFactoryConfig;
import com.hazelcast.config.CacheSimpleConfig.ExpiryPolicyFactoryConfig.TimedExpiryPolicyFactoryConfig.ExpiryPolicyType;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionConfig.MaxSizePolicy;
import com.hazelcast.config.EvictionPolicy;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.cache.CacheManager;
import javax.ejb.EJBException;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Singleton
@Startup
@Lock(LockType.READ)
public class JCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(JCacheConfig.class);

    @Inject
    Settings config;

    private CacheManager manager;

    @PostConstruct
    public void init() {
        log.info("Configuring cache");
        try {
            Context ctx = new InitialContext();
            manager = (CacheManager) ctx.lookup("payara/CacheManager");

            HazelcastCacheManager obj = manager.unwrap(HazelcastServerCacheManager.class);
            Config cacheConfigs = obj.getHazelcastInstance().getConfig();

            config.getJCache()
                    .forEach((name, rule) -> setupCache(cacheConfigs, name, rule));
        } catch (NamingException ex) {
            log.error("Error configuring JCache: {}", ex.getMessage());
            log.debug("Error configuring JCache: ", ex);
            throw new EJBException("Error configuring JCache", ex);
        }
    }

    private void setupCache(Config cacheConfigs, String name, String rule) {
        CacheSimpleConfig configTime = cacheConfigs.getCacheConfig(name);
        if (configTime == null) {
            log.info("Setting up cache: {} with rule: {}", name, rule);
            cacheConfigs.addCacheConfig(cacheConfig(name, rule));
        } else {
            log.info("Cache: {} is already set up", name);
        }
    }

    /**
     * Make configuration from rule
     *
     * @param name name of configuration
     * @param rule ${ExpiryPolicyType}:\d+(time-unit(ms|s|m|h)):${MaxEntries}
     * @return configuration
     */
    private CacheSimpleConfig cacheConfig(String name, String rule) {
        String[] split = rule.split(":", 3);
        return new CacheSimpleConfig()
                .setName(name)
                .setExpiryPolicyFactoryConfig(
                        new ExpiryPolicyFactoryConfig(
                                new TimedExpiryPolicyFactoryConfig(
                                        ExpiryPolicyType.valueOf(split[0].toUpperCase(Locale.ROOT)),
                                        durationConfig(split[1]))))
                .setEvictionConfig(
                        new EvictionConfig(cacheSize(split),
                                           MaxSizePolicy.ENTRY_COUNT,
                                           EvictionPolicy.LFU)); // Least Recently Used
    }

    public static int cacheSize(String[] split) throws NumberFormatException {
        return Integer.max(1, Integer.parseInt(split[2]));
    }

    private static DurationConfig durationConfig(String spec) {
        String[] split = spec.split("(?<=\\d)(?=\\D)");
        if (split.length == 2) {
            long units = Long.parseUnsignedLong(split[0], 10);
            switch (split[1].toLowerCase(Locale.ROOT)) {
                case "ms":
                    return new DurationConfig(units, TimeUnit.MILLISECONDS);
                case "s":
                    return new DurationConfig(units, TimeUnit.SECONDS);
                case "m":
                    return new DurationConfig(units, TimeUnit.MINUTES);
                case "h":
                    return new DurationConfig(units, TimeUnit.HOURS);
                default:
                    break;
            }
        }
        throw new IllegalArgumentException("Invalid time spec: " + spec);
    }

}
