package org.shaktifdn.registration.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.shaktifdn.registration.enums.AccountType;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
public class CreateWalletRequest {

    @NotBlank(message = "authorizationBytes are required")
    private String authorizationBytes;
    private String passphrase;

    @NotBlank(message = "accountType are required")
    private AccountType accountType;

    @NotBlank(message = "shaktiID are required")
    private String shaktiID;

}
