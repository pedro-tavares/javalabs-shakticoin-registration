package org.shaktifdn.registration.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.constant.Message;
import org.shaktifdn.registration.request.EmailVerificationRequest;
import org.shaktifdn.registration.request.VerifyEmailOtpRequest;
import org.shaktifdn.registration.response.EmailVerificationStatus;
import org.shaktifdn.registration.response.ShaktiResponse;
import org.shaktifdn.registration.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.validation.Valid;

import static org.shaktifdn.registration.constant.Constant.EMAIL_REGISTERATION_FLOW;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "emailController", description = "calling to email services APIs")
public class EmailController {

    private final EmailService emailService;

    /**
     * @param request request
     * @return Mono of Response Bean
     * @apiNote Method is used for send OTP to user's primary email
     */
    @PostMapping(value = "/registration")
    public Mono<ShaktiResponse<Object>> sendForRegistration(@RequestBody @Valid EmailVerificationRequest request) {
        log.info("starting with sendOTPToPrimaryEmail for user email id: {} ", request.getEmail());
        return emailService
                .sendOtp(request.getEmail().toLowerCase(), EMAIL_REGISTERATION_FLOW)
                .map(res -> ShaktiResponse.builder().status(true).message(Message.EMAIL_OTP_SENT_SUCCESS).build());
    }

    /**
     * @param request otp to verify
     * @return Mono of Response Bean
     * @apiNote Method is used for verify sent OTP to user's primary email
     */
    @PostMapping(value = "/registration/confirm")
    public Mono<ResponseEntity<ShaktiResponse<EmailVerificationStatus>>> verifyForRegistration(
            @Valid @RequestBody VerifyEmailOtpRequest request
    ) {
        log.info("starting with verifyOTPToPrimaryEmail for use email id {}", request.getEmail());
        return emailService.verifyOtp(request.getEmail().toLowerCase(), request.getOtp(), EMAIL_REGISTERATION_FLOW)
                .map(email -> ResponseEntity
                        .ok(ShaktiResponse
                                .<EmailVerificationStatus>builder()
                                .status(true)
                                .message(Message.EMAIL_VERIFIED_SUCCESS)
                                .data(
                                        EmailVerificationStatus
                                                .builder()
                                                .email(request.getEmail()
                                                ).isVerified(true)
                                                .build()
                                )
                                .build()
                        )
                )
                .switchIfEmpty(Mono.defer(() -> Mono.just(
                        ResponseEntity
                                .badRequest()
                                .body(ShaktiResponse
                                        .<EmailVerificationStatus>builder()
                                        .status(false)
                                        .message(Message.EMAIL_NOT_VERIFIED)
                                        .data(EmailVerificationStatus.builder().isVerified(false).build())
                                        .build()
                                )
                )));
    }

}
