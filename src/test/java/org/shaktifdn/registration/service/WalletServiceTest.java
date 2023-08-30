package org.shaktifdn.registration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.core.HoverflyConfig;
import io.specto.hoverfly.junit.core.model.RequestFieldMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.shaktifdn.registration.AbstractTest;
import org.shaktifdn.registration.RegistrationTestConfiguration;
import org.shaktifdn.registration.config.ServiceProperties;
import org.shaktifdn.registration.enums.AccountType;
import org.shaktifdn.registration.exception.ExternalServiceDependencyFailure;
import org.shaktifdn.registration.model.UserRegisterState;
import org.shaktifdn.registration.model.UserRegisterStateDetail;
import org.shaktifdn.registration.model.UserRegisterStateType;
import org.shaktifdn.registration.repository.UserRegisterStateDetailRepository;
import org.shaktifdn.registration.repository.UserRegisterStateRepository;
import org.shaktifdn.registration.request.CreateWalletRequest;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.shaktifdn.registration.request.NewUserWalletAccessRequest;
import org.shaktifdn.registration.request.WalletRequest;
import org.shaktifdn.registration.response.CreateWalletResponse;
import org.shaktifdn.registration.response.PassphraseResponse;
import org.shaktifdn.registration.response.ShaktiResponse;
import org.shaktifdn.registration.response.UserDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static io.specto.hoverfly.junit.core.HoverflyMode.SIMULATE;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {WalletService.class})
@Import(RegistrationTestConfiguration.class)
public class WalletServiceTest extends AbstractTest {

    private static Hoverfly hoverfly;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WalletService walletService;

    @MockBean
    private UserInfoClient userInfoClient;

    @MockBean
    private KycUserService kycUserService;

    @MockBean
    private UserRegisterStateRepository userRegisterStateRepository;

    @MockBean
    private UserRegisterStateDetailRepository userRegisterStateDetailRepository;

    @MockBean
    private ServiceProperties serviceProperties;

    @BeforeEach
    void setUp() {
        var localConfig = HoverflyConfig
                .localConfigs()
                .disableTlsVerification()
                .asWebServer()
                .proxyPort(18999);
        hoverfly = new Hoverfly(localConfig, SIMULATE);
        hoverfly.start();
    }


    @AfterEach
    void tearDown() {
        hoverfly.close();
    }

