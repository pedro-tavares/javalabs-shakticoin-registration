package org.shaktifdn.registration.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@Jacksonized
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class WalletBytesEncryptRequest {

    @NotBlank
    private String walletBytes;
    @NotBlank
    private String passphrase;
}
