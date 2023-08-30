package org.shaktifdn.registration.service;

import com.couchbase.client.java.query.QueryStatus;
import com.couchbase.client.java.query.ReactiveQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.shaktifdn.registration.model.UserRegisterState;
import org.shaktifdn.registration.model.UserRegisterStateType;
import org.shaktifdn.registration.repository.UserRegisterStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.couchbase.core.ReactiveCouchbaseTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationFailedService {

    private static final String DELETE_KYC_USER = "DELETE FROM `services` where `_type`=\"kycuser\" and `shaktiID`=\"?\"";
    private static final String DELETE_SELFY_ID = "DELETE FROM `services` where `_type`=\"selfy_id\" and `shaktiID`=\"?\"";
    private static final String DELETE_BOUNTY = "DELETE FROM `services` where `_type`=\"genesis_bonus_bounty\" and `shaktiID`=\"?\"";
    private static final String DELETE_USER_REGISTER_STATE_DETAIL = "DELETE FROM `services` where `_type`=\"UserRegisterStateDetail\" and `userRegisterStateId`=\"?\"";
    private static final String DELETE_USER_REGISTER_STATE = "DELETE FROM `services` where `_type`=\"UserRegisterState\" and `shaktiId`=\"?\"";
    private static final String DELETE_FROM_NOTIFICATION = "DELETE FROM `services` where `_type`=\"UserToken\" and `shaktiId`=\"?\"";

    private final UserRegisterStateRepository userRegisterStateRepository;
    private final Scheduler scheduler;
    private final ReactiveCouchbaseTemplate couchbaseTemplate;
    private final GluuServiceApi gluuService;

    @Value("${user.registration.failed.cleanup-after}")
    private Duration afterDuration;

    @Scheduled(fixedRateString = "${user.registration.failed.cleanup-after}", initialDelay = 60000)
    @Async
    public void cleanupCron() {
        cleanup()
                .subscribeOn(scheduler)
                .subscribe();
    }

    public Mono<Boolean> cleanup() {
        log.info("cleaning up 'failed' User Registrations");
        var beforeTime = OffsetDateTime.now().minus(afterDuration);
        log.info("getting all failed before: {}", beforeTime);

        return userRegisterStateRepository
                .findByIncomplete(
                        beforeTime.toInstant(),
                        UserRegisterStateType.values().length
                )
                .flatMap(this::cleanup)
                .collectList()
                .map(list -> true);
    }

    private Mono<Boolean> cleanup(UserRegisterState userRegisterState) {
        log.info("cleaning up: {}", userRegisterState);
        return Mono.zip(
                        gluuService.deleteUser(userRegisterState.getEmail()).map(response -> true),
                        executeQuery(DELETE_KYC_USER.replaceAll("\\?", userRegisterState.getShaktiId())),
                        executeQuery(DELETE_SELFY_ID.replaceAll("\\?", userRegisterState.getShaktiId())),
                        executeQuery(DELETE_BOUNTY.replaceAll("\\?", userRegisterState.getShaktiId())),
                        executeQuery(DELETE_FROM_NOTIFICATION.replaceAll("\\?", userRegisterState.getShaktiId())),
                        executeQuery(DELETE_USER_REGISTER_STATE_DETAIL.replaceAll("\\?", userRegisterState.getId()))
                )
                .flatMap(tuple -> {
                    log.info("status: {}", tuple);
                    if (tuple.getT1() && tuple.getT2() && tuple.getT3() && tuple.getT4() && tuple.getT5()) {
                        return executeQuery(
                                DELETE_USER_REGISTER_STATE.replaceAll("\\?",
                                        userRegisterState.getShaktiId())
                        );
                    } else {
                        log.error("failed deleting:{}", userRegisterState);
                        return Mono.just(false);
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("error on deleting failed registation: {}", userRegisterState, throwable);
                    return Mono.just(false);
                });
    }

    @NotNull
    private Mono<Boolean> executeQuery(String query) {
        return couchbaseTemplate
                .getCouchbaseClientFactory()
                .getCluster()
                .reactive()
                .query(query)
                .flatMap(ReactiveQueryResult::metaData)
                .map(metaData -> {
                    log.trace("result: {} - {}", query, metaData.status());
                    return metaData.status() == QueryStatus.SUCCESS;
                });
    }
}
