package org.shaktifdn.registration.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.shaktifdn.registration.config.ServiceProperties;
import org.shaktifdn.registration.exception.*;
import org.shaktifdn.registration.request.SendMobileOtpRequest;
import org.shaktifdn.registration.request.VerificationMobileOtpRequest;
import org.shaktifdn.registration.request.VerifyMobileOtpRequest;
import org.shaktifdn.registration.response.SmsServiceResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.shaktifdn.registration.util.Utils.handleExternalServiceCallException;

@Service
@Slf4j
@CircuitBreaker(name = "smsService", fallbackMethod = "getMobileFallback")
public class MobileService extends AbstractWebClient {

    public static final String MOBILE_OTP_REQUEST = "/otp/request";
    public static final String MOBILE_OTP_VERIFY = "/otp/verify";
    public static final String MOBILE_OTP_INQUIRY = "/inquiry/mobile";
    private static final String RETRIES_LIMIT_REACHED = "Retries limit reached";
    private static final String FAILED_WITH_ERROR_RESPONSE_FOR_MOBILE_NUMBER = "Mobile otp flow {} is failed with error response: {} for mobile number {} ";

    public MobileService(
            @Qualifier("loadBalanced") WebClient.Builder loadBalanced,
            ServiceProperties serviceProperties
    ) {
        super(loadBalanced, serviceProperties);
    }

    public Mono<SmsServiceResponse> send(String countryCode, String mobileNo, String requestedFlow) {
        log.info(
                "sending OTP for countryCode: {}, mobileNo: {},  requestedFlow: {}",
                countryCode, mobileNo, requestedFlow
        );
        SendMobileOtpRequest sendMobileOtpRequest = SendMobileOtpRequest
                .builder()
                .countryCode(countryCode)
                .mobileNo(mobileNo)
                .requestedFlow(requestedFlow)
                .build();

        return post(serviceProperties.getSmsService() + MOBILE_OTP_REQUEST,
                sendMobileOtpRequest, SmsServiceResponse.class)
                .flatMap(response -> {
                    if (200 != response.getCode()) {
                        log.error(FAILED_WITH_ERROR_RESPONSE_FOR_MOBILE_NUMBER,requestedFlow, response,mobileNo);
                        switch (response.getCode()) {
                            case 423:
                                return Mono.error(new LockedException(RETRIES_LIMIT_REACHED));
                            case 429:
                                return Mono.error(
                                        new TooManyRequestException(RETRIES_LIMIT_REACHED, response.getPayload())
                                );
                            default:
                                return Mono.error(new ShaktiWebClientException("There is some error to send otp to mobile: "
                                        + response.getMessage()));
                        }
                    }
                    log.info("mobile otp sms service send call is done: {}, {}", countryCode, mobileNo);
                    return Mono.just(response);
                })
                .onErrorResume(e -> handleSmsOtpSendError(mobileNo, e));
    }

    @NotNull
    private Mono<SmsServiceResponse> handleSmsOtpSendError(String mobileNo, Throwable e) {
        log.error(
                "mobile otp sms service send call failed for mobile number {} with error: ",
                mobileNo,
                e
        );
        if (e.getMessage().contains("404")) {
            return Mono.error(new RecordNotFoundException(mobileNo + " is not a verified mobile."));
        } else if (e.getMessage().contains("400")) {
            return Mono.error(new BadRequestException(e.getMessage()));
        } else {
            if (e instanceof LockedException | e instanceof TooManyRequestException) {
                return Mono.error(e);
            }
            return handleExternalServiceCallException(e, "Error on calling sending SMS OTP service  " + e.getMessage());
        }
    }

