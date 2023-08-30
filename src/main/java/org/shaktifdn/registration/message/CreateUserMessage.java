package org.shaktifdn.registration.message;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class CreateUserMessage {
    private String shaktiId;
    private String email;
    private String countryCode;
    private String mobileNo;
    private String authorizationBytes;
    private String mainnetWalletId;
    private String testnetWalletId;
    private String encryptedWalletBytes;
    private String encryptedPassphrase;
}
