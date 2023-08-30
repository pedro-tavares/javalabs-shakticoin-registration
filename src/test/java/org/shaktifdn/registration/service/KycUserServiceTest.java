package org.shaktifdn.registration.service;

import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.core.HoverflyConfig;
import io.specto.hoverfly.junit.core.model.RequestFieldMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shaktifdn.registration.AbstractTest;
import org.shaktifdn.registration.RegistrationTestConfiguration;
import org.shaktifdn.registration.config.ServiceProperties;
import org.shaktifdn.registration.exception.ExternalServiceDependencyFailure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import reactor.test.StepVerifier;

import static io.specto.hoverfly.junit.core.HoverflyMode.SIMULATE;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {KycUserService.class})
@Import(RegistrationTestConfiguration.class)
class KycUserServiceTest extends AbstractTest {

    private static Hoverfly hoverfly;

    @Autowired
    private KycUserService kycUserService;

    @MockBean
    private ServiceProperties serviceProperties;

    @BeforeEach
    void setUp() {
        var localConfig = HoverflyConfig
                .localConfigs()
                .disableTlsVerification()
                .asWebServer()
                .proxyPort(18999);
        hoverfly = new Hoverfly(localConfig, SIMULATE);
        hoverfly.start();
    }

    @AfterEach
    void tearDown() {
        hoverfly.close();
    }

    @Test
    void shouldCheckWalletExists() {
        when(serviceProperties.getKycService()).thenReturn("http://localhost:18999");
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .get(KycUserService.URL_KYC_WALLET)
                        .header("shaktiID", "sk-1")
                        .willReturn(
                                success()
                                        .body("{\"shaktiID\":\"sk-1\", \"walletID\":\"w-1\"}")
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));
        StepVerifier
                .create(kycUserService.isWalletExists("sk-1"))
                .expectNext(true)
                .verifyComplete();

        verify(serviceProperties).getKycService();
    }

    @Test
    void shouldCheckWalletNotExists() {
        when(serviceProperties.getKycService()).thenReturn("http://localhost:18999");
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .get(KycUserService.URL_KYC_WALLET)
                        .header("shaktiID", "sk-1")
                        .willReturn(
                                success()
                                        .body("{\"shaktiID\":\"sk-1\", \"walletID\":null}")
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));
        StepVerifier
                .create(kycUserService.isWalletExists("sk-1"))
                .expectNext(false)
                .verifyComplete();

        verify(serviceProperties).getKycService();
    }

    @Test
    void shouldThrowErrorOnCheckWalletExists() {
        when(serviceProperties.getKycService()).thenReturn("http://localhost:18999");
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .get(KycUserService.URL_KYC_WALLET)
                        .header("shaktiID", "sk-1")
                        .willReturn(serverError())
        ));
        StepVerifier
                .create(kycUserService.isWalletExists("sk-1"))
                .expectError(ExternalServiceDependencyFailure.class)
                .verify();

        verify(serviceProperties).getKycService();
    }
}