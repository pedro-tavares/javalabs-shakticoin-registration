package org.shaktifdn.registration.service;

import gluu.scim2.client.rest.ClientSideService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.gluu.oxtrust.model.scim2.BaseScimResource;
import org.gluu.oxtrust.model.scim2.ListResponse;
import org.gluu.oxtrust.model.scim2.user.UserResource;
import org.shaktifdn.registration.config.GluuProperties;
import org.shaktifdn.registration.exception.BadRequestException;
import org.shaktifdn.registration.exception.ConflictRecordsException;
import org.shaktifdn.registration.exception.RecordNotFoundException;
import org.shaktifdn.registration.exception.ShaktiWebClientException;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.shaktifdn.registration.request.TokenRequest;
import org.shaktifdn.registration.response.TokenResponse;
import org.shaktifdn.registration.util.Utils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static org.shaktifdn.registration.util.Utils.handleExternalServiceCallException;

@Service
@Slf4j
@CircuitBreaker(name = "gluuService")
@Profile("!testMode")
public class GluuService implements GluuServiceApi {

    public static final String GLUU_URL_AUTH_TKEN = "/oxauth/restv1/token";
    public static final String GLUU_URL_IDENTITY = "/scim/restv1";
    public static final String GLUU_URL_IDENTITY2 = "/identity/restv1";
    protected static final String USER_NAME_EQ = "userName eq \"";
    protected static final String USER_ATTRIBUTE_KEY = "urn:ietf:params:scim:schemas:extension:gluu:2.0:User";
    private static final String ATTRIBUTE_NOT_FOUND_FOR_S_AS_USER_NAME = "attribute not found for %s as user name";
    private static final String NO_RECORD_FOUND_FOR_S_AS_USER_NAME = "No record found for %s as user name";
    private static final String GRANT_TYPE = "grant_type";
    private static final String SCOPE = "scope";
    private static final String USERNAME = "username";
    private static final String GLUU_SEARCH_USER_RESPONSE = "Shakti searchUser response status code {} status type {}";

    private final GluuProperties gluuProperties;
    private final ClientSideService client;
    private final Scheduler scheduler;
    private final WebClient.Builder extWebClient;

    public GluuService(
            GluuProperties gluuProperties,
            Scheduler scheduler,
            @Qualifier("scimClient") ClientSideService client,
            @Qualifier("extWebClient") WebClient.Builder extWebClient
    ) {
        this.gluuProperties = gluuProperties;
        this.client = client;
        this.scheduler = scheduler;
        this.extWebClient = extWebClient;
    }

    @Override
    public Mono<TokenResponse> getToken(TokenRequest tokenRequest) {
        log.info("Gluu getToken request for user name : {}", tokenRequest.getUsername());
        return generateToken(
                Utils.encodeBase64(gluuProperties.getGluuClientId(), gluuProperties.getGluuClientSecret()),
                gluuProperties.getGluuUri() + GLUU_URL_AUTH_TKEN,
                tokenRequest,
                TokenResponse.class
        ).onErrorResume(e -> {
            if (e.getMessage().contains("" + HttpStatus.UNAUTHORIZED.value())) {
                return Mono.error(new BadRequestException("incorrect password for user name :" + tokenRequest.getUsername()));
            } else {
                log.error("Gluu get token failed for user name {} : ", tokenRequest.getUsername(), e);
                return handleExternalServiceCallException(e, "Error on Glue getToken service call " + e.getMessage());
            }
        });
    }

    @Override
    public Mono<Response> createUser(OnboardShaktiUserRequest onboardShakti, String ipAddress) {
        log.info("Gluu createUser request: {}", onboardShakti.getEmail());

        return Mono.fromCallable(() -> client.createUser(Utils.createUserModel(onboardShakti, ipAddress), null, null))
                .subscribeOn(scheduler)
                .flatMap(response -> {
                    log.info("created user on gluu: user email id {}, status {}", onboardShakti.getEmail(), response.getStatus());
                    if (response.getStatus() != HttpStatus.CREATED.value())
                        return Mono.error(new ShaktiWebClientException(HttpStatus.valueOf(response.getStatus()) + ""
                                + " Response occurs from " + gluuProperties.getGluuUri() + "/identity/restv1"));
                    else
                        return Mono.just(response);
                })
                .onErrorResume(e -> {
                    log.error("Gluu createUser error for user email :{}  ", onboardShakti.getEmail(), e);
                    if (e.getMessage().contains(HttpStatus.CONFLICT.value() + "")) {
                        return Mono.error(new ConflictRecordsException(onboardShakti.getEmail() + " is already registered in system"));
                    } else {
                        return handleExternalServiceCallException(e, "Error on Shakti create user service call " + e.getMessage());
                    }
                });
    }

