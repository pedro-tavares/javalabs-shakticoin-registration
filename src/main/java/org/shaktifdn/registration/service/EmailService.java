package org.shaktifdn.registration.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.shaktifdn.registration.config.ServiceProperties;
import org.shaktifdn.registration.exception.*;
import org.shaktifdn.registration.request.SendEmailOtpRequest;
import org.shaktifdn.registration.request.VerifyEmailOtpRequest;
import org.shaktifdn.registration.response.EmailServiceResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.shaktifdn.registration.util.Utils.handleExternalServiceCallException;

@Service
@Slf4j
@CircuitBreaker(name = "emailService")
public class EmailService extends AbstractWebClient {

    public static final String EMAIL_OTP_REQUEST = "/otp/request";
    public static final String EMAIL_OTP_VERIFY = "/otp/verify";
    public static final String EMAIL_OTP_INQUIRY = "/otp/inquire";
    private static final String RETRIES_LIMIT_REACHED = "Retries limit reached";

    private static final String EMAIL_BLOCKED = "Your email is blocked due to multiple failed attempts. Please try after sometime.";
    private static final String EMAIL_DISPOSABLE = "Your email id is disposable or not valid";
    private static final String OTP_ALREADY_VERIFIED = "OTP already verified";
    private static final String OTP_INVALID = "OTP Invalid";
    private static final String OTP_EXPIRED = "OTP Expired";
    private static final String IS_NOT_A_VERIFIED_EMAIL = " is not a verified Email.";
    private static final String THERE_IS_SOME_ERROR_TO_SEND_OTP_TO_EMAIL = "There is some error to send otp to email ";

    public EmailService(
            @Qualifier("loadBalanced") WebClient.Builder loadBalanced,
            ServiceProperties serviceProperties
    ) {
        super(loadBalanced, serviceProperties);
    }

    public Mono<EmailServiceResponse> sendOtp(String email, String requestedFlow) {
        SendEmailOtpRequest otpRequest = SendEmailOtpRequest
                .builder()
                .email(email)
                .requestedFlow(requestedFlow)
                .build();

        log.info("Sending email otp for user email id: {}", otpRequest.getEmail());

        return post(serviceProperties.getEmailService() + EMAIL_OTP_REQUEST,
                otpRequest, EmailServiceResponse.class)
                .flatMap(response -> handleSendEmailOtpResponse(otpRequest, response))
                .onErrorResume(e -> handleEmailSendOtpError(email, e));
    }

    @NotNull
    private Mono<EmailServiceResponse> handleSendEmailOtpResponse(SendEmailOtpRequest otpRequest, EmailServiceResponse response) {
        if (HttpStatus.OK.value() != response.getCode()) {
            log.error("error response for sending email otp for user email id {} is {}", otpRequest.getEmail(), response);
            switch (response.getCode()) {
                case 423:
                    return Mono.error(new LockedException(RETRIES_LIMIT_REACHED));
                case 429:
                    return Mono.error(
                            new TooManyRequestException(RETRIES_LIMIT_REACHED, response.getPayload())
                    );
                case 406:
                    return Mono.error(
                            new DisposableEmailException(EMAIL_DISPOSABLE)
                    );
                default:
                    return Mono.error(new ShaktiWebClientException(THERE_IS_SOME_ERROR_TO_SEND_OTP_TO_EMAIL
                            + response.getMessage()));
            }
        }
        return Mono.just(response);
    }

    @NotNull
    private Mono<EmailServiceResponse> handleEmailSendOtpError(String email, Throwable e) {
        log.error("error on email send otp for user email id {}", email, e);
        if (e.getMessage().contains("404")) {
            return Mono.error(new RecordNotFoundException(email + IS_NOT_A_VERIFIED_EMAIL));
        } else if (e.getMessage().contains("400")) {
            return Mono.error(new BadRequestException(e.getMessage()));
        } else if (e.getMessage().contains("406")) {
            return Mono.error(new DisposableEmailException(e.getMessage()));
        } else if (e.getMessage().contains("423") || e.getMessage().contains("429")) {
            return Mono.error(new LockedException(e.getMessage()));
        } else {
            if (e instanceof LockedException || e instanceof TooManyRequestException || e instanceof DisposableEmailException) {
                return Mono.error(e);
            }
            return handleExternalServiceCallException(e, "Error during call email otp request: " + e.getMessage());
        }
    }

