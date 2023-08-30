package org.shaktifdn.registration.service;


import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class XForwardedRemoteAddressResolverTest {
    private final InetSocketAddress remote0000Address = InetSocketAddress.createUnresolved("0.0.0.0", 1234);

    private final XForwardedRemoteAddressResolver trustOne = XForwardedRemoteAddressResolver.maxTrustedIndex(1);


    @Test
    public void maxIndexOneReturnsLastForwardedIp() {
        ServerWebExchange exchange = buildExchange(oneTwoThreeBuilder());

        InetSocketAddress address = trustOne.resolve(exchange.getRequest());

        assertThat(address.getHostName()).isEqualTo("0.0.0.3");
    }

    @Test
    public void maxIndexOneFallsBackToRemoteIp() {
        ServerWebExchange exchange = buildExchange(remoteAddressOnlyBuilder());

        InetSocketAddress address = trustOne.resolve(exchange.getRequest());

        assertThat(address.getHostName()).isEqualTo("0.0.0.0");
    }

    @Test
    public void maxIndexOneReturnsNullIfNoForwardedOrRemoteIp() {
        ServerWebExchange exchange = buildExchange(emptyBuilder());

        InetSocketAddress address = trustOne.resolve(exchange.getRequest());

        assertThat(address).isEqualTo(null);
    }

    @Test
    public void trustOneFallsBackOnEmptyHeader() {
        ServerWebExchange exchange = buildExchange(remoteAddressOnlyBuilder().header("X-Forwarded-For", ""));

        InetSocketAddress address = trustOne.resolve(exchange.getRequest());

        assertThat(address.getHostName()).isEqualTo("0.0.0.0");

    }

    @Test
    public void trustOneFallsBackOnMultipleHeaders() {
        ServerWebExchange exchange = buildExchange(
                remoteAddressOnlyBuilder().header("X-Forwarded-For", "0.0.0.1").header("X-Forwarded-For", "0.0.0.2"));

        InetSocketAddress address = trustOne.resolve(exchange.getRequest());

        assertThat(address.getHostName()).isEqualTo("0.0.0.0");
    }


    private MockServerHttpRequest.BaseBuilder emptyBuilder() {
        return MockServerHttpRequest.get("someUrl");
    }

    private MockServerHttpRequest.BaseBuilder remoteAddressOnlyBuilder() {
        return MockServerHttpRequest.get("someUrl").remoteAddress(remote0000Address);
    }

    private MockServerHttpRequest.BaseBuilder oneTwoThreeBuilder() {
        return MockServerHttpRequest.get("someUrl").remoteAddress(remote0000Address).header("X-Forwarded-For",
                "0.0.0.1, 0.0.0.2, 0.0.0.3");
    }

    private ServerWebExchange buildExchange(MockServerHttpRequest.BaseBuilder requestBuilder) {
        return MockServerWebExchange.from(requestBuilder.build());
    }


}