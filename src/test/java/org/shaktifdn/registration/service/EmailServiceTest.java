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
import org.shaktifdn.registration.exception.*;
import org.shaktifdn.registration.request.SendEmailOtpRequest;
import org.shaktifdn.registration.request.VerifyEmailOtpRequest;
import org.shaktifdn.registration.response.EmailServiceResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.Map;

import static io.specto.hoverfly.junit.core.HoverflyMode.SIMULATE;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailServiceTest extends AbstractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Hoverfly hoverfly;
    private EmailService emailService;

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
        when(properties.getEmailService()).thenReturn("http://localhost:18999");

        emailService = new EmailService(
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
    void sendOtp() throws JsonProcessingException {
        SendEmailOtpRequest request = SendEmailOtpRequest
                .builder()
                .email("test@xyz.c")
                .requestedFlow("test")
                .build();

        EmailServiceResponse response = EmailServiceResponse.builder().code(HttpStatus.OK.value()).build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(EmailService.EMAIL_OTP_REQUEST)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));


        StepVerifier
                .create(emailService.sendOtp(request.getEmail(), request.getRequestedFlow()))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void shouldLockErrorOnSendOtp() throws JsonProcessingException {
        testSendOtpError(HttpStatus.LOCKED.value(), "", LockedException.class);
    }

    @Test
    void shouldTooManyErrorOnSendOtp() throws JsonProcessingException {
        testSendOtpError(HttpStatus.TOO_MANY_REQUESTS.value(), "", TooManyRequestException.class);
    }

    @Test
    void shouldServerErrorOnSendOtp() throws JsonProcessingException {
        testSendOtpError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "", ShaktiWebClientException.class);
    }

    @Test
    void shouldThrow404OnSendOtp() throws JsonProcessingException {
        testSendOtpError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "404", RecordNotFoundException.class);
    }

    @Test
    void shouldThrow400OnSendOtp() throws JsonProcessingException {
        testSendOtpError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "400", BadRequestException.class);
    }

    @Test
    void shouldThrow406OnSendOtp() throws JsonProcessingException {
        testSendOtpError(HttpStatus.NOT_ACCEPTABLE.value(), "406", DisposableEmailException.class);
    }

    private void testSendOtpError(int code, String message, Class<? extends AbstractException> errorClass) throws JsonProcessingException {
        SendEmailOtpRequest request = SendEmailOtpRequest
                .builder()
                .email("test@xyz.c")
                .requestedFlow("test")
                .build();

        EmailServiceResponse response = EmailServiceResponse.builder().code(code).message(message).build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(EmailService.EMAIL_OTP_REQUEST)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));


        StepVerifier
                .create(emailService.sendOtp(request.getEmail(), request.getRequestedFlow()))
                .expectError(errorClass)
                .verify();
    }

    @Test
    void verifyOtp() throws JsonProcessingException {
        VerifyEmailOtpRequest request = VerifyEmailOtpRequest
                .builder()
                .email("test@xyz.c")
                .requestedFlow("test")
                .otp("123123")
                .build();

        EmailServiceResponse response = EmailServiceResponse
                .builder()
                .code(200)
                .build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(EmailService.EMAIL_OTP_VERIFY)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));


        StepVerifier
                .create(emailService.verifyOtp(
                        request.getEmail(), "123123", request.getRequestedFlow()
                ))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void should403ThrowErrorOnVerifyOtp() throws JsonProcessingException {
        testErrorForVerifyOtp(403, "", BadRequestException.class);
    }

    @Test
    void should406ThrowErrorOnVerifyOtp() throws JsonProcessingException {
        testErrorForVerifyOtp(406, "", BadRequestException.class);
    }

    @Test
    void should410ThrowErrorOnVerifyOtp() throws JsonProcessingException {
        testErrorForVerifyOtp(410, "", BadRequestException.class);
    }

    @Test
    void should409ThrowErrorOnVerifyOtp() throws JsonProcessingException {
        testErrorForVerifyOtp(409, "", ConflictRecordsException.class);
    }

    @Test
    void should423ThrowErrorOnVerifyOtp() throws JsonProcessingException {
        testErrorForVerifyOtp(423, "", LockedException.class);
    }

    @Test
    void shouldServerErrorThrowErrorOnVerifyOtp() throws JsonProcessingException {
        testErrorForVerifyOtp(500, "", ShaktiWebClientException.class);
    }

    @Test
    void shouldThrow404ErrorOnVerifyOtp() throws JsonProcessingException {
        testErrorForVerifyOtp(500, "404", RecordNotFoundException.class);
    }

    @Test
    void shouldThrow400ErrorOnVerifyOtp() throws JsonProcessingException {
        testErrorForVerifyOtp(500, "400", BadRequestException.class);
    }

    @Test
    void shouldThrow409ErrorOnVerifyOtp() throws JsonProcessingException {
        testErrorForVerifyOtp(500, "409", ConflictRecordsException.class);
    }

    private void testErrorForVerifyOtp(
            int code,
            String message,
            Class<? extends AbstractException> errorClass
    ) throws JsonProcessingException {
        VerifyEmailOtpRequest request = VerifyEmailOtpRequest
                .builder()
                .email("test@xyz.c")
                .requestedFlow("test")
                .otp("123123")
                .build();

        EmailServiceResponse response = EmailServiceResponse
                .builder()
                .code(code)
                .message(message)
                .build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(EmailService.EMAIL_OTP_VERIFY)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));


        StepVerifier
                .create(emailService.verifyOtp(
                        request.getEmail(), "123123", request.getRequestedFlow()
                ))
                .expectError(errorClass)
                .verify();
    }

    @Test
    void shouldIsOtpVerify() throws JsonProcessingException {
        SendEmailOtpRequest request = SendEmailOtpRequest
                .builder()
                .email("a@a.com")
                .requestedFlow("support")
                .build();

        EmailServiceResponse response = EmailServiceResponse
                .builder()
                .code(200)
                .payload(Map.of("status", "VERIFIED"))
                .build();

        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(EmailService.EMAIL_OTP_INQUIRY)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));

        StepVerifier
                .create(emailService.isOtpVerified(
                        request.getEmail(), request.getRequestedFlow()
                ))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldNotVerifyOtp() throws JsonProcessingException {
        SendEmailOtpRequest request = SendEmailOtpRequest
                .builder()
                .email("a@a.com")
                .requestedFlow("support")
                .build();

        EmailServiceResponse response = EmailServiceResponse
                .builder()
                .code(200)
                .payload(Map.of("status", "NOT_VERIFIED"))
                .build();

        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(EmailService.EMAIL_OTP_INQUIRY)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));

        StepVerifier
                .create(emailService.isOtpVerified(
                        request.getEmail(), request.getRequestedFlow()
                ))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldErrorOnOtpInquiry() throws JsonProcessingException {
        SendEmailOtpRequest request = SendEmailOtpRequest
                .builder()
                .email("a@a.com")
                .requestedFlow("support")
                .build();

        EmailServiceResponse response = EmailServiceResponse
                .builder()
                .code(500)
                .build();

        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(EmailService.EMAIL_OTP_INQUIRY)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));

        StepVerifier
                .create(emailService.isOtpVerified(
                        request.getEmail(), request.getRequestedFlow()
                ))
                .expectError(ShaktiWebClientException.class)
                .verify();
    }
}