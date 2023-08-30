package org.shaktifdn.registration.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.shaktifdn.registration.AbstractTest;
import org.shaktifdn.registration.constant.Constant;
import org.shaktifdn.registration.constant.Message;
import org.shaktifdn.registration.exception.BadRequestException;
import org.shaktifdn.registration.request.ConfirmMobileOtp;
import org.shaktifdn.registration.request.RegisterMobile;
import org.shaktifdn.registration.response.SmsServiceResponse;
import org.shaktifdn.registration.service.MobileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.shaktifdn.registration.constant.Constant.MOBILE_REGISTERATION_FLOW;

@WebFluxTest(MobileController.class)
class MobileControllerTest extends AbstractTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private MobileService mobileService;

    @Test
    void sendOTPToSmsEvent() {
        //given
        RegisterMobile registerMobileEvent = RegisterMobile
                .builder()
                .mobileNo("7676767676")
                .countryCode("+44")
                .requestedFlow(Constant.MOBILE_NO_SERVICE)
                .build();
        when(mobileService.send(
                eq("+44"),
                eq("7676767676"),
                eq(MOBILE_REGISTERATION_FLOW)
        )).thenReturn(Mono.just(SmsServiceResponse.builder().build()));
        //when
        webClient
                .post()
                .uri("/sms/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerMobileEvent)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.message").isEqualTo(Message.MOBILE_OTP_SENT_SUCCESS);
        //then
        verify(mobileService, Mockito.atLeastOnce())
                .send(
                        eq("+44"),
                        eq("7676767676"),
                        eq(MOBILE_REGISTERATION_FLOW)
                );
    }

    @Test
    void sendOTPToSmsEvent_failed() {
        //given
        RegisterMobile registerMobileEvent = RegisterMobile
                .builder()
                .mobileNo("7676767676")
                .countryCode("+44")
                .requestedFlow(Constant.MOBILE_NO_SERVICE)
                .build();
        when(mobileService.send(
                eq("+44"),
                eq("7676767676"),
                eq(MOBILE_REGISTERATION_FLOW)
        )).thenReturn(Mono.just(SmsServiceResponse.builder().build()));
        //when
        webClient
                .post()
                .uri("/sms/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerMobileEvent)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.message").isEqualTo(Message.MOBILE_OTP_SENT_SUCCESS);
        //then
        verify(mobileService, Mockito.atLeastOnce()).send(
                eq("+44"),
                eq("7676767676"),
                eq(MOBILE_REGISTERATION_FLOW)
        );
    }

    @Test
    void verifyMobileOtp() {
        //given
        ConfirmMobileOtp confirmMobileEvent = ConfirmMobileOtp
                .builder().mobileNo("7676767676")
                .countryCode("+44")
                .otp("12345")
                .requestedFlow(Constant.MOBILE_NO_SERVICE)
                .build();
        when(mobileService.verify(
                eq(confirmMobileEvent.getCountryCode()),
                eq(confirmMobileEvent.getMobileNo()),
                eq(confirmMobileEvent.getOtp()),
                eq(MOBILE_REGISTERATION_FLOW)
        )).thenReturn(Mono.just(SmsServiceResponse.builder().code(HttpStatus.OK.value()).build()));
        //when
        webClient
                .post()
                .uri("/sms/registration/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(confirmMobileEvent)
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.message").isEqualTo(Message.MOBILE_OTP_VERIFIED_SUCCESS)
                .jsonPath("$.data.isVerified").isEqualTo(true);
        ;
        //then
        verify(mobileService, Mockito.atLeastOnce())
                .verify(
                        eq(confirmMobileEvent.getCountryCode()),
                        eq(confirmMobileEvent.getMobileNo()),
                        eq(confirmMobileEvent.getOtp()),
                        eq(MOBILE_REGISTERATION_FLOW)
                );
    }

    @Test
    void verifyMobileOtp_failed() {
        //given
        ConfirmMobileOtp confirmMobileEvent = ConfirmMobileOtp
                .builder().mobileNo("7676767676")
                .countryCode("+44")
                .otp("12345")
                .requestedFlow(Constant.MOBILE_NO_SERVICE)
                .build();
        when(mobileService.verify(
                eq(confirmMobileEvent.getCountryCode()),
                eq(confirmMobileEvent.getMobileNo()),
                eq(confirmMobileEvent.getOtp()),
                eq(MOBILE_REGISTERATION_FLOW)
        )).thenReturn(Mono.error(new BadRequestException("error")));
        //when
        webClient
                .post()
                .uri("/sms/registration/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(confirmMobileEvent)
                .exchange().expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
        ;
        //then
        verify(mobileService, Mockito.atLeastOnce())
                .verify(
                        eq(confirmMobileEvent.getCountryCode()),
                        eq(confirmMobileEvent.getMobileNo()),
                        eq(confirmMobileEvent.getOtp()),
                        eq(MOBILE_REGISTERATION_FLOW)
                );
    }

    @Test
    void verifyMobileOtp_bad_request() {
        //given
        ConfirmMobileOtp confirmMobileEvent = ConfirmMobileOtp
                .builder()
                .build();
        //when
        webClient
                .post()
                .uri("/sms/registration/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(confirmMobileEvent)
                .exchange().expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
        ;
        //then
        verifyNoInteractions(mobileService);
    }

}