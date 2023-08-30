package org.shaktifdn.registration.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IpAddressServiceTest {

    public static final String IP_ADDRESS = "0.0.0.0";

    @Mock
    InetAddress inetAddress;
    @InjectMocks
    IpAddressService ipAddressService;
    @Mock
    private InetSocketAddress mockedAddress;
    @Mock
    private XForwardedRemoteAddressResolver xForwardedRemoteAddressResolver;
    @Mock
    private ServerHttpRequest ServerHttpRequest;

    @Test
    void shouldGetTheRightIpAddress() {
        given(xForwardedRemoteAddressResolver.resolve(any(ServerHttpRequest.class))).willReturn(mockedAddress);
        given(mockedAddress.getAddress()).willReturn(inetAddress);
        given(inetAddress.getHostAddress()).willReturn(IP_ADDRESS);

        String clientIpAddress = ipAddressService.getClientIpAddress("test@gmail.com", ServerHttpRequest);

        assertThat(clientIpAddress).isNotBlank();
        assertThat(clientIpAddress).isEqualTo(IP_ADDRESS);
    }

    @Test
    void shouldReturnNullIpAddress() {
        given(xForwardedRemoteAddressResolver.resolve(any(ServerHttpRequest.class))).willReturn(null);
        String clientIpAddress = ipAddressService.getClientIpAddress("test@gmail.com", ServerHttpRequest);

        assertThat(clientIpAddress).isNull();

    }


}