package org.shaktifdn.registration.message;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.shaktifdn.registration.model.UserRegisterState;
import org.shaktifdn.registration.model.UserRegisterStateDetail;
import org.shaktifdn.registration.model.UserRegisterStateType;
import org.shaktifdn.registration.repository.UserRegisterStateDetailRepository;
import org.shaktifdn.registration.repository.UserRegisterStateRepository;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource("classpath:application-test.yml")
@ContextConfiguration(classes = {MessageHandler.class})
class MessageHandlerTest {

    @Autowired
    private ApplicationContext context;

    @MockBean
    private UserRegisterStateRepository userRegisterStateRepository;

    @MockBean
    private UserRegisterStateDetailRepository userRegisterStateDetailRepository;

    @Test
    void kycUserCreated() {

        KYCUserCreatedMessage message = KYCUserCreatedMessage
                .builder()
                .shaktiId("sk-1")
                .email("a@aa.com")
                .build();

        UserRegisterState userRegisterState = UserRegisterState.create(
                OnboardShaktiUserRequest.builder().build(),
                false
        );
        when(userRegisterStateRepository.findByShaktiId(message.getShaktiId()))
                .thenReturn(Mono.just(userRegisterState));
        ArgumentCaptor<List<UserRegisterStateDetail>> captor = ArgumentCaptor.forClass(List.class);
        when(userRegisterStateDetailRepository.saveAll(captor.capture()))
                .thenAnswer(invocationOnMock -> Mono.just(invocationOnMock.getArgument(0)));
        //noinspection unchecked
        Function<Flux<KYCUserCreatedMessage>, Flux<Void>> fn
                = context.getBean("kycUserCreated", Function.class);

        StepVerifier
                .create(fn.apply(Flux.just(message)))
                .verifyComplete();

        verify(userRegisterStateRepository).findByShaktiId(message.getShaktiId());
        verify(userRegisterStateDetailRepository).saveAll(anyCollection());
        assertThat(captor.getValue().size()).isEqualTo(1);
        assertThat(captor.getValue().get(0).getUserRegisterStateId()).isEqualTo(userRegisterState.getId());
        assertThat(captor.getValue().get(0).getStateType()).isEqualTo(UserRegisterStateType.KYC_USER_CREATED);
    }

    @Test
    void kycUserCreated_isMobile() {

        KYCUserCreatedMessage message = KYCUserCreatedMessage
                .builder()
                .shaktiId("sk-1")
                .email("a@aa.com")
                .build();

        UserRegisterState userRegisterState = UserRegisterState.create(
                OnboardShaktiUserRequest.builder().build(),
                true
        );
        when(userRegisterStateRepository.findByShaktiId(message.getShaktiId()))
                .thenReturn(Mono.just(userRegisterState));
        ArgumentCaptor<List<UserRegisterStateDetail>> captor = ArgumentCaptor.forClass(List.class);
        when(userRegisterStateDetailRepository.saveAll(captor.capture()))
                .thenAnswer(invocationOnMock -> Mono.just(invocationOnMock.getArgument(0)));
        //noinspection unchecked
        Function<Flux<KYCUserCreatedMessage>, Flux<Void>> fn
                = context.getBean("kycUserCreated", Function.class);

        StepVerifier
                .create(fn.apply(Flux.just(message)))
                .verifyComplete();

        verify(userRegisterStateRepository).findByShaktiId(message.getShaktiId());
        verify(userRegisterStateDetailRepository).saveAll(anyCollection());
        assertThat(captor.getValue().size()).isEqualTo(2);
        assertThat(captor.getValue().get(0).getUserRegisterStateId()).isEqualTo(userRegisterState.getId());
        assertThat(captor.getValue().get(1).getUserRegisterStateId()).isEqualTo(userRegisterState.getId());
        assertThat(captor.getValue().get(0).getStateType()).isEqualTo(UserRegisterStateType.KYC_USER_CREATED);
        assertThat(captor.getValue().get(1).getStateType()).isEqualTo(UserRegisterStateType.KYC_USER_WALLET_UPDATED);
    }

    @Test
    void kycUserCreated_when_error() {

        KYCUserCreatedMessage message = KYCUserCreatedMessage
                .builder()
                .shaktiId("sk-1")
                .email("a@aa.com")
                .build();

        when(userRegisterStateRepository.findByShaktiId(message.getShaktiId()))
                .thenReturn(Mono.error(new RuntimeException("unit-test")));
        //noinspection unchecked
        Function<Flux<KYCUserCreatedMessage>, Flux<Void>> fn
                = context.getBean("kycUserCreated", Function.class);

        StepVerifier
                .create(fn.apply(Flux.just(message)))
                .verifyComplete();

        verify(userRegisterStateRepository).findByShaktiId(message.getShaktiId());
        verify(userRegisterStateDetailRepository, never()).save(any());
    }