    public Mono<SmsServiceResponse> verify(
            String countryCode, String mobileNo, String otp, String requestedFlow
    ) {
        log.info(
                "verifying OTP for countryCode: {}, mobileNo: {}, requestedFlow: {}",
                countryCode, mobileNo, requestedFlow
        );
        VerifyMobileOtpRequest verifyMobileOtpRequest = VerifyMobileOtpRequest
                .builder()
                .countryCode(countryCode)
                .mobileNo(mobileNo)
                .otp(otp)
                .requestedFlow(requestedFlow)
                .build();

        return post(serviceProperties.getSmsService() + MOBILE_OTP_VERIFY,
                verifyMobileOtpRequest, SmsServiceResponse.class)
                .flatMap(verificationResponse -> {
                    if (200 != verificationResponse.getCode()) {
                        log.error(FAILED_WITH_ERROR_RESPONSE_FOR_MOBILE_NUMBER,requestedFlow, verificationResponse, mobileNo);
                        switch (verificationResponse.getCode()) {
                            case 403:
                            case 406:
                                return Mono.error(new BadRequestException("OTP Invalid"));
                            case 409:
                                return Mono.error(new ConflictRecordsException("OTP already verified"));
                            case 410:
                                return Mono.error(new BadRequestException("OTP Expired"));
                            case 423:
                                return Mono.error(new LockedException(RETRIES_LIMIT_REACHED));
                            default:
                                return Mono.error(new ShaktiWebClientException("Failed to verifying otp to mobile :"
                                        + verificationResponse.getMessage()));
                        }
                    }
                    log.info("OTP verified: {}, {}", countryCode, mobileNo);
                    return Mono.just(verificationResponse);
                })
                .onErrorResume(e -> handleSmsOtpVerificationError(mobileNo, e));
    }

    @NotNull
    private Mono<SmsServiceResponse> handleSmsOtpVerificationError(String mobileNo, Throwable e) {
        log.error("VerifyOTPToMobile is failed for mobile number {}  with error {}", mobileNo, e.getMessage());
        if (e.getMessage().contains("404")) {
            return Mono.error(new RecordNotFoundException(mobileNo + " is not a verified mobile."));
        } else if (e.getMessage().contains("400")) {
            return Mono.error(new BadRequestException(e.getMessage()));
        } else {
            if (e instanceof BadRequestException | e instanceof ConflictRecordsException | e instanceof LockedException) {
                return Mono.error(e);
            }
            return handleExternalServiceCallException(e, "Error on calling verifying SMS OTP service  " + e.getMessage());
        }
    }

    public Mono<SmsServiceResponse> inquire(String mobileNo, String countryCode, String requestedFlow) {
        VerificationMobileOtpRequest verificationMobileOtpRequest =
                VerificationMobileOtpRequest
                        .builder()
                        .mobileNo(mobileNo)
                        .countryCode(countryCode)
                        .requestedFlow(requestedFlow)
                        .build();
        log.info("checking mobile status in sms service for mobile number : {}", mobileNo);

        return post(serviceProperties.getSmsService() + MOBILE_OTP_INQUIRY,
                verificationMobileOtpRequest, SmsServiceResponse.class)
                .flatMap(verificationResponse -> {
                    if (200 != verificationResponse.getCode()) {
                        log.error(FAILED_WITH_ERROR_RESPONSE_FOR_MOBILE_NUMBER, requestedFlow, verificationResponse, mobileNo);
                        if (verificationResponse.getCode() == 400) {
                            return Mono.error(new BadRequestException("OTP Invalid"));
                        }
                        return Mono.error(new ShaktiWebClientException("Failed to verifying otp to mobile :"
                                + verificationResponse.getMessage()));
                    }
                    log.info("OTP verified: {}, {}", countryCode, mobileNo);
                    return Mono.just(verificationResponse);
                })
                .onErrorResume(e -> handleSmsOtpInquiryError(mobileNo, e));
    }

    @NotNull
    private Mono<SmsServiceResponse> handleSmsOtpInquiryError(String mobileNo, Throwable e) {
        log.error(
                "checking mobile status failed for mobile number {} with error : ",
                mobileNo,
                e
        );
        if (e.getMessage().contains("404"))
            return Mono.error(new RecordNotFoundException(mobileNo + " is not a verified number"));
        else if (e.getMessage().contains("410"))
            return Mono.error(
                    new RecordNotFoundException(
                            mobileNo + e.getMessage().replace("410", " ")
                    )
            );
        else if (e.getMessage().contains("400")) {
            return Mono.error(new BadRequestException(e.getMessage()));
        } else {
            if (e instanceof BadRequestException) {
                return Mono.error(e);
            }
            return handleExternalServiceCallException(e, "Error on calling inquire SMS OTP service  " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public Mono<Boolean> getMobileFallback(Throwable throwable) {
        log.error("Mobile service fallback invocation", throwable);
        return Mono.error(throwable);
    }
}
