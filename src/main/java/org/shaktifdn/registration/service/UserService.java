package org.shaktifdn.registration.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.shaktifdn.registration.constant.Constant;
import org.shaktifdn.registration.exception.BadRequestException;
import org.shaktifdn.registration.exception.ExternalServiceDependencyFailure;
import org.shaktifdn.registration.exception.ShaktiWebClientException;
import org.shaktifdn.registration.exception.UserAlreadyRegisteredException;
import org.shaktifdn.registration.message.CreateUserMessage;
import org.shaktifdn.registration.model.UserRegisterState;
import org.shaktifdn.registration.model.UserRegisterStateDetail;
import org.shaktifdn.registration.model.UserRegisterStateType;
import org.shaktifdn.registration.repository.UserRegisterStateDetailRepository;
import org.shaktifdn.registration.repository.UserRegisterStateRepository;
import org.shaktifdn.registration.request.NewUserWalletAccessRequest;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.shaktifdn.registration.request.WalletBytesEncryptRequest;
import org.shaktifdn.registration.response.ResponseBean;
import org.shaktifdn.registration.response.WalletBytesEncryptResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;

import java.util.UUID;

import static org.shaktifdn.registration.constant.Constant.EMAIL_REGISTERATION_FLOW;

@Service
@Slf4j
public class UserService {

    private final EmailService emailService;
    private final MobileService mobileService;
    private final GluuServiceApi gluuService;
    private final SelfyIdService selfyIdService;
    private final UserRegisterStateRepository userRegisterStateRepository;
    private final UserRegisterStateDetailRepository userRegisterStateDetailRepository;
    private final BizVaultService bizVaultService;
    private final WalletService walletService;
    private final Sinks.Many<CreateUserMessage> createUserMessageSink;

    public UserService(
            EmailService emailService,
            MobileService mobileService,
            GluuServiceApi gluuService,
            SelfyIdService selfyIdService,
            UserRegisterStateRepository userRegisterStateRepository,
            UserRegisterStateDetailRepository userRegisterStateDetailRepository,
            BizVaultService bizVaultService,
            WalletService walletService,
            Sinks.Many<CreateUserMessage> createUserMessageSink
    ) {
        this.emailService = emailService;
        this.mobileService = mobileService;
        this.gluuService = gluuService;
        this.selfyIdService = selfyIdService;
        this.userRegisterStateRepository = userRegisterStateRepository;
        this.userRegisterStateDetailRepository = userRegisterStateDetailRepository;
        this.bizVaultService = bizVaultService;
        this.walletService = walletService;
        this.createUserMessageSink = createUserMessageSink;
    }

    /**
     * This method is verifies whether the email and mobile are verified , if yes it
     * proceed for the user creation
     *
     * @param onboardShakti shakti user request
     * @return Mono of Response Bean
     */
    public Mono<ResponseBean> saveOnboardShakti(OnboardShaktiUserRequest onboardShakti, String clientIpAddress) {
        onboardShakti.setEmail(onboardShakti.getEmail().toLowerCase());
        if (isMobileClient(onboardShakti)) {
            return onboardMobileUser(onboardShakti, clientIpAddress);
        }
        return onboardWebBrowserUser(onboardShakti, clientIpAddress);
    }


