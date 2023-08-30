package org.shaktifdn.registration.config;

import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.response.UserDetail;
import org.shaktifdn.registration.service.UserInfoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.ReactiveAuditorAware;
import reactor.core.publisher.Mono;

;

@Slf4j
public class NativeAuditorAware implements ReactiveAuditorAware<String> {

    @Autowired
    private UserInfoClient client;

    @Override
    public Mono<String> getCurrentAuditor() {
        return client.getShaktiId()
                .map(UserDetail::getShaktiId)
                .onErrorResume(throwable -> {
                    log.warn("unable to get current user so using System as a user {}", throwable.getMessage());
                    return Mono.just("System");
                });
    }

}
