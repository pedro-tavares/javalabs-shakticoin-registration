package org.shaktifdn.registration.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UserRegistrationStatusResponse {
    private Boolean isRegistered;
}
