package org.shaktifdn.registration.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.exception.UnauthorizedException;
import org.shaktifdn.registration.response.UserDetail;
import org.shaktifdn.registration.security.SecurityUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static org.shaktifdn.registration.util.Utils.handleExternalServiceCallException;

@Service
@Slf4j
@AllArgsConstructor
public class UserInfoClient {
    private static final String SHAKTI_ID = "shaktiID";
    private static final String EMAIL = "email";
    private static final String MOBILE_NUMBER = "phone_mobile_number";
    private static final String CLIENT_ID = "gluu";
    private final DefaultReactiveOAuth2UserService defaultReactiveOAuth2UserService
            = new DefaultReactiveOAuth2UserService();
    private final ReactiveClientRegistrationRepository clientRegistrations;

    public Mono<UserDetail> getShaktiId() {
        return SecurityUtils.getAuthentication()
                .map(authentication -> (JwtAuthenticationToken) authentication)
                .flatMap(jwtAuthenticationToken ->
                        clientRegistrations.findByRegistrationId(CLIENT_ID)
                                .map(clientRegistration ->
                                        geAuth2UserRequest(jwtAuthenticationToken, clientRegistration))
                                .map(defaultReactiveOAuth2UserService::loadUser)
                                .flatMap(oAuth2UserMono -> oAuth2UserMono.map(OAuth2User::getAttributes))
                                .map(this::toUserDetail)
                                .onErrorResume(throwable -> {
                                    log.error("User not found in Glue database", throwable);
                                    return Mono.error(
                                            () -> new UnauthorizedException("User not found in Glue database")
                                    );
                                })
                )
                .switchIfEmpty(Mono.error(new UnauthorizedException("authentication not found")))
                .onErrorResume(throwable -> {
                    log.error("Get Shakti id service call failure ", throwable);
                    return handleExternalServiceCallException(throwable, "Error on getShaktiId service call " + throwable.getMessage());
                });
    }

    private OAuth2UserRequest geAuth2UserRequest(
            JwtAuthenticationToken jwtAuthenticationToken,
            ClientRegistration clientRegistration) {
        return new OAuth2UserRequest(
                clientRegistration,
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        jwtAuthenticationToken.getToken().getTokenValue(),
                        jwtAuthenticationToken.getToken().getIssuedAt(),
                        jwtAuthenticationToken.getToken().getExpiresAt())
        );
    }

    private UserDetail toUserDetail(java.util.Map<String, Object> userAttributesMap) {
        return UserDetail
                .builder()
                .shaktiId(Objects.requireNonNull(userAttributesMap.get(SHAKTI_ID)).toString())
                .email(Objects.requireNonNull(userAttributesMap.get(EMAIL)).toString())
                .mobileNo(Objects.toString(userAttributesMap.get(MOBILE_NUMBER)))
                .build();
    }
}