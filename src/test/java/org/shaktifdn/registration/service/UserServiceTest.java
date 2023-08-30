package org.shaktifdn.registration.service;

import org.gluu.oxauth.client.UserInfoClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.shaktifdn.registration.AbstractTest;
import org.shaktifdn.registration.constant.Message;
import org.shaktifdn.registration.message.CreateUserMessage;
import org.shaktifdn.registration.model.UserRegisterState;
import org.shaktifdn.registration.model.UserRegisterStateDetail;
import org.shaktifdn.registration.model.UserRegisterStateType;
import org.shaktifdn.registration.repository.UserRegisterStateDetailRepository;
import org.shaktifdn.registration.repository.UserRegisterStateRepository;
import org.shaktifdn.registration.request.NewUserWalletAccessRequest;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.shaktifdn.registration.request.WalletBytesEncryptRequest;
import org.shaktifdn.registration.response.SmsServiceResponse;
import org.shaktifdn.registration.response.TokenResponse;
import org.shaktifdn.registration.response.WalletBytesEncryptResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {UserService.class})
public class UserServiceTest extends AbstractTest {

    private static final String IP_ADDRESS = "0.0.0.0";

    @Autowired
    private UserService userService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private MobileService mobileService;

    @MockBean
    private GluuServiceApi gluuService;

    @MockBean
    private UserInfoClient userInfoClient;

    @MockBean
    private UserRegisterStateRepository userRegisterStateRepository;

    @MockBean
    private UserRegisterStateDetailRepository userRegisterStateDetailRepository;

    @MockBean
    private Sinks.Many<CreateUserMessage> createUserMessageSink;

    @MockBean
    private SelfyIdService selfyIdService;

    @MockBean
    private BizVaultService bizVaultServiceWebClient;

    @MockBean
    private IpAddressService ipAddressService;

    @MockBean
    private WalletService walletService;


