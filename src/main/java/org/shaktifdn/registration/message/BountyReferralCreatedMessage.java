package org.shaktifdn.registration.message;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class BountyReferralCreatedMessage {

    private String shaktiId;
    private String genesisBonusBountyId;
}