    public Mono<EmailServiceResponse> verifyOtp(String email, String otp, String requestedFlow) {
        VerifyEmailOtpRequest otpRequest = VerifyEmailOtpRequest.builder()
                .otp(otp)
                .email(email)
                .requestedFlow(requestedFlow)
                .build();

        log.info("verify email otp for user email id  {}", otpRequest.getEmail());

        return post(serviceProperties.getEmailService() + EMAIL_OTP_VERIFY,
                otpRequest, EmailServiceResponse.class)
                .flatMap(verificationResponse -> getEmailVerificationResponse(otpRequest, verificationResponse))
                .onErrorResume(e -> {
                    log.error("error on email verify otp for email: {}", email, e);
                    return handleEmailOtpVerifyFailure(email, e);
                });
    }

    @NotNull
    private Mono<EmailServiceResponse> getEmailVerificationResponse(VerifyEmailOtpRequest otpRequest, EmailServiceResponse verificationResponse) {
        if (200 != verificationResponse.getCode()) {
            log.error("error response for verify email otp for email id  {} is {} ", otpRequest.getEmail(), verificationResponse);
            switch (verificationResponse.getCode()) {
                case 403:
                case 406:
                    return Mono.error(new BadRequestException(OTP_INVALID));
                case 409:
                    return Mono.error(new ConflictRecordsException(OTP_ALREADY_VERIFIED));
                case 410:
                    return Mono.error(new BadRequestException(OTP_EXPIRED));
                case 423:
                    return Mono.error(new LockedException(RETRIES_LIMIT_REACHED));
                default:
                    return Mono.error(
                            new ShaktiWebClientException(THERE_IS_SOME_ERROR_TO_SEND_OTP_TO_EMAIL
                                    + verificationResponse.getMessage()
                            ));
            }
        }
        return Mono.just(verificationResponse);
    }

    @NotNull
    private Mono<EmailServiceResponse> handleEmailOtpVerifyFailure(String email, Throwable e) {
        if (e.getMessage().contains("404")) {
            return Mono.error(new RecordNotFoundException(email + IS_NOT_A_VERIFIED_EMAIL));
        } else if (e.getMessage().contains("400")) {
            return Mono.error(new BadRequestException(e.getMessage()));
        } else if (e.getMessage().contains("409")) {
            return Mono.error(new ConflictRecordsException(OTP_ALREADY_VERIFIED));
        } else if (e.getMessage().contains("423")) {
            return Mono.error(new LockedException(EMAIL_BLOCKED));
        } else {
            if (e instanceof LockedException || e instanceof BadRequestException || e instanceof ConflictRecordsException) {
                return Mono.error(e);
            }
            return handleExternalServiceCallException(e, "Error during email verification process " + e.getMessage());

        }
    }

    public Mono<Boolean> isOtpVerified(String email, String requestedFlow) {
        SendEmailOtpRequest otpRequest = SendEmailOtpRequest
                .builder()
                .email(email)
                .requestedFlow(requestedFlow)
                .build();
        log.info("isOtpVerified request for user email id {}", otpRequest.getEmail());

        return post(
                serviceProperties.getEmailService() + EMAIL_OTP_INQUIRY,
                otpRequest,
                EmailServiceResponse.class
        ).flatMap(verificationResponse -> {
            log.info(
                    "Email: {}, requestedFlow: {} - response: {}",
                    email,
                    requestedFlow,
                    verificationResponse
            );
            if (200 != verificationResponse.getCode()) {
                return Mono.error(new ShaktiWebClientException(THERE_IS_SOME_ERROR_TO_SEND_OTP_TO_EMAIL
                        + verificationResponse.getMessage()));
            }
            boolean isVerified = verificationResponse.getPayload() != null &&
                    verificationResponse.getPayload().get("status") != null &&
                    verificationResponse.getPayload().get("status")
                            .toString().equalsIgnoreCase("VERIFIED");
            log.info("Email: {}, requestedFlow: {} - status: {}", email, requestedFlow, isVerified);
            return Mono.just(isVerified);
        }).onErrorResume(e -> handleExternalServiceCallException(e, "Error during call is email otp Verified service: " + e.getMessage()));
    }
}