    @Override
    public Mono<Response> deleteUser(String email) {
        log.info("Gluu deleteUser request: {}", email);

        return getUser(email)
                .flatMap(fetchedUser ->
                        Mono.fromCallable(() -> client.deleteUser(fetchedUser.getId())).subscribeOn(scheduler)
                )
                .flatMap(response -> {
                    log.info("delete user on gluu: email {}, status {}", email, response.getStatus());
                    if (response.getStatus() != HttpStatus.NO_CONTENT.value() &&
                            response.getStatus() != HttpStatus.NOT_FOUND.value()) {
                        return Mono.error(new ShaktiWebClientException("deleting user in Shakti failed"));
                    } else {
                        return Mono.just(response);
                    }
                })
                .onErrorResume(e -> {
                    if (e instanceof RecordNotFoundException) {
                        return Mono.just(Response.ok().build());
                    }
                    log.error("Gluu deleteUser error for user email :{}  ", email, e);
                    return handleExternalServiceCallException(e, "Error on Shakti delete user service call " + e.getMessage());
                });
    }

    @Override
    public Mono<Boolean> isEmailRegistered(String email) {
        return getUser(email)
                .map(userResource -> true)
                .switchIfEmpty(Mono.just(false))
                .onErrorResume(throwable -> {
                    if (throwable instanceof RecordNotFoundException) {
                        return Mono.just(false);
                    }
                    log.error("Gluu checkExistingUser error for user name {}", email, throwable);
                    return handleExternalServiceCallException(throwable, "Error on Shakti isEmailRegistered service call " + throwable.getMessage());
                });
    }

    @Override
    @SuppressWarnings("SameParameterValue")
    public <T> Mono<T> generateToken(String basicAuthToken, String url, TokenRequest tokenRequest, Class<T> response) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE, tokenRequest.getGrant_type());
        formData.add(SCOPE, tokenRequest.getScope());
        formData.add(USERNAME, tokenRequest.getUsername());
        formData.add("password", tokenRequest.getPassword());

        log.info("HTTP Post request url: {}", url);
        log.info("HTTP Post request data: grant_type {} , scope {} , username {} ", formData.get(GRANT_TYPE), formData.get(SCOPE), formData.get(USERNAME));

        return extWebClient.build()
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuthToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromValue(formData))
                .retrieve()
                .bodyToMono(response);
    }

    @Override
    public Mono<UserResource> getUser(String userName) {
        String filter = USER_NAME_EQ + userName + "\"";
        return Mono.fromCallable(
                        () -> client.searchUsers(filter, 1, 1, null, null, null, null)
                )
                .timeout(Duration.ofSeconds(5), Mono.error(new TimeoutException("Searching users timed out for user: " + userName)))
                .subscribeOn(scheduler)
                .filter(this::checkSearchUserResponseStatusCode)
                .map(response -> {
                    List<BaseScimResource> resources = response.readEntity(ListResponse.class).getResources();
                    if (resources == null || resources.isEmpty()) {
                        throw new RecordNotFoundException(format(NO_RECORD_FOUND_FOR_S_AS_USER_NAME, userName));
                    }
                    log.info("Length of fetched users list size is in search user gluu flow : {} and filter: {} ", resources.size(), filter);
                    UserResource fetchedUser = (UserResource) resources.get(0);
                    if (!fetchedUser.getCustomAttributes()
                            .containsKey(USER_ATTRIBUTE_KEY)) {
                        throw new RecordNotFoundException(format(ATTRIBUTE_NOT_FOUND_FOR_S_AS_USER_NAME, userName));
                    }
                    return fetchedUser;
                })
                .switchIfEmpty(Mono.error(new RecordNotFoundException(format(NO_RECORD_FOUND_FOR_S_AS_USER_NAME, userName))));
    }

    @Override
    public boolean checkSearchUserResponseStatusCode(Response response) {
        log.info(GLUU_SEARCH_USER_RESPONSE, response.getStatus(), response.getStatusInfo());
        log.info("Gluu search user response body type {} ", response.getEntity() != null ? response.getEntity().getClass().getName() : "NULL");
        return response.getStatus() == 200;
    }
}
