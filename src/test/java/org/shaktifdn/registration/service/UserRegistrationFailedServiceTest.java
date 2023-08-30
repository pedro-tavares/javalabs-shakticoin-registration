package org.shaktifdn.registration.service;

import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.shaktifdn.registration.AbstractIntegrationTest;
import org.shaktifdn.registration.model.UserRegisterState;
import org.shaktifdn.registration.model.UserRegisterStateDetail;
import org.shaktifdn.registration.model.UserRegisterStateType;
import org.shaktifdn.registration.repository.UserRegisterStateDetailRepository;
import org.shaktifdn.registration.repository.UserRegisterStateRepository;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@Slf4j
class UserRegistrationFailedServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UserRegisterStateRepository userRegisterStateRepository;

    @Autowired
    private UserRegisterStateDetailRepository userRegisterStateDetailRepository;

    @Autowired
    private UserRegistrationFailedService service;

    @Autowired
    private CouchbaseTemplate couchbaseTemplate;

    @MockBean
    private GluuServiceApi gluuService;

    @Value("${user.registration.failed.cleanup-after}")
    private Duration afterDuration;

    @Test
    void cleanup() {
        assertThat(executeQuery("DELETE FROM `services` s ").metaData().status()).isEqualTo(QueryStatus.SUCCESS);
        // Given
        UserRegisterState userRegisterState = UserRegisterState
                .create(
                        OnboardShaktiUserRequest.builder().shaktiID(UUID.randomUUID().toString()).build(),
                        true
                );
        List<UserRegisterStateDetail> details = new ArrayList<>();
        details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.GLUU_CREATED));
        details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.KYC_USER_CREATED));
        details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.KYC_USER_WALLET_UPDATED));
        details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.WALLET_CREATED));

        userRegisterState.setLastModification(OffsetDateTime.now().minus(afterDuration).minusMinutes(1).toInstant());
        assertThat(insert(UUID.randomUUID().toString(), "kycuser", userRegisterState.getShaktiId()))
                .isEqualTo(QueryStatus.SUCCESS);
        assertThat(insert(UUID.randomUUID().toString(), "selfy_id", userRegisterState.getShaktiId()))
                .isEqualTo(QueryStatus.SUCCESS);
        assertThat(insert(UUID.randomUUID().toString(), "genesis_bonus_bounty", userRegisterState.getShaktiId()))
                .isEqualTo(QueryStatus.SUCCESS);

        StepVerifier.create(userRegisterStateRepository.save(userRegisterState))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
        StepVerifier.create(userRegisterStateDetailRepository.saveAll(details))
                .expectNextMatches(Objects::nonNull)
                .expectNextMatches(Objects::nonNull)
                .expectNextMatches(Objects::nonNull)
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
        when(gluuService.deleteUser(userRegisterState.getEmail())).thenReturn(Mono.just(Response.ok().build()));

        await().atMost(3, TimeUnit.SECONDS)
                .with()
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> true);
        assertThat(
                executeQuery("SELECT s.* FROM `services` s ")
                        .rowsAsObject().size()
        ).isEqualTo(8);

        // When
        StepVerifier.create(service.cleanup()).expectNext(true).verifyComplete();

        await().atMost(3, TimeUnit.SECONDS)
                .with()
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> true);

        // Then
        assertThat(executeQuery("SELECT s.* FROM `services` s ").rowsAsObject().isEmpty()).isEqualTo(true);
        verify(gluuService).deleteUser(userRegisterState.getEmail());
    }

    @Test
    void shouldNotCleanUpDueToTimestamp() {
        assertThat(executeQuery("DELETE FROM `services` s ").metaData().status()).isEqualTo(QueryStatus.SUCCESS);
        // Given
        UserRegisterState userRegisterState = UserRegisterState
                .create(
                        OnboardShaktiUserRequest.builder()
                                .shaktiID(UUID.randomUUID().toString())
                                .build(),
                        true
                );
        userRegisterState.setLastModification(OffsetDateTime.now().minusMinutes(1).toInstant());
        assertThat(insert(UUID.randomUUID().toString(), "kycuser", userRegisterState.getShaktiId()))
                .isEqualTo(QueryStatus.SUCCESS);
        assertThat(insert(UUID.randomUUID().toString(), "selfy_id", userRegisterState.getShaktiId()))
                .isEqualTo(QueryStatus.SUCCESS);
        assertThat(insert(UUID.randomUUID().toString(), "genesis_bonus_bounty", userRegisterState.getShaktiId()))
                .isEqualTo(QueryStatus.SUCCESS);

        StepVerifier.create(userRegisterStateRepository.save(userRegisterState))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
        when(gluuService.deleteUser(userRegisterState.getEmail())).thenReturn(Mono.just(Response.ok().build()));

        await().atMost(3, TimeUnit.SECONDS)
                .with()
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> true);
        assertThat(
                executeQuery("SELECT s.* FROM `services` s ")
                        .rowsAsObject().size()
        ).isEqualTo(4);

        // When
        StepVerifier.create(service.cleanup()).expectNext(true).verifyComplete();

        await().atMost(3, TimeUnit.SECONDS)
                .with()
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> true);

        // Then
        await().atMost(3, TimeUnit.SECONDS)
                .with()
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> true);
        assertThat(
                executeQuery("SELECT s.* FROM `services` s ")
                        .rowsAsObject().size()
        ).isEqualTo(4);
        assertThat(executeQuery("SELECT s.* FROM `services` s ").rowsAsObject().isEmpty()).isEqualTo(false);
        verify(gluuService, never()).deleteUser(userRegisterState.getEmail());
    }

    @Test
    void shouldNotCleanUp() {
        assertThat(executeQuery("DELETE FROM `services` s ").metaData().status()).isEqualTo(QueryStatus.SUCCESS);
        // Given
        UserRegisterState userRegisterState = UserRegisterState
                .create(
                        OnboardShaktiUserRequest.builder()
                                .shaktiID(UUID.randomUUID().toString())
                                .build(),
                        true
                );
        List<UserRegisterStateDetail> details = new ArrayList<>();
        details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.GLUU_CREATED));
        details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.KYC_USER_CREATED));
        details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.KYC_USER_WALLET_UPDATED));
        details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.WALLET_CREATED));
        details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.SELFY_ID_ENCRYPTED));
        details.add(UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.BOUNTY_CREATED));

        userRegisterState.setLastModification(OffsetDateTime.now().minusMinutes(200).toInstant());
        assertThat(insert(UUID.randomUUID().toString(), "kycuser", userRegisterState.getShaktiId()))
                .isEqualTo(QueryStatus.SUCCESS);
        assertThat(insert(UUID.randomUUID().toString(), "selfy_id", userRegisterState.getShaktiId()))
                .isEqualTo(QueryStatus.SUCCESS);
        assertThat(insert(UUID.randomUUID().toString(), "genesis_bonus_bounty", userRegisterState.getShaktiId()))
                .isEqualTo(QueryStatus.SUCCESS);

        StepVerifier.create(userRegisterStateRepository.save(userRegisterState))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
        StepVerifier.create(userRegisterStateDetailRepository.saveAll(details))
                .expectNextMatches(Objects::nonNull)
                .expectNextMatches(Objects::nonNull)
                .expectNextMatches(Objects::nonNull)
                .expectNextMatches(Objects::nonNull)
                .expectNextMatches(Objects::nonNull)
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
        when(gluuService.deleteUser(userRegisterState.getEmail())).thenReturn(Mono.just(Response.ok().build()));

        await().atMost(3, TimeUnit.SECONDS)
                .with()
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> true);
        assertThat(
                executeQuery("SELECT s.* FROM `services` s ")
                        .rowsAsObject().size()
        ).isEqualTo(10);

        // When
        StepVerifier.create(service.cleanup()).expectNext(true).verifyComplete();

        await().atMost(3, TimeUnit.SECONDS)
                .with()
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> true);

        // Then
        await().atMost(3, TimeUnit.SECONDS)
                .with()
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> true);
        assertThat(
                executeQuery("SELECT s.* FROM `services` s ")
                        .rowsAsObject().size()
        ).isEqualTo(10);
        assertThat(executeQuery("SELECT s.* FROM `services` s ").rowsAsObject().isEmpty()).isEqualTo(false);
        verify(gluuService, never()).deleteUser(userRegisterState.getEmail());
    }

    private QueryStatus insert(String id, String type, String shaktiId) {
        return couchbaseTemplate
                .getCouchbaseClientFactory()
                .getCluster()
                .query(
                        "INSERT INTO `services` (KEY, VALUE) VALUES (\"" + id + "\", {\"_type\": \"" + type +
                                "\", \"shaktiID\": \"" + shaktiId + "\"})"
                ).metaData().status();
    }

    private QueryResult executeQuery(String query) {
        return couchbaseTemplate
                .getCouchbaseClientFactory()
                .getCluster()
                .query(query);
    }
}