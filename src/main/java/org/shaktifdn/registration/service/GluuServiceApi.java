package org.shaktifdn.registration.service;

import org.gluu.oxtrust.model.scim2.user.UserResource;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.shaktifdn.registration.request.TokenRequest;
import org.shaktifdn.registration.response.TokenResponse;
import reactor.core.publisher.Mono;

import javax.ws.rs.core.Response;

public interface GluuServiceApi {
    Mono<TokenResponse> getToken(TokenRequest tokenRequest);

    Mono<Response> createUser(OnboardShaktiUserRequest onboardShakti, String ipAddress);

    Mono<Response> deleteUser(String email);

    Mono<Boolean> isEmailRegistered(String email);

    @SuppressWarnings("SameParameterValue")
    <T> Mono<T> generateToken(String basicAuthToken, String url, TokenRequest tokenRequest, Class<T> response);

    Mono<UserResource> getUser(String userName);

    boolean checkSearchUserResponseStatusCode(Response response);
}
