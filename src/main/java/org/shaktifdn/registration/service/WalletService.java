package org.shaktifdn.registration.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.config.ServiceProperties;
import org.shaktifdn.registration.enums.AccountType;
import org.shaktifdn.registration.exception.RecordNotFoundException;
import org.shaktifdn.registration.exception.WalletAlreadyExistsException;
import org.shaktifdn.registration.model.UserRegisterStateDetail;
import org.shaktifdn.registration.model.UserRegisterStateType;
import org.shaktifdn.registration.repository.UserRegisterStateDetailRepository;
import org.shaktifdn.registration.repository.UserRegisterStateRepository;
import org.shaktifdn.registration.request.CreateWalletRequest;
import org.shaktifdn.registration.request.NewUserWalletAccessRequest;
import org.shaktifdn.registration.request.WalletRequest;
import org.shaktifdn.registration.response.CreateWalletResponse;
import org.shaktifdn.registration.response.PassphraseResponse;
import org.shaktifdn.registration.response.ShaktiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;

import static org.shaktifdn.registration.util.Utils.handleExternalServiceCallException;

@Service
@Slf4j
@CircuitBreaker(name = "walletService")
public class WalletService extends AbstractWebClient {

    public static final String URL_WALLETS = "/wallets";
    public static final String URL_WALLETS_PASSPHRASE = "/wallets/passphrase";
    public static final String URL_WALLET_DEVICE_ACCESS_REGISTRATION = "/wallet/user/device/access/new";

    private final UserInfoClient userInfoClient;
    private final KycUserService kycUserService;
    private final UserRegisterStateRepository userRegisterStateRepository;
    private final UserRegisterStateDetailRepository userRegisterStateDetailRepository;
    private final Scheduler scheduler;
    private final WebClient.Builder loadBalancedSameBearerToken;

    public WalletService(
            @Qualifier("loadBalanced") WebClient.Builder loadBalanced,
            @Qualifier("loadBalancedSameBearerToken") WebClient.Builder loadBalancedSameBearerToken,
            ServiceProperties serviceProperties,
            UserInfoClient userInfoClient,
            KycUserService kycUserService,
            UserRegisterStateRepository userRegisterStateRepository,
            UserRegisterStateDetailRepository userRegisterStateDetailRepository,
            Scheduler scheduler
    ) {
        super(loadBalanced, serviceProperties);

        this.userInfoClient = userInfoClient;
        this.kycUserService = kycUserService;
        this.userRegisterStateRepository = userRegisterStateRepository;
        this.userRegisterStateDetailRepository = userRegisterStateDetailRepository;
        this.scheduler = scheduler;
        this.loadBalancedSameBearerToken = loadBalancedSameBearerToken;
    }

    public Mono<CreateWalletResponse> create(WalletRequest walletRequest) {
        return userInfoClient.getShaktiId()
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RecordNotFoundException("User not found for wallet creation"))))
                .zipWhen(userDetail -> kycUserService.isWalletExists(userDetail.getShaktiId()))
                .map(tuple -> {
                    if (tuple.getT2()) {
                        throw new WalletAlreadyExistsException("wallet already exists");
                    }
                    return tuple.getT1();
                })
                .flatMap(userDetail -> {
                    log.info("creating wallet for ShaktiID: {}", userDetail.getShaktiId());
                    return create(
                            CreateWalletRequest
                                    .builder()
                                    .shaktiID(userDetail.getShaktiId())
                                    .accountType(AccountType.PERSONAL)
                                    .authorizationBytes(walletRequest.getAuthorizationBytes())
                                    .passphrase(walletRequest.getPassphrase())
                                    .build()
                    )
                            .publishOn(scheduler)
                            .doOnNext(response -> {
                                log.info("create UserRegisterState for user: {}", userDetail);
                                userRegisterStateRepository
                                        .findByShaktiId(userDetail.getShaktiId())
                                        .flatMap(userRegisterState -> {
                                            List<UserRegisterStateDetail> details = new ArrayList<>();
                                            details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.WALLET_CREATED));
                                            details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.KYC_USER_WALLET_UPDATED));
                                            return userRegisterStateDetailRepository
                                                    .saveAll(details)
                                                    .collectList()
                                                    .map(ignore -> userRegisterState);
                                        })
                                        .subscribe();
                            });
                });
    }

    public Mono<String> userDeviceAccessRegister(NewUserWalletAccessRequest request) {
        log.info("Registering device access: {}", request);
        return post(
                loadBalancedSameBearerToken,
                serviceProperties.getWalletService() + URL_WALLET_DEVICE_ACCESS_REGISTRATION,
                request,
                String.class
        ).switchIfEmpty(Mono.defer(() -> {
            log.debug("no content from User device Access registration: {}", request);
            return Mono.just("");
        })).onErrorResume(e -> {
            log.error("error wallet device access register {}", e.getMessage());
            return handleExternalServiceCallException(e, "Error on wallet device access register service call " + e.getMessage());
        });
    }

    private Mono<CreateWalletResponse> create(CreateWalletRequest createWalletRequest) {
        log.info("Create wallet for  user shakti id : {}", createWalletRequest.getShaktiID());
        return get(
                serviceProperties.getWalletService() + URL_WALLETS_PASSPHRASE,
                new ParameterizedTypeReference<ShaktiResponse<PassphraseResponse>>() {
                }
        )
                .flatMap(response -> {
                    log.info("got passphrase generated, creating wallet");
                    createWalletRequest.setPassphrase(response.getData().getPassphrase());
                    return post(serviceProperties.getWalletService() + URL_WALLETS,
                            createWalletRequest, CreateWalletResponse.class);
                })

                .onErrorResume(e -> {
                    log.error("error creating wallet {}", e.getMessage());
                    return handleExternalServiceCallException(e, "Error on wallet createt service call " + e.getMessage());
                });
    }

}
