package org.shaktifdn.registration.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.shaktifdn.registration.constant.Constant;
import org.shaktifdn.registration.constant.Message;

import javax.validation.Valid;
import javax.validation.constraints.*;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
public class OnboardShaktiUserRequest {

    String shaktiID;

    @NotBlank(message = Message.EMAIL_MANDATORY)
    @Email(message = Message.INVALID_EMAIL)
    String email;

    @Pattern(regexp = Constant.IPADDRESS_PATTERN, message = Message.INVALID_IP)
    String ipaddress;

    @NotBlank(message = Message.COUNTRYCODE_MANDATORY)
    @Pattern(regexp = Constant.COUNTRY_CODE_PATTERN, message = Message.INVALID_COUNTRY_CODE_NUMBER)
    String countryCode;

    @Size(min = 8, max = 15, message = Message.INVALID_MOBILE_NUMBER)
    @Pattern(regexp = "[0-9]+", message = Message.INVALID_MOBILE_NUMBER)
    String mobileNo;

    boolean emailVerified;
    boolean mobileVerified;

    @NotNull(message = Message.PASSWORD_MANDATORY)
    @Pattern(regexp = Constant.PASSOWORD_PATTERN, message = Message.INVALID_PASSWORD)
    String password;

    @NotBlank(message = Message.DEVICE_ID_MANDATORY)
    String deviceId;

    String pin;

    @Valid
    GeoJSONModel geojson;

    String authorizationBytes;

    String mainnetWalletId;
    String testnetWalletId;
    String walletBytes;
    String passphrase;

}
