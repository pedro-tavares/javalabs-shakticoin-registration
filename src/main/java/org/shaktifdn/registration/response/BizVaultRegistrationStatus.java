package org.shaktifdn.registration.response;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class BizVaultRegistrationStatus {

    private boolean isBizVaultRegistered;
}