    @Test
    public void saveOnboardShaktiTest() {
        //given
        OnboardShaktiUserRequest onboardShaktiUserRequest = givenOnboardShaktiModel(false);
        onboardShaktiUserRequest.setMainnetWalletId("m-1");
        onboardShaktiUserRequest.setTestnetWalletId("t-1");
        SmsServiceResponse emailServiceResponse = SmsServiceResponse
                .builder()
                .message(Message.EMAIL_OTP_SENT_SUCCESS)
                .code(200)
                .build();
        //when
        when(gluuService.createUser(any(), anyString()))
                .thenReturn(Mono.just(Response.ok().build()));
        when(mobileService.inquire(any(), any(), any()))
                .thenReturn(Mono.just(emailServiceResponse));
        when(emailService.isOtpVerified(any(), any()))
                .thenReturn(Mono.just(true));
        UserRegisterState userRegisterState =
                UserRegisterState.create(onboardShaktiUserRequest, true);
        ArgumentCaptor<UserRegisterState> userRegisterStateCaptor = ArgumentCaptor.forClass(UserRegisterState.class);
        when(userRegisterStateRepository.save(userRegisterStateCaptor.capture()))
                .thenReturn(Mono.just(userRegisterState));
        when(userRegisterStateRepository.findByShaktiId(any()))
                .thenReturn(Mono.just(userRegisterState));
        when(gluuService.isEmailRegistered(onboardShaktiUserRequest.getEmail()))
                .thenReturn(Mono.just(false));
        when(bizVaultServiceWebClient.isEmailRegistered(onboardShaktiUserRequest.getEmail()))
                .thenReturn(Mono.just(false));
        ArgumentCaptor<CreateUserMessage> createUserMessageCaptor = ArgumentCaptor.forClass(CreateUserMessage.class);
        when(createUserMessageSink.tryEmitNext(createUserMessageCaptor.capture())).thenReturn(Sinks.EmitResult.OK);

        ArgumentCaptor<UserRegisterStateDetail> userRegisterStateDetailCaptor
                = ArgumentCaptor.forClass(UserRegisterStateDetail.class);
        when(userRegisterStateDetailRepository.save(userRegisterStateDetailCaptor.capture()))
                .thenAnswer(invocationOnMock -> Mono.just(invocationOnMock.getArgument(0)));
        when(walletService.userDeviceAccessRegister(any())).thenReturn(Mono.just(""));
        //then
        StepVerifier
                .create(userService.saveOnboardShakti(onboardShaktiUserRequest, IP_ADDRESS))
                .expectNextMatches(responseBean -> {
                    assertEquals(201, responseBean.getStatus());
                    assertNotNull(responseBean.getDetails());
                    OnboardShaktiUserRequest request = (OnboardShaktiUserRequest) responseBean.getDetails();
                    assertNotNull(request.getShaktiID());
                    assertEquals(onboardShaktiUserRequest.getEmail(), request.getEmail());
                    assertEquals(onboardShaktiUserRequest.getMobileNo(), request.getMobileNo());
                    return true;
                })
                .verifyComplete();

        then(gluuService).should().isEmailRegistered(onboardShaktiUserRequest.getEmail());
        verify(gluuService).createUser(any(), anyString());
        verify(mobileService).inquire(any(), any(), any());
        verify(emailService).isOtpVerified(any(), any());
        verify(createUserMessageSink).tryEmitNext(any());
        then(userRegisterStateRepository).should().save(any());

        UserRegisterState userRegisterStateToTest = userRegisterStateCaptor.getValue();
        assertNotNull(userRegisterStateToTest.getShaktiId());
        assertEquals(onboardShaktiUserRequest.getEmail(), userRegisterStateToTest.getEmail());
        assertEquals(onboardShaktiUserRequest.getCountryCode(), userRegisterStateToTest.getCountryCode());
        assertEquals(onboardShaktiUserRequest.getMobileNo(), userRegisterStateToTest.getMobileNumber());
        verify(userRegisterStateDetailRepository).save(any());

        assertEquals(userRegisterStateDetailCaptor.getValue().getStateType(), UserRegisterStateType.GLUU_CREATED);

        CreateUserMessage createUserMessage = createUserMessageCaptor.getValue();
        assertNotNull(createUserMessage);
        assertNotNull(createUserMessage.getShaktiId());
        assertEquals(onboardShaktiUserRequest.getEmail(), createUserMessage.getEmail());
        assertEquals(onboardShaktiUserRequest.getCountryCode(), createUserMessage.getCountryCode());
        assertEquals(onboardShaktiUserRequest.getMobileNo(), createUserMessage.getMobileNo());
        assertEquals(onboardShaktiUserRequest.getAuthorizationBytes(), createUserMessage.getAuthorizationBytes());
        assertEquals(onboardShaktiUserRequest.getWalletBytes(), createUserMessage.getEncryptedWalletBytes());
        assertEquals(onboardShaktiUserRequest.getMainnetWalletId(), createUserMessage.getMainnetWalletId());
        assertEquals(onboardShaktiUserRequest.getTestnetWalletId(), createUserMessage.getTestnetWalletId());
    }

