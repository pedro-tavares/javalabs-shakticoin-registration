package org.shaktifdn.registration.service;

import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.core.HoverflyConfig;
import io.specto.hoverfly.junit.core.model.RequestFieldMatcher;
import io.specto.hoverfly.junit.dsl.HoverflyDsl;
import io.specto.hoverfly.junit.dsl.matchers.HoverflyMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.shaktifdn.registration.config.ServiceProperties;
import org.shaktifdn.registration.exception.ExternalServiceDependencyFailure;
import org.shaktifdn.registration.exception.SelfyIdBadRequestException;
import org.shaktifdn.registration.request.WalletBytesEncryptRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static io.specto.hoverfly.junit.core.HoverflyMode.SIMULATE;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author romeh
 */
class SelfyIdServiceTest {
    private static SelfyIdService selfyIdService;
    private static Hoverfly hoverfly;

    @BeforeAll
    static void setUp() throws IOException {
        var localConfig = HoverflyConfig
                .localConfigs()
                .disableTlsVerification()
                .asWebServer()
                .proxyPort(8999);
        hoverfly = new Hoverfly(localConfig, SIMULATE);
        hoverfly.start();
        ServiceProperties properties = mock(ServiceProperties.class);
        when(properties.getSelfyIdService())
                .thenReturn(String.format("http://localhost:%s", hoverfly.getHoverflyConfig().getProxyPort()));

        selfyIdService = new SelfyIdService(
                WebClient.builder()
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                properties
        );
    }

    @AfterAll
    static void tearDown() throws IOException {
        hoverfly.close();
    }

    @Test
    void encrypt() {
        hoverfly.reset();
        var simulation = dsl(
                // mock Kyc service
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(HoverflyMatchers.contains("/selfyid/encrypt"))
                        .anyBody()
                        .anyQueryParams()
                        .willReturn(
                                success()
                                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .body("{\"encryptedWalletBytes\":\"encrypt test\",\"encryptedPassphrase\":\"encrypt pass\"}")
                        ));
        hoverfly.simulate(simulation);
        StepVerifier
                .create(selfyIdService.encrypt(
                        WalletBytesEncryptRequest.builder().walletBytes("test").passphrase("pass").build()
                ))
                .expectNextMatches(response -> response.getEncryptedWalletBytes().equals("encrypt test") &&
                        response.getEncryptedPassphrase().equals("encrypt pass"))
                .verifyComplete();
    }

    @Test
    void encryptBadRequest() {
        hoverfly.reset();
        hoverfly.simulate(dsl(
                // mock selfyid service
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(HoverflyMatchers.contains("/selfyid/encrypt"))
                        .anyBody()
                        .anyQueryParams()
                        .willReturn(HoverflyDsl.response().status(HttpStatus.BAD_REQUEST.value()))));

        StepVerifier
                .create(selfyIdService.encrypt(
                        WalletBytesEncryptRequest.builder().build()
                ))
                .expectError(SelfyIdBadRequestException.class)
                .verify();
    }

    @Test
    void encryptInternalServiceError() {
        hoverfly.reset();
        hoverfly.simulate(dsl(
                // mock selfyid service
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(HoverflyMatchers.contains("/selfyid/encrypt"))
                        .anyBody()
                        .anyQueryParams()
                        .willReturn(HoverflyDsl.response().status(HttpStatus.INTERNAL_SERVER_ERROR.value()))));

        StepVerifier
                .create(selfyIdService.encrypt(
                        WalletBytesEncryptRequest.builder().build()
                ))
                .expectError(ExternalServiceDependencyFailure.class)
                .verify();
    }

}