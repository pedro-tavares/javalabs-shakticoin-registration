package org.shaktifdn.registration.message;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KYCUserCreatedMessage {

    String shaktiId;
    String email;
    String mobile;
}
