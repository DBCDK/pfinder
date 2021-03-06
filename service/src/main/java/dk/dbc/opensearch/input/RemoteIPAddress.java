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
package dk.dbc.opensearch.input;

import dk.dbc.opensearch.setup.Settings;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.EMPTY_LIST;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
@Singleton
@Startup
@Lock(LockType.READ)
public class RemoteIPAddress {

    private static final Logger log = LoggerFactory.getLogger(RemoteIPAddress.class);

    private static final long IPV4_MAX = 0xffffffffL;
    private List<IPRange> allowedProxyIpRanges;

    @Inject
    Settings settings;

    @PostConstruct
    public void init() {
        String xForwardedFor = settings.getXForwardedFor();
        if (xForwardedFor == null || xForwardedFor.isEmpty()) {
            this.allowedProxyIpRanges = EMPTY_LIST;
        } else {
            this.allowedProxyIpRanges = Arrays.stream(xForwardedFor.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(RemoteIPAddress::ipRange)
                    .collect(Collectors.toList());
        }
    }

    public String ip(HttpHeaders headers, HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        String xForwardedFor = headers.getHeaderString("x-forwarded-for");
        // X-Forwarded-For syntax is client-ip[, proxy-ip ...]
        log.trace("xForwardedFor header: {}", xForwardedFor);

        if (xForwardedFor != null && !xForwardedFor.isEmpty() &&
            inIpRange(peer)) {
            // Proxy connected to us, and has x-forwarded-for set
            String[] xForwardedFors = xForwardedFor.split(",");
            // Ensure (optional) proxies are in our allowed list
            int pos = xForwardedFors.length;
            while (--pos > 0) {
                String proxy = xForwardedFors[pos].trim();
                if (!inIpRange(proxy)) {
                    return proxy; // Proxy is not in allowed list - proxy is our peer
                }
            }
            return xForwardedFors[pos].trim();
        }
        return peer;
    }

    private boolean inIpRange(String peer) {
        long ip = ipOf(peer);
        return allowedProxyIpRanges.stream().anyMatch(i -> i.isInRange(ip));
    }

    private static IPRange ipRange(String hosts) {
        if (hosts.contains("-")) {
            String[] parts = hosts.split("-", 2);
            long ipMin = ipOf(parts[0]);
            long ipMax = ipOf(parts[1]);
            return new IPRange(ipMin, ipMax);
        } else if (hosts.contains("/")) {
            String[] parts = hosts.split("/", 2);
            long ip = ipOf(parts[0]);
            long net = ( IPV4_MAX << ( 32 - Integer.parseInt(parts[1]) ) ) & IPV4_MAX;
            return new IPRange(ip & net, ip | ~net);
        } else {
            long ip = ipOf(hosts);
            return new IPRange(ip, ip);
        }
    }

    private static long ipOf(String addr) {
        if (addr.contains(".")) {
            return Arrays.stream(addr.split("\\.")).mapToInt(Integer::parseUnsignedInt).reduce(0, (l, r) -> ( l << 8 ) + r);
        } else {
            return IPV4_MAX;
        }
    }

    private static class IPRange {

        private final long min;
        private final long max;

        private IPRange(long min, long max) {
            this.min = min;
            this.max = max;
        }

        private boolean isInRange(long ip) {
            return ip >= min && ip <= max;
        }

        @Override
        public String toString() {
            return String.format("(%08x-%08x)", min, max);
        }

    }

}
