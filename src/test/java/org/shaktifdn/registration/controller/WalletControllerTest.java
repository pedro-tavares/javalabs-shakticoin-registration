package org.shaktifdn.registration.controller;

import org.junit.jupiter.api.Test;
import org.shaktifdn.registration.AbstractTest;
import org.shaktifdn.registration.exception.UnauthorizedException;
import org.shaktifdn.registration.exception.WalletAlreadyExistsException;
import org.shaktifdn.registration.request.WalletRequest;
import org.shaktifdn.registration.response.CreateWalletResponse;
import org.shaktifdn.registration.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(WalletController.class)
class WalletControllerTest extends AbstractTest {

    @MockBean
    private WalletService walletService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void create() {
        WalletRequest request = new WalletRequest();
        request.setPassphrase("test");
        when(walletService.create(request)).thenReturn(Mono.just(new CreateWalletResponse()));
        webTestClient
                .post()
                .uri("/wallet")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Your wallet creation request is in progress");
        verify(walletService).create(request);
    }

    @Test
    void create_already_exists() {
        WalletRequest request = new WalletRequest();
        request.setPassphrase("test");
        when(walletService.create(request)).thenReturn(Mono.error(new WalletAlreadyExistsException("")));
        webTestClient
                .post()
                .uri("/wallet")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("You already have a wallet");
        verify(walletService).create(request);
    }

    @Test
    void shouldThrowUnauthorizedError() {
        WalletRequest request = new WalletRequest();
        request.setPassphrase("test");
        when(walletService.create(request)).thenReturn(Mono.error(new UnauthorizedException("")));
        webTestClient
                .post()
                .uri("/wallet")
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
        verify(walletService).create(request);
    }

    @Test
    void create_invalid_request() {
        WalletRequest request = new WalletRequest();
        webTestClient
                .post()
                .uri("/wallet")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }
}