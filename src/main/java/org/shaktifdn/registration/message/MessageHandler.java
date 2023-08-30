package org.shaktifdn.registration.message;

import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.model.UserRegisterStateDetail;
import org.shaktifdn.registration.model.UserRegisterStateType;
import org.shaktifdn.registration.repository.UserRegisterStateDetailRepository;
import org.shaktifdn.registration.repository.UserRegisterStateRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@Slf4j
public class MessageHandler {

    @Bean
    public Many<CreateUserMessage> createUserMessageSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<CreateUserMessage>> createUser(Many<CreateUserMessage> createUserMessageSink) {
        return createUserMessageSink::asFlux;
    }

    @Bean
    public Function<Flux<KYCUserCreatedMessage>, Flux<Void>> kycUserCreated(
            UserRegisterStateRepository userRegisterStateRepository,
            UserRegisterStateDetailRepository userRegisterStateDetailRepository
    ) {
        return input -> input
                .flatMap(kycUserCreatedMessage -> {
                    log.info("updating KYC User Created for {}", kycUserCreatedMessage);
                    return userRegisterStateRepository
                            .findByShaktiId(kycUserCreatedMessage.getShaktiId())
                            .flatMap(userRegisterState -> {
                                List<UserRegisterStateDetail> details = new ArrayList<>();
                                details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.KYC_USER_CREATED));
                                if (userRegisterState.isMobileUser()) {
                                    details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.KYC_USER_WALLET_UPDATED));
                                }
                                return userRegisterStateDetailRepository.saveAll(details)
                                        .collectList()
                                        .map(ignore -> userRegisterState);
                            });
                })
                .flatMap(ignore -> Flux.<Void>empty())
                .onErrorResume(throwable -> {
                    log.error("error while processing KYCUserCreatedMessage", throwable);
                    return Flux.empty();
                })
                .onErrorStop();
    }

    @Bean
    public Function<Flux<BountyReferralCreatedMessage>, Flux<Void>> bountyReferralCreated(
            UserRegisterStateRepository userRegisterStateRepository,
            UserRegisterStateDetailRepository userRegisterStateDetailRepository
    ) {
        return input -> input
                .flatMap(message -> {
                    log.info("updating BonusBounty for {}", message);
                    return userRegisterStateRepository
                            .findByShaktiId(message.getShaktiId())
                            .flatMap(userRegisterState ->
                                    userRegisterStateDetailRepository
                                            .save(UserRegisterStateDetail
                                                    .create(userRegisterState, UserRegisterStateType.BOUNTY_CREATED)
                                            )
                            );
                })
                .flatMap(userRegisterState -> Flux.<Void>empty())
                .onErrorResume(throwable -> {
                    log.error("error while processing BountyReferralCreatedMessage", throwable);
                    return Flux.empty();
                })
                .onErrorStop();
    }

    @Bean
    public Function<Flux<SelfyIdCreatedMessage>, Flux<Void>> selfyIdCreated(
            UserRegisterStateRepository userRegisterStateRepository,
            UserRegisterStateDetailRepository userRegisterStateDetailRepository
    ) {
        return input -> input
                .flatMap(selfyIdCreatedMessage -> {
                    log.info("updating SelfyID for {}", selfyIdCreatedMessage);
                    return userRegisterStateRepository
                            .findByShaktiId(selfyIdCreatedMessage.getShaktiId())
                            .flatMap(userRegisterState -> userRegisterStateDetailRepository
                                    .save(UserRegisterStateDetail
                                            .create(userRegisterState, UserRegisterStateType.SELFY_ID_ENCRYPTED)
                                    )
                            );
                })
                .flatMap(userRegisterState -> Flux.<Void>empty())
                .onErrorResume(throwable -> {
                    log.error("error while processing SelfyIdCreatedMessage", throwable);
                    return Flux.empty();
                })
                .onErrorStop();
    }

}
