package org.shaktifdn.registration.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shaktifdn.registration.AbstractTest;
import org.shaktifdn.registration.exception.ConflictRecordsException;
import org.shaktifdn.registration.exception.RecordNotFoundException;
import org.shaktifdn.registration.exception.SelfyIdBadRequestException;
import org.shaktifdn.registration.exception.ShaktiWebClientException;
import org.shaktifdn.registration.request.GeoJSONModel;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.shaktifdn.registration.response.ResponseBean;
import org.shaktifdn.registration.service.IpAddressService;
import org.shaktifdn.registration.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import javax.ws.rs.core.UriBuilder;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(UserController.class)
public class UserControllerTest extends AbstractTest {

    private static final String IP_ADDRESS = "0.0.0.0";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

    @MockBean
    private IpAddressService ipAddressService;

    @BeforeEach
    void setup() {
        when(ipAddressService.getClientIpAddress(anyString(), any(ServerHttpRequest.class))).thenReturn(IP_ADDRESS);
    }


    @Test
    public void saveOnboardShaktiTest() {
        OnboardShaktiUserRequest request = new OnboardShaktiUserRequest("1", "amitzkumar001@gmail.com", "127.0.0.1",
                "+1", "9876549876", true, false, "Qwert@123", "1234", "123456",
                new GeoJSONModel(2, 24), "fd", null, null, null, null);
        when(userService.saveOnboardShakti(eq(request), anyString()))
                .thenReturn(Mono.just(new ResponseBean()));
        webTestClient
                .post().uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange().expectStatus().isOk();
        verify(userService).saveOnboardShakti(eq(request), anyString());
    }
    @Test
    public void saveOnboardShaktiTestWithPeriodInPassword() {
        OnboardShaktiUserRequest request = new OnboardShaktiUserRequest("1", "amitzkumar001@gmail.com", "127.0.0.1",
                "+1", "9876549876", true, false, "Qwert.123", "1234", "123456",
                new GeoJSONModel(2, 24), "fd", null, null, null, null);
        when(userService.saveOnboardShakti(eq(request), anyString()))
                .thenReturn(Mono.just(new ResponseBean()));
        webTestClient
                .post().uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange().expectStatus().isOk();
        verify(userService).saveOnboardShakti(eq(request), anyString());
    }

    @Test
    public void shouldThrowConflictError() {
        OnboardShaktiUserRequest request = new OnboardShaktiUserRequest("1", "amitzkumar001@gmail.com", "127.0.0.1",
                "+1", "9876549876", true, false, "Qwert@123", "1234", "123456",
                new GeoJSONModel(2, 24), "fd", null, null, null, null);

        when(userService.saveOnboardShakti(eq(request), anyString()))
                .thenReturn(Mono.error(new ConflictRecordsException("test")));
        webTestClient
                .post().uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange().expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
        verify(userService).saveOnboardShakti(eq(request), anyString());
    }

    @Test
    public void shouldThrowRecordNotFoundError() {
        OnboardShaktiUserRequest request = new OnboardShaktiUserRequest("1", "amitzkumar001@gmail.com", "127.0.0.1",
                "+1", "9876549876", true, false, "Qwert@123", "1234", "123456",
                new GeoJSONModel(2, 24), "fd", null, null, null, null);

        when(userService.saveOnboardShakti(eq(request), anyString()))
                .thenReturn(Mono.error(new RecordNotFoundException("test")));
        webTestClient
                .post().uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange().expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
        verify(userService).saveOnboardShakti(eq(request), anyString());
    }

    @Test
    public void shouldThrowShaktiWebClientError() {
        OnboardShaktiUserRequest request = new OnboardShaktiUserRequest("1", "amitzkumar001@gmail.com", "127.0.0.1",
                "+1", "9876549876", true, false, "Qwert@123", "1234", "123456",
                new GeoJSONModel(2, 24), "fd", null, null, null, null);

        when(userService.saveOnboardShakti(eq(request), anyString()))
                .thenReturn(Mono.error(new ShaktiWebClientException("test")));
        webTestClient
                .post().uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange().expectStatus().is4xxClientError();
        verify(userService).saveOnboardShakti(eq(request), anyString());
    }

    @Test
    public void shouldThrowSelfyBadError() {
        OnboardShaktiUserRequest request = new OnboardShaktiUserRequest("1", "amitzkumar001@gmail.com", "127.0.0.1",
                "+1", "9876549876", true, false, "Qwert@123", "1234", "123456",
                new GeoJSONModel(2, 24), "fd", null, null, null, null);

        when(userService.saveOnboardShakti(eq(request), anyString()))
                .thenReturn(Mono.error(new SelfyIdBadRequestException("test")));
        webTestClient
                .post().uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
        verify(userService).saveOnboardShakti(eq(request), anyString());
    }

    @Test
    public void shouldThrowSelfyError() {
        OnboardShaktiUserRequest request = new OnboardShaktiUserRequest("1", "amitzkumar001@gmail.com", "127.0.0.1",
                "+1", "9876549876", true, false, "Qwert@123", "1234", "123456",
                new GeoJSONModel(2, 24), "fd", null, null, null, null);

        when(userService.saveOnboardShakti(eq(request), anyString()))
                .thenReturn(Mono.error(new ShaktiWebClientException("test")));
        webTestClient
                .post().uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange().expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
        verify(userService).saveOnboardShakti(eq(request), anyString());
    }

    @Test
    public void checkStatusByEmailTest_already_exists() {
        String email = "amitzkumar001@gmail.com";
        given(userService.checkEmailIsRegistered(email)).willReturn(Mono.just(true));
        webTestClient
                .get()
                .uri(UriBuilder.fromPath("/users/status").queryParam("email", email).build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.OK)
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("User is already registered");
    }

    @Test
    public void checkStatusByEmailTest() {
        String email = "amitzkumar001@gmail.com";
        given(userService.checkEmailIsRegistered(email)).willReturn(Mono.just(false));
        webTestClient
                .get()
                .uri(UriBuilder.fromPath("/users/status").queryParam("email", email).build())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.OK)
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("User is not registered");
    }

}
