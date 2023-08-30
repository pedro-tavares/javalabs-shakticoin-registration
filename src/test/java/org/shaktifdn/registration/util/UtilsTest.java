package org.shaktifdn.registration.util;

import org.junit.jupiter.api.Test;
import org.shaktifdn.registration.exception.BadRequestException;
import org.shaktifdn.registration.exception.ExternalServiceDependencyFailure;
import org.shaktifdn.registration.exception.ShaktiWebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;


class UtilsTest {

    @Test
    void shouldReturnExternalDependencyError() {
        WebClientResponseException webClientResponseException = new WebClientResponseException(500, "test message", null, null, null);
        StepVerifier.create(Utils.handleExternalServiceCallException(webClientResponseException, "test is back"))
                .expectError(ExternalServiceDependencyFailure.class).verify();
    }

    @Test
    void shouldReturnShaktiWebClientError() {
        BadRequestException badRequestException = new BadRequestException("test");
        StepVerifier.create(Utils.handleExternalServiceCallException(badRequestException, "test is back"))
                .expectError(ShaktiWebClientException.class).verify();
    }

}