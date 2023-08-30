package org.shaktifdn.registration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.core.HoverflyConfig;
import io.specto.hoverfly.junit.core.model.RequestFieldMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shaktifdn.registration.AbstractTest;
import org.shaktifdn.registration.config.ServiceProperties;
import org.shaktifdn.registration.response.BizVaultRegistrationStatus;
import org.shaktifdn.registration.response.ShaktiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static io.specto.hoverfly.junit.core.HoverflyMode.SIMULATE;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BizVaultServiceTest extends AbstractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Hoverfly hoverfly;
    private BizVaultService bizVaultServiceWebClient;

    @BeforeEach
    void setUp() {
        var localConfig = HoverflyConfig
                .localConfigs()
                .disableTlsVerification()
                .asWebServer()
                .proxyPort(18999);
        hoverfly = new Hoverfly(localConfig, SIMULATE);
        hoverfly.start();

        ServiceProperties properties = mock(ServiceProperties.class);
        when(properties.getBizVaultService()).thenReturn("http://localhost:18999");

        bizVaultServiceWebClient = new BizVaultService(
                WebClient.builder()
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                properties
        );
    }

    @AfterEach
    void tearDown() {
        hoverfly.close();
    }

    @Test
    void isEmailRegistered() throws JsonProcessingException {
        ShaktiResponse<BizVaultRegistrationStatus> response =
                ShaktiResponse
                        .<BizVaultRegistrationStatus>builder()
                        .status(true)
                        .data(BizVaultRegistrationStatus.builder().isBizVaultRegistered(true).build())
                        .build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .get(BizVaultService.BIZVAULTS_EMAIL_REGISTRATION_STATUS)
                        .queryParam("email", "a@a.com")
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));


        StepVerifier
                .create(bizVaultServiceWebClient.isEmailRegistered("a@a.com"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void isEmailRegisteredNotRegistered() throws JsonProcessingException {
        ShaktiResponse<BizVaultRegistrationStatus> response =
                ShaktiResponse
                        .<BizVaultRegistrationStatus>builder()
                        .status(true)
                        .data(BizVaultRegistrationStatus.builder().isBizVaultRegistered(false).build())
                        .build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .get(BizVaultService.BIZVAULTS_EMAIL_REGISTRATION_STATUS)
                        .queryParam("email", "a@a.com")
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));


        StepVerifier
                .create(bizVaultServiceWebClient.isEmailRegistered("a@a.com"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void isEmailRegisteredError() {
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .get(BizVaultService.BIZVAULTS_EMAIL_REGISTRATION_STATUS)
                        .queryParam("email", "a@a.com")
                        .header("Content-Type", "application/json")
                        .willReturn(serverError())
        ));


        StepVerifier
                .create(bizVaultServiceWebClient.isEmailRegistered("a@a.com"))
                .expectErrorMessage("An error has been occurred during the request processing , please try again later.")
                .verify();
    }

}