    @Test
//    @Disabled
    public void saveOnboardMobileShaktiTest() {

        //given
        OnboardShaktiUserRequest onboardShaktiUserRequest = givenOnboardShaktiModel(true);
        onboardShaktiUserRequest.setMainnetWalletId("m-1");
        onboardShaktiUserRequest.setTestnetWalletId("t-1");
        SmsServiceResponse smsServiceResponse = SmsServiceResponse
                .builder()
                .message(Message.EMAIL_OTP_SENT_SUCCESS)
                .code(200)
                .build();

        //when
        when(gluuService.createUser(any(), anyString()))
                .thenReturn(Mono.just(Response.ok().build()));
        when(mobileService.inquire(any(), any(), any()))
                .thenReturn(Mono.just(smsServiceResponse));
        when(emailService.isOtpVerified(any(), any()))
                .thenReturn(Mono.just(true));
        when(gluuService.getToken(any()))
                .thenReturn(Mono.just(new TokenResponse()));
        UserRegisterState userRegisterState =
                UserRegisterState.create(onboardShaktiUserRequest, true);
        ArgumentCaptor<UserRegisterState> userRegisterStateCaptor = ArgumentCaptor.forClass(UserRegisterState.class);
        when(userRegisterStateRepository.save(userRegisterStateCaptor.capture()))
                .thenReturn(Mono.just(userRegisterState));
        when(userRegisterStateRepository.findByShaktiId(any()))
                .thenReturn(Mono.just(userRegisterState));
        when(gluuService.isEmailRegistered(onboardShaktiUserRequest.getEmail()))
                .thenReturn(Mono.just(false));
        when(bizVaultServiceWebClient.isEmailRegistered(onboardShaktiUserRequest.getEmail()))
                .thenReturn(Mono.just(false));
        ArgumentCaptor<CreateUserMessage> createUserMessageCaptor = ArgumentCaptor.forClass(CreateUserMessage.class);
        when(createUserMessageSink.tryEmitNext(createUserMessageCaptor.capture())).thenReturn(Sinks.EmitResult.OK);
        ArgumentCaptor<WalletBytesEncryptRequest> walletBytesEncryptRequestCaptor =
                ArgumentCaptor.forClass(WalletBytesEncryptRequest.class);
        when(selfyIdService.encrypt(walletBytesEncryptRequestCaptor.capture()))
                .thenReturn(Mono.just(WalletBytesEncryptResponse.builder().encryptedWalletBytes("enc").build()));

        ArgumentCaptor<UserRegisterStateDetail> userRegisterStateDetailCaptor
                = ArgumentCaptor.forClass(UserRegisterStateDetail.class);
        when(userRegisterStateDetailRepository.save(userRegisterStateDetailCaptor.capture()))
                .thenAnswer(invocationOnMock -> Mono.just(invocationOnMock.getArgument(0)));

        NewUserWalletAccessRequest.builder().deviceId("dc-1234").build();
        when(walletService.userDeviceAccessRegister(any())).thenReturn(Mono.just(""));
        //then
        StepVerifier
                .create(userService.saveOnboardShakti(onboardShaktiUserRequest, IP_ADDRESS))
                .expectNextMatches(
                        response -> response.getStatus() == 201 && response.getDetails().equals(onboardShaktiUserRequest)
                )
                .verifyComplete();
        then(gluuService).should().isEmailRegistered(onboardShaktiUserRequest.getEmail());
        then(userRegisterStateRepository).should(atLeast(1)).save(any());
        verify(createUserMessageSink).tryEmitNext(any());
        verify(selfyIdService).encrypt(any());


        UserRegisterState userRegisterStateToTest = userRegisterStateCaptor.getValue();
        assertNotNull(userRegisterStateToTest.getShaktiId());
        assertEquals(onboardShaktiUserRequest.getEmail(), userRegisterStateToTest.getEmail());
        assertEquals(onboardShaktiUserRequest.getCountryCode(), userRegisterStateToTest.getCountryCode());
        assertEquals(onboardShaktiUserRequest.getMobileNo(), userRegisterStateToTest.getMobileNumber());
        verify(userRegisterStateDetailRepository, times(2)).save(any());

        assertEquals(userRegisterStateDetailCaptor.getAllValues().get(0).getStateType(), UserRegisterStateType.GLUU_CREATED);
        assertEquals(userRegisterStateDetailCaptor.getAllValues().get(1).getStateType(), UserRegisterStateType.WALLET_CREATED);
        assertEquals(
                onboardShaktiUserRequest.getWalletBytes(),
                walletBytesEncryptRequestCaptor.getValue().getWalletBytes()
        );

        CreateUserMessage createUserMessage = createUserMessageCaptor.getValue();
        assertNotNull(createUserMessage);
        assertNotNull(createUserMessage.getShaktiId());
        assertEquals(onboardShaktiUserRequest.getEmail(), createUserMessage.getEmail());
        assertEquals(onboardShaktiUserRequest.getCountryCode(), createUserMessage.getCountryCode());
        assertEquals(onboardShaktiUserRequest.getMobileNo(), createUserMessage.getMobileNo());
        assertEquals(onboardShaktiUserRequest.getAuthorizationBytes(), createUserMessage.getAuthorizationBytes());
        assertEquals("enc", createUserMessage.getEncryptedWalletBytes());
        assertEquals(onboardShaktiUserRequest.getMainnetWalletId(), createUserMessage.getMainnetWalletId());
        assertEquals(onboardShaktiUserRequest.getTestnetWalletId(), createUserMessage.getTestnetWalletId());
    }

