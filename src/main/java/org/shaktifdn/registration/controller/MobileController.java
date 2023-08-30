package org.shaktifdn.registration.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.constant.Message;
import org.shaktifdn.registration.request.ConfirmMobileOtp;
import org.shaktifdn.registration.request.RegisterMobile;
import org.shaktifdn.registration.response.MobileVerificationStatus;
import org.shaktifdn.registration.response.ShaktiResponse;
import org.shaktifdn.registration.service.MobileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import static org.shaktifdn.registration.constant.Constant.MOBILE_REGISTERATION_FLOW;

@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "mobileController", description = "calling to Sms service for mobile number registration and verification")
public class MobileController {

    private final MobileService mobileService;

    /**
     * @param registerMobileEvent request
     * @apiNote Method is used for send OTP to user's mobile
     */
    @PostMapping(value = "/registration")
    public Mono<ShaktiResponse<Object>> sendForRegistration(@RequestBody @Valid RegisterMobile registerMobileEvent) {
        log.info("Starting sending OTP to mobile {}", registerMobileEvent.getMobileNo());
        return mobileService
                .send(
                        registerMobileEvent.getCountryCode(),
                        registerMobileEvent.getMobileNo(),
                        MOBILE_REGISTERATION_FLOW
                )
                .map(res -> ShaktiResponse.builder().status(true).message(Message.MOBILE_OTP_SENT_SUCCESS).build());
    }

    /**
     * @param confirmMobileOtp request
     * @apiNote Method is used for send OTP to user's mobile
     */
    @PostMapping(value = "/registration/confirm")
    public Mono<ResponseEntity<ShaktiResponse<MobileVerificationStatus>>> confirmForRegistration(
            @RequestBody @Valid ConfirmMobileOtp confirmMobileOtp
    ) {
        log.info("Starting confirming OTP to mobile {}", confirmMobileOtp.getMobileNo());
        return mobileService
                .verify(
                        confirmMobileOtp.getCountryCode(),
                        confirmMobileOtp.getMobileNo(),
                        confirmMobileOtp.getOtp(),
                        MOBILE_REGISTERATION_FLOW
                )
                .map(response -> ResponseEntity
                        .status(response.getCode())
                        .body(ShaktiResponse
                                .<MobileVerificationStatus>builder()
                                .status(response.getCode() == HttpStatus.OK.value())
                                .message(
                                        response.getCode() == HttpStatus.OK.value() ?
                                                Message.MOBILE_OTP_VERIFIED_SUCCESS :
                                                response.getMessage()
                                )
                                .data(MobileVerificationStatus
                                        .builder()
                                        .isVerified(response.getCode() == HttpStatus.OK.value())
                                        .build()
                                )
                                .build()
                        )
                );
    }

}
