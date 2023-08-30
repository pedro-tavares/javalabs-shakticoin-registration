package org.shaktifdn.registration.service;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.config.ServiceProperties;
import org.shaktifdn.registration.exception.SelfyIdBadRequestException;
import org.shaktifdn.registration.request.WalletBytesEncryptRequest;
import org.shaktifdn.registration.response.WalletBytesEncryptResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

import static org.shaktifdn.registration.util.Utils.handleExternalServiceCallException;

@Service("selfyIdApi")
@CircuitBreaker(name = "selfyIdService", fallbackMethod = "selfyIdFallback")
@Slf4j
public class SelfyIdService {

    private final WebClient webClient;

    public SelfyIdService(
            @Qualifier("loadBalanced")
                    WebClient.Builder webClient,
            ServiceProperties serviceProperties) {
        this.webClient = webClient
                .baseUrl(serviceProperties.getSelfyIdService() + "/selfyid/encrypt")
                .build();
    }

    public Mono<WalletBytesEncryptResponse> encrypt(WalletBytesEncryptRequest request) {
        return webClient
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        isBadRequest(),
                        clientResponse -> Mono.error(new SelfyIdBadRequestException("Bad request has been sent"))
                )
                .bodyToMono(WalletBytesEncryptResponse.class)
                .onErrorResume(
                        ex -> {
                            log.error("Error on calling Selfie service to encrypt", ex);
                            if (ex instanceof SelfyIdBadRequestException) {
                                return Mono.error(ex);
                            }
                            return handleExternalServiceCallException(ex, "Error on calling Selfie service to encrypt " + ex.getMessage());
                        }
                );
    }

    private Predicate<HttpStatus> isBadRequest() {
        return httpStatus -> httpStatus.value() == HttpStatus.BAD_REQUEST.value();
    }

    @SuppressWarnings("unused")
    public Mono<String> selfyIdFallback(Throwable throwable) {
        log.error("selfyId fallback invocation for error {}", throwable.getMessage());
        return Mono.error(throwable);
    }
}