    @Test
    void bountyReferralCreated() {

        BountyReferralCreatedMessage message = BountyReferralCreatedMessage
                .builder()
                .shaktiId("sk-1")
                .genesisBonusBountyId("1")
                .build();
        UserRegisterState userRegisterState = UserRegisterState.create(
                OnboardShaktiUserRequest.builder().build(),
                false
        );
        when(userRegisterStateRepository.findByShaktiId(message.getShaktiId()))
                .thenReturn(Mono.just(userRegisterState));
        ArgumentCaptor<UserRegisterStateDetail> captor = ArgumentCaptor.forClass(UserRegisterStateDetail.class);
        when(userRegisterStateDetailRepository.save(captor.capture()))
                .thenAnswer(invocationOnMock -> Mono.just(invocationOnMock.getArgument(0)));
        //noinspection unchecked
        Function<Flux<BountyReferralCreatedMessage>, Flux<Void>> fn
                = context.getBean("bountyReferralCreated", Function.class);

        StepVerifier
                .create(fn.apply(Flux.just(message)))
                .verifyComplete();

        verify(userRegisterStateRepository).findByShaktiId(message.getShaktiId());
        verify(userRegisterStateDetailRepository).save(any());
        assertThat(captor.getValue().getUserRegisterStateId()).isEqualTo(userRegisterState.getId());
        assertThat(captor.getValue().getStateType()).isEqualTo(UserRegisterStateType.BOUNTY_CREATED);
    }

    @Test
    void bountyReferralCreated_when_error() {

        BountyReferralCreatedMessage message = BountyReferralCreatedMessage
                .builder()
                .shaktiId("sk-1")
                .genesisBonusBountyId("1")
                .build();

        when(userRegisterStateRepository.findByShaktiId(message.getShaktiId()))
                .thenReturn(Mono.error(new RuntimeException("unit-test")));
        //noinspection unchecked
        Function<Flux<BountyReferralCreatedMessage>, Flux<Void>> fn
                = context.getBean("bountyReferralCreated", Function.class);

        StepVerifier
                .create(fn.apply(Flux.just(message)))
                .verifyComplete();

        verify(userRegisterStateRepository).findByShaktiId(message.getShaktiId());
        verify(userRegisterStateRepository, never()).save(any());
    }

    @Test
    void selfyIdCreated() {

        SelfyIdCreatedMessage message = SelfyIdCreatedMessage
                .builder()
                .shaktiId("sk-1")
                .email("aa@aa.com")
                .build();
        UserRegisterState userRegisterState = UserRegisterState.create(
                OnboardShaktiUserRequest.builder().build(),
                false
        );
        when(userRegisterStateRepository.findByShaktiId(message.getShaktiId()))
                .thenReturn(Mono.just(userRegisterState));
        ArgumentCaptor<UserRegisterStateDetail> captor = ArgumentCaptor.forClass(UserRegisterStateDetail.class);
        when(userRegisterStateDetailRepository.save(captor.capture()))
                .thenAnswer(invocationOnMock -> Mono.just(invocationOnMock.getArgument(0)));
        //noinspection unchecked
        Function<Flux<SelfyIdCreatedMessage>, Flux<Void>> fn
                = context.getBean("selfyIdCreated", Function.class);

        StepVerifier
                .create(fn.apply(Flux.just(message)))
                .verifyComplete();

        verify(userRegisterStateRepository).findByShaktiId(message.getShaktiId());
        verify(userRegisterStateDetailRepository).save(any());
        assertThat(captor.getValue().getUserRegisterStateId()).isEqualTo(userRegisterState.getId());
        assertThat(captor.getValue().getStateType()).isEqualTo(UserRegisterStateType.SELFY_ID_ENCRYPTED);
    }

    @Test
    void selfyIdCreated_error() {

        SelfyIdCreatedMessage message = SelfyIdCreatedMessage
                .builder()
                .shaktiId("sk-1")
                .email("aa@aa.com")
                .build();

        when(userRegisterStateRepository.findByShaktiId(message.getShaktiId()))
                .thenReturn(Mono.error(new RuntimeException("unit-test")));
        //noinspection unchecked
        Function<Flux<SelfyIdCreatedMessage>, Flux<Void>> fn
                = context.getBean("selfyIdCreated", Function.class);

        StepVerifier
                .create(fn.apply(Flux.just(message)))
                .verifyComplete();

        verify(userRegisterStateRepository).findByShaktiId(message.getShaktiId());
        verify(userRegisterStateRepository, never()).save(any());
    }

}