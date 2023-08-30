package org.shaktifdn.registration.controller;

import org.junit.jupiter.api.Test;
import org.shaktifdn.registration.AbstractTest;
import org.shaktifdn.registration.constant.Message;
import org.shaktifdn.registration.exception.LockedException;
import org.shaktifdn.registration.exception.TooManyRequestException;
import org.shaktifdn.registration.request.EmailVerificationRequest;
import org.shaktifdn.registration.request.VerifyEmailOtpRequest;
import org.shaktifdn.registration.response.EmailServiceResponse;
import org.shaktifdn.registration.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.shaktifdn.registration.constant.Constant.EMAIL_REGISTERATION_FLOW;

@WebFluxTest(EmailController.class)
public class EmailControllerTest extends AbstractTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private EmailService emailService;

    @Test
    public void sendForRegistration() {
        EmailVerificationRequest emailVerificationRequest =
                new EmailVerificationRequest("amitzkumar001@gmail.com");
        when(emailService.sendOtp(
                emailVerificationRequest.getEmail(),
                EMAIL_REGISTERATION_FLOW
        )).thenReturn(Mono.just(EmailServiceResponse.builder().build()));

        webClient
                .post()
                .uri("/email/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(emailVerificationRequest)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.message").isEqualTo(Message.EMAIL_OTP_SENT_SUCCESS);

        verify(emailService).sendOtp(
                emailVerificationRequest.getEmail(),
                EMAIL_REGISTERATION_FLOW
        );
    }

    @Test
    public void sendForRegistration_failed() {
        EmailVerificationRequest emailVerificationRequest =
                new EmailVerificationRequest("");
        when(emailService.sendOtp(
                emailVerificationRequest.getEmail(),
                EMAIL_REGISTERATION_FLOW
        )).thenReturn(Mono.just(EmailServiceResponse.builder().build()));

        webClient
                .post()
                .uri("/email/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(emailVerificationRequest)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);

        verifyNoMoreInteractions(emailService);
    }

    @Test
    public void testVerify() {

        when(emailService.verifyOtp("aa@aa.com", "123456", EMAIL_REGISTERATION_FLOW))
                .thenReturn(Mono.just(EmailServiceResponse.builder().build()));
        VerifyEmailOtpRequest request = VerifyEmailOtpRequest
                .builder()
                .email("aa@aa.com")
                .otp("123456")
                .build();
        webClient
                .post()
                .uri("/email/registration/confirm?token=123456")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.message").isEqualTo(Message.EMAIL_VERIFIED_SUCCESS)
                .jsonPath("$.data.isVerified").isEqualTo(true)
                .jsonPath("$.data.email").isEqualTo("aa@aa.com");

        verify(emailService).verifyOtp("aa@aa.com", "123456", EMAIL_REGISTERATION_FLOW);
    }

    @Test
    public void testVerify_failed() {

        when(emailService.verifyOtp("aa@aa.com", "123456", EMAIL_REGISTERATION_FLOW))
                .thenReturn(Mono.empty());
        VerifyEmailOtpRequest request = VerifyEmailOtpRequest
                .builder()
                .email("aa@aa.com")
                .otp("123456")
                .build();
        webClient
                .post()
                .uri("/email/registration/confirm?token=123456")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false)
                .jsonPath("$.message").isEqualTo(Message.EMAIL_NOT_VERIFIED)
                .jsonPath("$.data.isVerified").isEqualTo(false);

        verify(emailService).verifyOtp("aa@aa.com", "123456", EMAIL_REGISTERATION_FLOW);
    }

    @Test
    public void shouldThrowLockException() {

        when(emailService.verifyOtp(any(), any(), any())).thenReturn(Mono.error(new LockedException("test")));
        VerifyEmailOtpRequest request = VerifyEmailOtpRequest
                .builder()
                .email("aa@aa.com")
                .otp("123456")
                .build();
        webClient
                .post()
                .uri("/email/registration/confirm?token=123456")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);

        verify(emailService).verifyOtp(any(), any(), any());
    }

    @Test
    public void shouldThrowWebClientResponseException() {

        when(emailService.verifyOtp(any(), any(), any()))
                .thenReturn(Mono.error(new WebClientResponseException(500, "test", null, null, null)));
        VerifyEmailOtpRequest request = VerifyEmailOtpRequest
                .builder()
                .email("aa@aa.com")
                .otp("123456")
                .build();
        webClient
                .post()
                .uri("/email/registration/confirm?token=123456")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);

        verify(emailService).verifyOtp(any(), any(), any());
    }

    @Test
    public void shouldThrowException() {

        when(emailService.verifyOtp(any(), any(), any()))
                .thenReturn(Mono.error(new Exception()));
        VerifyEmailOtpRequest request = VerifyEmailOtpRequest
                .builder()
                .email("aa@aa.com")
                .otp("123456")
                .build();
        webClient
                .post()
                .uri("/email/registration/confirm?token=123456")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);

        verify(emailService).verifyOtp(any(), any(), any());
    }

    @Test
    public void shouldThrowTooMany() {

        when(emailService.verifyOtp(any(), any(), any()))
                .thenReturn(Mono.error(new TooManyRequestException("test", null)));
        VerifyEmailOtpRequest request = VerifyEmailOtpRequest
                .builder()
                .email("aa@aa.com")
                .otp("123456")
                .build();
        webClient
                .post()
                .uri("/email/registration/confirm?token=123456")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);

        verify(emailService).verifyOtp(any(), any(), any());
    }


}