    @Test
    public void saveOnboardShaktiTest_already_registered() {
        //given
        OnboardShaktiUserRequest onboardShaktiUserRequest = givenOnboardShaktiModel(false);
        SmsServiceResponse smsServiceResponse =
                SmsServiceResponse
                        .builder()
                        .message(Message.EMAIL_OTP_SENT_SUCCESS)
                        .code(200)
                        .build();
        //when
        when(gluuService.isEmailRegistered(onboardShaktiUserRequest.getEmail()))
                .thenReturn(Mono.just(true));
        when(bizVaultServiceWebClient.isEmailRegistered(onboardShaktiUserRequest.getEmail()))
                .thenReturn(Mono.just(false));
        //then
        StepVerifier
                .create(userService.saveOnboardShakti(onboardShaktiUserRequest, IP_ADDRESS))
                .expectErrorMessage("siddiquifaizal@yahoo.com email is already registered")
                .verify();

        verify(gluuService).isEmailRegistered(onboardShaktiUserRequest.getEmail());
        verify(gluuService, never()).createUser(any(), anyString());
        verify(mobileService, never()).inquire(any(), any(), any());
    }

    @Test
    public void saveOnboardShaktiTest_bizVault_already_registered() {
        //given
        OnboardShaktiUserRequest onboardShaktiUserRequest = givenOnboardShaktiModel(false);
        //when
        when(gluuService.isEmailRegistered(onboardShaktiUserRequest.getEmail()))
                .thenReturn(Mono.just(false));
        when(bizVaultServiceWebClient.isEmailRegistered(onboardShaktiUserRequest.getEmail()))
                .thenReturn(Mono.just(true));
        //then
        StepVerifier
                .create(userService.saveOnboardShakti(onboardShaktiUserRequest, IP_ADDRESS))
                .expectErrorMessage("siddiquifaizal@yahoo.com email is already registered")
                .verify();

        verify(gluuService).isEmailRegistered(onboardShaktiUserRequest.getEmail());
        verify(gluuService, never()).createUser(any(), anyString());
        verify(mobileService, never()).inquire(any(), any(), any());
    }

    private OnboardShaktiUserRequest givenOnboardShaktiModel(boolean isMobile) {
        OnboardShaktiUserRequest onboardShaktiUserRequest = new OnboardShaktiUserRequest();
        onboardShaktiUserRequest.setEmail("siddiquifaizal@yahoo.com");
        onboardShaktiUserRequest.setShaktiID("siddiquifaizal@yahoo.com");
        onboardShaktiUserRequest.setPassword("password");
        onboardShaktiUserRequest.setMobileNo("123456789");
        onboardShaktiUserRequest.setPin("1234");
        onboardShaktiUserRequest.setDeviceId("dc-1234");
        if (isMobile) {
            onboardShaktiUserRequest.setWalletBytes("EncryptedWalletBytes");
            onboardShaktiUserRequest.setMainnetWalletId("MainnetWalletId");
            onboardShaktiUserRequest.setTestnetWalletId("TestnetWalletId");
        }
        return onboardShaktiUserRequest;
    }

    @Test
    public void checkUserExistTest() {
        String email = "amitzkumar001@gmail.com";
        when(gluuService.isEmailRegistered(email)).thenReturn(Mono.just(true));
        when(bizVaultServiceWebClient.isEmailRegistered(email)).thenReturn(Mono.just(false));
        StepVerifier
                .create(userService.checkEmailIsRegistered(email))
                .expectNext(true)
                .verifyComplete();
        verify(gluuService).isEmailRegistered(email);
        verify(bizVaultServiceWebClient).isEmailRegistered(email);
    }

    @Test
    public void checkUserExistTestBizVault() {
        String email = "amitzkumar001@gmail.com";
        when(gluuService.isEmailRegistered(email)).thenReturn(Mono.just(false));
        when(bizVaultServiceWebClient.isEmailRegistered(email)).thenReturn(Mono.just(true));
        StepVerifier
                .create(userService.checkEmailIsRegistered(email))
                .expectNext(true)
                .verifyComplete();
        verify(gluuService).isEmailRegistered(email);
        verify(bizVaultServiceWebClient).isEmailRegistered(email);
    }

    @Test
    public void checkUserNotExist() {
        String email = "amitzkumar001@gmail.com";
        when(gluuService.isEmailRegistered(email)).thenReturn(Mono.just(false));
        when(bizVaultServiceWebClient.isEmailRegistered(email)).thenReturn(Mono.just(false));
        StepVerifier
                .create(userService.checkEmailIsRegistered(email))
                .expectNext(false)
                .verifyComplete();
        verify(gluuService).isEmailRegistered(email);
        verify(bizVaultServiceWebClient).isEmailRegistered(email);
    }
}