    /**
     * To check user exit or not
     *
     * @param email email String
     * @return Mono of response bean
     */
    public Mono<Boolean> checkEmailIsRegistered(String email) {
        return Mono.zip(
                        gluuService.isEmailRegistered(email.toLowerCase()),
                        bizVaultService.isEmailRegistered(email.toLowerCase())
                )
                .map(tuple -> tuple.getT1() || tuple.getT2())
                .onErrorResume(throwable -> {
                            log.error("error  while checking Email registration {}", email, throwable);
                            return Mono.error(throwable);
                        }
                )
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("empty result while checking Email registration {}", email);
                    return Mono.error(new ShaktiWebClientException(
                            "unexpected result while checking Email registration " + email
                    ));
                }));
    }

    /**
     * Check emailId is verified, calling the email service from onboard
     *
     * @param onboardShakti shakti request
     */
    private Mono<OnboardShaktiUserRequest> checkEmail(OnboardShaktiUserRequest onboardShakti) {
        return emailService.isOtpVerified(onboardShakti.getEmail().toLowerCase(), EMAIL_REGISTERATION_FLOW)
                .flatMap(status -> {
                    if (status) {
                        onboardShakti.setEmailVerified(true);
                        return Mono.just(onboardShakti);
                    } else {
                        log.info("email: {} is not verified", onboardShakti.getEmail());
                        return Mono.error(
                                new BadRequestException(onboardShakti.getEmail() + " is not verified")
                        );
                    }
                })
                .switchIfEmpty(Mono.error(new BadRequestException(onboardShakti.getEmail())))
                .onErrorResume(throwable -> Mono.error(new ShaktiWebClientException("Error in on-boarding: " + throwable.getMessage())));
    }

    /**
     * Check mobile is verified, calling the SMS OTP service from onboard
     *
     * @param onboardShakti Onboard shakti model
     * @return Mono of OnboardShaktiModel
     */
    private Mono<OnboardShaktiUserRequest> checkMobile(OnboardShaktiUserRequest onboardShakti) {
        return mobileService
                .inquire(
                        onboardShakti.getMobileNo(),
                        onboardShakti.getCountryCode(),
                        Constant.MOBILE_REGISTERATION_FLOW
                )
                .flatMap(verificationResponse -> {
                    if (verificationResponse.getCode() == 200) {
                        onboardShakti.setMobileVerified(true);
                        return Mono.just(onboardShakti);
                    } else {
                        log.info(
                                "error on mobile: {} verified check: {}",
                                onboardShakti.getMobileNo(),
                                verificationResponse
                        );
                        return Mono.error(new BadRequestException(
                                onboardShakti.getMobileNo() + " " + verificationResponse.getMessage()));
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("No response from sms service for mobile number {}", onboardShakti.getMobileNo());
                    return Mono.error(new BadRequestException(onboardShakti.getMobileNo()));
                }))
                .onErrorResume(throwable -> {
                    log.error("Error while inquiring mobile number for {} ", onboardShakti, throwable);
                    return Mono.error(new ShaktiWebClientException("Error while inquiring mobile number " + onboardShakti.getMobileNo()));
                });
    }

    private Mono<ResponseBean> onboardUser(OnboardShaktiUserRequest onboardShakti, Boolean isMobileUser, String ipAddress) {
        return Mono.zip(
                        gluuService
                                .isEmailRegistered(onboardShakti.getEmail()),
                        bizVaultService.isEmailRegistered(onboardShakti.getEmail())
                )
                .map(tuple -> tuple.getT1() || tuple.getT2())
                .flatMap(status -> {
                    if (status) {
                        return Mono.error(new UserAlreadyRegisteredException(
                                onboardShakti.getEmail() + " email is already registered"
                        ));
                    }
                    return Mono.zip(
                                    checkEmail(onboardShakti),
                                    checkMobile(onboardShakti)
                            )
                            .map(Tuple2::getT1);
                })
                .flatMap(this::addShaktiId)
                .flatMap(onboardShaktiModel -> createGluuUser(isMobileUser, onboardShaktiModel, ipAddress))
                .switchIfEmpty(Mono.error(new ExternalServiceDependencyFailure(HttpStatus.FAILED_DEPENDENCY, "Error while adding the user to the authentication server records")))
                .zipWhen(onboardShaktiModel -> Mono.zip(
                                encryptWalletBytes(isMobileUser, onboardShaktiModel),
                                walletService.userDeviceAccessRegister(
                                        NewUserWalletAccessRequest
                                                .builder()
                                                .shaktiId(onboardShaktiModel.getShaktiID())
                                                .deviceId(onboardShakti.getDeviceId())
                                                .ipAddress(ipAddress)
                                                .location(onboardShakti.getGeojson() != null ? (onboardShakti.getGeojson().getLatitude() + "," + onboardShakti.getGeojson().getLongitude()) : "")
                                                .build()
                                )
                        ).map(Tuple2::getT1)
                )
                .doOnNext(tuple -> sendUserCreatedMessage(tuple.getT1(), tuple.getT2()))
                .flatMap(tuple -> Mono.just(new ResponseBean(201, tuple.getT1())))
                .switchIfEmpty(Mono.error(new ExternalServiceDependencyFailure(HttpStatus.FAILED_DEPENDENCY, "No response from selfyid")))
                .onErrorResume(e -> {
                    log.error(
                            "An error has been thrown from the {} on board flow for user email id {}",
                            isMobileUser ? "mobile" : "web",
                            onboardShakti.getEmail(), e);
                    return Mono.error(e);
                });
    }

    private Mono<OnboardShaktiUserRequest> createGluuUser(Boolean isMobileUser, OnboardShaktiUserRequest onboardShaktiModel, String ipAddress) {
        return gluuService
                .createUser(onboardShaktiModel, ipAddress)
                .flatMap(response -> {
                    log.info("Creating user state for new registered user for email {} ", onboardShaktiModel.getEmail());
                    UserRegisterState userRegisterState =
                            UserRegisterState.create(onboardShaktiModel, isMobileUser);
                    return userRegisterStateRepository
                            .save(userRegisterState)
                            .flatMap(state -> userRegisterStateDetailRepository.save(
                                    UserRegisterStateDetail.create(state, UserRegisterStateType.GLUU_CREATED)
                            ));
                })
                .then(Mono.just(onboardShaktiModel));
    }

    private Mono<WalletBytesEncryptResponse> encryptWalletBytes(
            Boolean isMobileUser,
            OnboardShaktiUserRequest onboardShaktiModel
    ) {
        if (isMobileUser) {
            log.info("encrypting wallet bytes on selfyid for mobile user shakti id {}, email: {} ", onboardShaktiModel.getShaktiID(), onboardShaktiModel.getEmail());
            return selfyIdService.encrypt(
                    WalletBytesEncryptRequest
                            .builder()
                            .walletBytes(onboardShaktiModel.getWalletBytes())
                            .passphrase(onboardShaktiModel.getPassphrase())
                            .build()
            );
        } else {
            log.info("No need for encrypting wallet bytes on selfyid for web based user shakti id {}, email: {} ", onboardShaktiModel.getShaktiID(), onboardShaktiModel.getEmail());
            return Mono.just(WalletBytesEncryptResponse.builder().build());
        }
    }

    private void sendUserCreatedMessage(
            OnboardShaktiUserRequest onboardShaktiUserRequest,
            WalletBytesEncryptResponse walletBytesEncryptResponse
    ) {
        log.info("Sending create user message user email id {} ", onboardShaktiUserRequest.getEmail());
        createUserMessageSink.tryEmitNext(
                CreateUserMessage
                        .builder()
                        .shaktiId(onboardShaktiUserRequest.getShaktiID())
                        .email(onboardShaktiUserRequest.getEmail().toLowerCase())
                        .countryCode(onboardShaktiUserRequest.getCountryCode())
                        .mobileNo(onboardShaktiUserRequest.getMobileNo())
                        .authorizationBytes(onboardShaktiUserRequest.getAuthorizationBytes())
                        .encryptedWalletBytes(walletBytesEncryptResponse.getEncryptedWalletBytes())
                        .encryptedPassphrase(walletBytesEncryptResponse.getEncryptedPassphrase())
                        .mainnetWalletId(onboardShaktiUserRequest.getMainnetWalletId())
                        .testnetWalletId(onboardShaktiUserRequest.getTestnetWalletId())
                        .build()
        );
    }

    /**
     * fully on board the user with mobile and kyc user creations
     *
     * @param onboardShakti the on-boarding user model
     * @return the confirmation response of successful or failed on-boarding
     */
    private Mono<ResponseBean> onboardWebBrowserUser(OnboardShaktiUserRequest onboardShakti, String ipAddress) {
        log.info("Starting web customer on boarding flow for email id {}", onboardShakti.getEmail());
        return onboardUser(onboardShakti, false, ipAddress);
    }

    /**
     * flow here does not need wallet creation or kyc user creation as it will be managed by the mobile application
     *
     * @param onboardShakti the on-boarding user model
     * @return the confirmation response of successful or failed on-boarding
     */
    private Mono<ResponseBean> onboardMobileUser(OnboardShaktiUserRequest onboardShakti, String ipAddress) {
        log.info("Starting mobile customer on boarding flow for email id {}", onboardShakti.getEmail());
        return onboardUser(onboardShakti, true, ipAddress)
                .doOnNext(responseBean ->
                        userRegisterStateRepository
                                .findByShaktiId(onboardShakti.getShaktiID())
                                .flatMap(userRegisterState -> {
                                    log.info("Updating user state for wallet creation for email {} ", onboardShakti.getEmail());
                                    return userRegisterStateDetailRepository.save(
                                            UserRegisterStateDetail.create(userRegisterState, UserRegisterStateType.WALLET_CREATED)
                                    );
                                })
                                .subscribe()
                );
    }


    private Mono<OnboardShaktiUserRequest> addShaktiId(OnboardShaktiUserRequest onboardShaktiUserRequest) {
        onboardShaktiUserRequest.setShaktiID(UUID.randomUUID().toString());
        return Mono.just(onboardShaktiUserRequest);
    }

    private boolean isMobileClient(OnboardShaktiUserRequest onboardShaktiUserRequest) {
        return StringUtils.isNotBlank(onboardShaktiUserRequest.getWalletBytes()) &&
                StringUtils.isNotBlank(onboardShaktiUserRequest.getMainnetWalletId()) &&
                StringUtils.isNotBlank(onboardShaktiUserRequest.getTestnetWalletId());
    }
}
