package org.shaktifdn.registration.service;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.config.ServiceProperties;
import org.shaktifdn.registration.response.ShaktiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;

import static org.shaktifdn.registration.util.Utils.handleExternalServiceCallException;

@Service
@Slf4j
@CircuitBreaker(name = "bizVaultService")
public class BizVaultService extends AbstractWebClient {

    public static final String BIZVAULTS_EMAIL_REGISTRATION_STATUS = "/bizvaults/email/registration/status";

    public BizVaultService(
            @Qualifier("loadBalanced") WebClient.Builder loadBalanced,
            ServiceProperties serviceProperties
    ) {
        super(loadBalanced, serviceProperties);
    }

    public Mono<Boolean> isEmailRegistered(String email) {
        log.info("checking email registration status for email: {}", email);

        return loadBalanced.build()
                .get()
                .uri(serviceProperties.getBizVaultService() +
                        BIZVAULTS_EMAIL_REGISTRATION_STATUS + "?email=" + email.toLowerCase())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ShaktiResponse<LinkedHashMap<String, Object>>>() {
                })
                .map(response -> response.getStatus() && (Boolean) response.getData().get("bizVaultRegistered"))
                .onErrorResume(e -> {
                    log.error("Error checking BizVault email status", e);
                    return handleExternalServiceCallException(e, "Error on checking isEmail Registered on bizvault service: " + e.getMessage());
                });
    }

}
