package org.shaktifdn.registration.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.shaktifdn.registration.response.ResponseBean;
import org.shaktifdn.registration.response.ShaktiResponse;
import org.shaktifdn.registration.response.UserRegistrationStatusResponse;
import org.shaktifdn.registration.service.IpAddressService;
import org.shaktifdn.registration.service.UserService;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "userController", description = "calling to user services APIs")
public class UserController {

    private final UserService userService;
    private final IpAddressService ipAddressService;

    /**
     * @param user onboard shakti model
     * @return Mono of Response Bean
     * @apiNote This API will Onboard the user after successfully verified email and mobile. By this user account is created in Shakti EchoSystem
     */
    @PostMapping
    public Mono<ResponseBean> saveOnboardShakti(@RequestBody @Valid OnboardShaktiUserRequest user, ServerHttpRequest serverHttpRequest) {
        String clientIpAddress = ipAddressService.getClientIpAddress(user.getEmail(), serverHttpRequest);
        log.info("Onboard Shakti - save User Method Started for user email id {} ", user.getEmail());
        return userService.saveOnboardShakti(user, clientIpAddress);
    }

    /**
     * Check status of user by email.
     *
     * @param email the email
     * @return the mono of response bean
     */
    @GetMapping(value = "/status")
    public Mono<ShaktiResponse<UserRegistrationStatusResponse>> checkStatusByEmail(@RequestParam("email") String email) {
        log.info("Started checkStatusByEmail for email {}", email);
        return userService.checkEmailIsRegistered(email.toLowerCase())
                .map(userExist -> ShaktiResponse
                        .<UserRegistrationStatusResponse>builder()
                        .status(true)
                        .message(userExist ? "User is already registered" : "User is not registered")
                        .data(UserRegistrationStatusResponse.builder().isRegistered(userExist).build())
                        .build()
                );
    }
}
