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
import org.shaktifdn.registration.request.SendMobileOtpRequest;
import org.shaktifdn.registration.request.VerificationMobileOtpRequest;
import org.shaktifdn.registration.request.VerifyMobileOtpRequest;
import org.shaktifdn.registration.response.SmsServiceResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.Map;

import static io.specto.hoverfly.junit.core.HoverflyMode.SIMULATE;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MobileServiceTest extends AbstractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Hoverfly hoverfly;
    private MobileService mobileService;

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
        when(properties.getSmsService()).thenReturn("http://localhost:18999");

        mobileService = new MobileService(
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
        SendMobileOtpRequest request = SendMobileOtpRequest
                .builder()
                .countryCode("+1")
                .mobileNo("1231231234")
                .requestedFlow("test")
                .build();

        SmsServiceResponse response = SmsServiceResponse.builder().code(HttpStatus.OK.value()).build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(MobileService.MOBILE_OTP_REQUEST)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));


        StepVerifier
                .create(mobileService.send(
                        request.getCountryCode(),
                        request.getMobileNo(),
                        request.getRequestedFlow()
                ))
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

    private void testSendOtpError(int code, String message, Class<? extends AbstractException> errorClass) throws JsonProcessingException {
        SendMobileOtpRequest request = SendMobileOtpRequest
                .builder()
                .countryCode("+1")
                .mobileNo("1231231234")
                .requestedFlow("test")
                .build();

        SmsServiceResponse response = SmsServiceResponse.builder().code(code).message(message).build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(MobileService.MOBILE_OTP_REQUEST)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));


        StepVerifier
                .create(mobileService.send(
                        request.getCountryCode(),
                        request.getMobileNo(),
                        request.getRequestedFlow()
                ))
                .expectError(errorClass)
                .verify();
    }

    @Test
    void verifyOtp() throws JsonProcessingException {
        VerifyMobileOtpRequest request = VerifyMobileOtpRequest
                .builder()
                .countryCode("+1")
                .mobileNo("`1231231234`")
                .requestedFlow("test")
                .otp("123123")
                .build();

        SmsServiceResponse response = SmsServiceResponse
                .builder()
                .code(200)
                .build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(MobileService.MOBILE_OTP_VERIFY)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));


        StepVerifier
                .create(mobileService.verify(
                        request.getCountryCode(),
                        request.getMobileNo(),
                        request.getOtp(),
                        request.getRequestedFlow()
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
        testErrorForVerifyOtp(409, "409", ConflictRecordsException.class);
    }

    private void testErrorForVerifyOtp(
            int code,
            String message,
            Class<? extends AbstractException> errorClass
    ) throws JsonProcessingException {
        VerifyMobileOtpRequest request = VerifyMobileOtpRequest
                .builder()
                .countryCode("+1")
                .mobileNo("`1231231234`")
                .requestedFlow("test")
                .otp("123123")
                .build();

        SmsServiceResponse response = SmsServiceResponse
                .builder()
                .code(code)
                .message(message)
                .build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(MobileService.MOBILE_OTP_VERIFY)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));


        StepVerifier
                .create(mobileService.verify(
                        request.getCountryCode(),
                        request.getMobileNo(),
                        request.getOtp(),
                        request.getRequestedFlow()
                ))
                .expectError(errorClass)
                .verify();
    }

    @Test
    void shouldIsOtpVerify() throws JsonProcessingException {
        VerificationMobileOtpRequest request = VerificationMobileOtpRequest
                .builder()
                .countryCode("+1")
                .mobileNo("1231233214")
                .requestedFlow("support")
                .build();

        SmsServiceResponse response = SmsServiceResponse
                .builder()
                .code(200)
                .payload(Map.of("status", "VERIFIED"))
                .build();

        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(MobileService.MOBILE_OTP_INQUIRY)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));

        StepVerifier
                .create(mobileService.inquire(
                        request.getMobileNo(),
                        request.getCountryCode(),
                        request.getRequestedFlow()
                ))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void shouldNotVerifyOtp() throws JsonProcessingException {
        VerificationMobileOtpRequest request = VerificationMobileOtpRequest
                .builder()
                .countryCode("+1")
                .mobileNo("1231233214")
                .requestedFlow("support")
                .build();

        SmsServiceResponse response = SmsServiceResponse
                .builder()
                .code(200)
                .payload(Map.of("status", "NOT_VERIFIED"))
                .build();

        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(MobileService.MOBILE_OTP_INQUIRY)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));

        StepVerifier
                .create(mobileService.inquire(
                        request.getMobileNo(),
                        request.getCountryCode(),
                        request.getRequestedFlow()
                ))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void shouldThrown400ErrorOnOtpInquiry() throws JsonProcessingException {
        testInquireErrors(400, "400", BadRequestException.class);
    }

    @Test
    void shouldThrown410ErrorOnOtpInquiry() throws JsonProcessingException {
        testInquireErrors(410, "410", RecordNotFoundException.class);
    }

    @Test
    void should400ThrownServerErrorOnOtpInquiry() throws JsonProcessingException {
        testInquireErrors(500, "400", BadRequestException.class);
    }

    @Test
    void shouldThrown404ErrorOnOtpInquiry() throws JsonProcessingException {
        testInquireErrors(500, "404", RecordNotFoundException.class);
    }

    @Test
    void shouldThrownServerErrorOnOtpInquiry() throws JsonProcessingException {
        testInquireErrors(500, "", ShaktiWebClientException.class);
    }

    @Test
    void shouldErrorOnOtpInquiry() throws JsonProcessingException {
        VerificationMobileOtpRequest request = VerificationMobileOtpRequest
                .builder()
                .countryCode("+1")
                .mobileNo("1231233214")
                .requestedFlow("support")
                .build();

        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(MobileService.MOBILE_OTP_INQUIRY)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                serverError()
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));

        StepVerifier
                .create(mobileService.inquire(
                        request.getMobileNo(),
                        request.getCountryCode(),
                        request.getRequestedFlow()
                ))
                .expectError(ExternalServiceDependencyFailure.class)
                .verify();
    }

    private void testInquireErrors(
            int code,
            String message,
            Class<? extends AbstractException> errorClass
    ) throws JsonProcessingException {
        VerificationMobileOtpRequest request = VerificationMobileOtpRequest
                .builder()
                .countryCode("+1")
                .mobileNo("1231233214")
                .requestedFlow("support")
                .build();

        SmsServiceResponse response = SmsServiceResponse
                .builder()
                .code(code)
                .message(message)
                .build();

        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(MobileService.MOBILE_OTP_INQUIRY)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));

        StepVerifier
                .create(mobileService.inquire(
                        request.getMobileNo(),
                        request.getCountryCode(),
                        request.getRequestedFlow()
                ))
                .expectError(errorClass)
                .verify();
    }

    @Test
    void fallback() {
        StepVerifier
                .create(mobileService.getMobileFallback(new RuntimeException()))
                .expectError(RuntimeException.class)
                .verify();
    }

}