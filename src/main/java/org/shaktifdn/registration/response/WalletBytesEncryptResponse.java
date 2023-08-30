package org.shaktifdn.registration.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@Jacksonized
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class WalletBytesEncryptResponse {

    @NotBlank
    private String encryptedWalletBytes;
    @NotBlank
    private String encryptedPassphrase;
}
