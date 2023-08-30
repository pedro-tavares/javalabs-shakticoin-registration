package org.shaktifdn.registration.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.shaktifdn.registration.config.ServiceProperties;
import org.shaktifdn.registration.response.WalletStatusResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.shaktifdn.registration.util.Utils.handleExternalServiceCallException;

@Service
@Slf4j
@CircuitBreaker(name = "kycUserService")
public class KycUserService extends AbstractWebClient {

    public static final String URL_KYC_WALLET = "/kyc/wallet";
    private static final String EXCEPTION_CHECKING_WALLET_EXISTS_FOR_USER_ID = "exception checking wallet exists for user id {}";

    public KycUserService(
            @Qualifier("loadBalanced") WebClient.Builder loadBalanced,
            ServiceProperties serviceProperties
    ) {
        super(loadBalanced, serviceProperties);
    }

    public Mono<Boolean> isWalletExists(String shaktiId) {
        log.info("Checking if wallet is exists for shakti id {} ", shaktiId);
        return get(
                serviceProperties.getKycService() + URL_KYC_WALLET,
                httpHeaders -> {
                    httpHeaders.add("shaktiID", shaktiId);
                },
                WalletStatusResponse.class
        )
                .flatMap(walletStatusResponse -> {
                    if (StringUtils.isBlank(walletStatusResponse.getWalletID())) {
                        log.info("No wallet found for shakti id {}", shaktiId);
                        return Mono.just(false);
                    }
                    return Mono.just(true);
                })
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException) {
                        if (((WebClientResponseException) e).getStatusCode().is4xxClientError()) {
                            log.info("No wallet found for shakti id {}", shaktiId);
                            return Mono.just(false);
                        }
                        log.error(EXCEPTION_CHECKING_WALLET_EXISTS_FOR_USER_ID, shaktiId, e);
                        return handleExternalServiceCallException(e, "Error on kyc user wallet exist service call " + e.getMessage());
                    }
                    log.error(EXCEPTION_CHECKING_WALLET_EXISTS_FOR_USER_ID, shaktiId, e);
                    return handleExternalServiceCallException(e, "Error on kyc user wallet exist service call " + e.getMessage());
                })
                .switchIfEmpty(Mono.just(true));
    }

}
