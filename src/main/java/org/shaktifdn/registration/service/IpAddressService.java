package org.shaktifdn.registration.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;

@Service
@Slf4j
@AllArgsConstructor
public class IpAddressService {

    private final XForwardedRemoteAddressResolver xForwardedRemoteAddressResolver;

    public String getClientIpAddress(String userEmail, ServerHttpRequest serverHttpRequest) {
        InetSocketAddress inetSocketAddress = xForwardedRemoteAddressResolver.resolve(serverHttpRequest);
        String ipAddress = inetSocketAddress != null && inetSocketAddress.getAddress() != null ?
                inetSocketAddress.getAddress().getHostAddress() : null;
        if (StringUtils.hasText(ipAddress)
                && ipAddress.length() > 15
                && ipAddress.indexOf(',') > 0) {
            ipAddress = ipAddress.substring(0, ipAddress.indexOf(','));
        }
        log.info("Calling user IP address for user email id {} is {}", userEmail, ipAddress);
        return ipAddress;
    }
}
