package org.shaktifdn.registration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class XForwardedRemoteAddressResolver {
    /**
     * Forwarded-For header name.
     */
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private final Function<ServerHttpRequest, InetSocketAddress> defaultRemoteIpResolver = ServerHttpRequest::getRemoteAddress;
    private final int maxTrustedIndex;

    private XForwardedRemoteAddressResolver(int maxTrustedIndex) {
        this.maxTrustedIndex = maxTrustedIndex;
    }

    /**
     * <em>trusted</em> IP address found in the X-Forwarded-For header (when present).
     * This configuration exists to prevent a malicious actor from spoofing the value of
     * the X-Forwarded-For header. If you know that your gateway application is only
     * accessible from a a trusted load balancer, then you can trust that the load
     * balancer will append a valid client IP address to the X-Forwarded-For header, and
     * should use a value of `1` for the `maxTrustedIndex`.
     * <p>
     * <p>
     * Given the X-Forwarded-For value of [0.0.0.1, 0.0.0.2, 0.0.0.3]:
     *
     * <pre>
     * maxTrustedIndex -> result
     *
     * [MIN_VALUE,0] -> IllegalArgumentException
     * 1 -> 0.0.0.3
     * 2 -> 0.0.0.2
     * 3 -> 0.0.0.1
     * [4, MAX_VALUE] -> 0.0.0.1
     * </pre>
     *
     * @param maxTrustedIndex correlates to the number of trusted proxies expected in
     *                        front of Spring Cloud Gateway (index starts at 1).
     * @return a {@link XForwardedRemoteAddressResolver} which extracts the last
     */
    public static XForwardedRemoteAddressResolver maxTrustedIndex(int maxTrustedIndex) {
        Assert.isTrue(maxTrustedIndex > 0, "An index greater than 0 is required");
        return new XForwardedRemoteAddressResolver(maxTrustedIndex);
    }

    public InetSocketAddress resolve(ServerHttpRequest exchange) {
        List<String> xForwardedValues = extractXForwardedValues(exchange);
        Collections.reverse(xForwardedValues);
        if (!xForwardedValues.isEmpty()) {
            int index = Math.min(xForwardedValues.size(), maxTrustedIndex) - 1;
            return new InetSocketAddress(xForwardedValues.get(index), 0);
        }
        return defaultRemoteIpResolver.apply(exchange);
    }

    /**
     * The X-Forwarded-For header contains a comma separated list of IP addresses. This
     * method parses those IP addresses into a list. If no X-Forwarded-For header is
     * found, an empty list is returned. If multiple X-Forwarded-For headers are found, an
     * empty list is returned out of caution.
     *
     * @return The parsed values of the X-Forwarded-Header.
     */
    private List<String> extractXForwardedValues(ServerHttpRequest exchange) {
        List<String> xForwardedValues = exchange.getHeaders().get(X_FORWARDED_FOR);
        if (xForwardedValues == null || xForwardedValues.isEmpty()) {
            return Collections.emptyList();
        }
        if (xForwardedValues.size() > 1) {
            log.warn("Multiple X-Forwarded-For headers found, discarding all");
            return Collections.emptyList();
        }
        List<String> values = Arrays.asList(xForwardedValues.get(0).split(", "));
        if (values.size() == 1 && !StringUtils.hasText(values.get(0))) {
            return Collections.emptyList();
        }
        return values;
    }
}