    @Test
    public void create() throws JsonProcessingException {
        WalletRequest walletRequest = new WalletRequest();
        walletRequest.setPassphrase("test");
        UserDetail userDetail = UserDetail.builder().shaktiId("sk-1").build();
        when(userInfoClient.getShaktiId()).thenReturn(
                Mono.just(userDetail)
        );
        when(kycUserService.isWalletExists(userDetail.getShaktiId())).thenReturn(Mono.just(false));
        CreateWalletRequest request = CreateWalletRequest
                .builder()
                .authorizationBytes(walletRequest.getAuthorizationBytes())
                .passphrase("test-p")
                .accountType(AccountType.PERSONAL)
                .shaktiID(userDetail.getShaktiId())
                .build();

        UserRegisterState userRegisterState = UserRegisterState.create(
                OnboardShaktiUserRequest.builder().build(),
                false
        );
        when(userRegisterStateRepository.findByShaktiId(userDetail.getShaktiId()))
                .thenReturn(Mono.just(userRegisterState));
        ArgumentCaptor<List<UserRegisterStateDetail>> captor = ArgumentCaptor.forClass(List.class);
        when(userRegisterStateDetailRepository.saveAll(captor.capture()))
                .thenAnswer(invocationOnMock -> Flux.just(invocationOnMock.getArgument(0)));

        CreateWalletResponse createWalletResponse = new CreateWalletResponse();
        createWalletResponse.setWalletBytes("wb");
        createWalletResponse.setWalletBytes("wb");
        createWalletResponse.setMainnetWalletID("mn-1");
        createWalletResponse.setMainnetWalletID("tn-1");
        createWalletResponse.setMessage("hello");
        when(serviceProperties.getWalletService()).thenReturn("http://localhost:18999");
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(WalletService.URL_WALLETS)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(createWalletResponse))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        ),
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .get(WalletService.URL_WALLETS_PASSPHRASE)
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(
                                                ShaktiResponse.builder().data(PassphraseResponse.builder().passphrase("test-p").build()).build()
                                        ))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));

        StepVerifier
                .create(walletService.create(walletRequest))
                .expectNext(createWalletResponse)
                .verifyComplete();

        verify(userInfoClient).getShaktiId();
        verify(kycUserService).isWalletExists(userDetail.getShaktiId());
        verify(userRegisterStateRepository).findByShaktiId(userDetail.getShaktiId());
        verify(userRegisterStateDetailRepository).saveAll(anyCollection());
        assertThat(captor.getValue().get(0).getUserRegisterStateId()).isEqualTo(userRegisterState.getId());
        assertThat(captor.getValue().get(0).getStateType()).isEqualTo(UserRegisterStateType.WALLET_CREATED);
        assertThat(captor.getValue().get(1).getUserRegisterStateId()).isEqualTo(userRegisterState.getId());
        assertThat(captor.getValue().get(1).getStateType()).isEqualTo(UserRegisterStateType.KYC_USER_WALLET_UPDATED);
    }

    @Test
    public void shouldThrowWebServiceError() throws JsonProcessingException {
        WalletRequest walletRequest = new WalletRequest();
        walletRequest.setPassphrase("test");
        UserDetail userDetail = UserDetail.builder().shaktiId("sk-1").build();
        when(userInfoClient.getShaktiId()).thenReturn(
                Mono.just(userDetail)
        );
        when(kycUserService.isWalletExists(userDetail.getShaktiId())).thenReturn(Mono.just(false));
        CreateWalletRequest request = CreateWalletRequest
                .builder()
                .authorizationBytes(walletRequest.getAuthorizationBytes())
                .passphrase(walletRequest.getPassphrase())
                .accountType(AccountType.PERSONAL)
                .shaktiID(userDetail.getShaktiId())
                .build();

        when(userRegisterStateRepository.findByShaktiId(userDetail.getShaktiId()))
                .thenReturn(Mono.just(new UserRegisterState()));
        when(userRegisterStateRepository.save(any()))
                .thenReturn(Mono.just(new UserRegisterState()));

        CreateWalletResponse createWalletResponse = new CreateWalletResponse();
        createWalletResponse.setWalletBytes("wb");
        createWalletResponse.setMainnetWalletID("mn-1");
        createWalletResponse.setMainnetWalletID("tn-1");
        createWalletResponse.setMessage("hello");
        when(serviceProperties.getWalletService()).thenReturn("http://localhost:18999");
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(WalletService.URL_WALLETS)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(serverError())
        ));


        StepVerifier
                .create(walletService.create(walletRequest))
                .expectError(ExternalServiceDependencyFailure.class)
                .verify();

        verify(userInfoClient).getShaktiId();
        verify(kycUserService).isWalletExists(userDetail.getShaktiId());
        verifyNoInteractions(userRegisterStateRepository);
    }

    @Test
    public void create_already_exists() {
        WalletRequest request = new WalletRequest();
        request.setPassphrase("test");
        when(userInfoClient.getShaktiId()).thenReturn(
                Mono.just(UserDetail.builder().shaktiId("sk-1").build())
        );
        when(kycUserService.isWalletExists("sk-1")).thenReturn(Mono.just(true));
        StepVerifier
                .create(walletService.create(request))
                .expectErrorMessage("wallet already exists")
                .verify();

        verify(userInfoClient).getShaktiId();
        verify(kycUserService).isWalletExists("sk-1");
    }

    @Test
    public void create_user_does_not_exists() {
        WalletRequest request = new WalletRequest();
        request.setPassphrase("test");
        when(userInfoClient.getShaktiId()).thenReturn(Mono.empty());
        StepVerifier
                .create(walletService.create(request))
                .expectErrorMessage("User not found for wallet creation")
                .verify();


        verify(userInfoClient).getShaktiId();
        verify(kycUserService, never()).isWalletExists(any());
    }

    @Test
    public void userDeviceAccessRegister() throws JsonProcessingException {
        NewUserWalletAccessRequest request = NewUserWalletAccessRequest.builder().shaktiId("sk-1").deviceId("d1").build();
        when(serviceProperties.getWalletService()).thenReturn("http://localhost:18999");
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(WalletService.URL_WALLET_DEVICE_ACCESS_REGISTRATION)
                        .body(objectMapper.writeValueAsString(request))
                        .header("Content-Type", "application/json")
                        .willReturn(success())
        ));

        StepVerifier
                .create(walletService.userDeviceAccessRegister(request))
                .expectNext("")
                .verifyComplete();
    }
}
