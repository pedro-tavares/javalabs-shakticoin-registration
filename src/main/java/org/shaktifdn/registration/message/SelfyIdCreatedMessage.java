package org.shaktifdn.registration.message;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SelfyIdCreatedMessage {
    String shaktiId;
    String email;

    public static SelfyIdCreatedMessage of(String shaktiId, String email) {
        return new SelfyIdCreatedMessage(shaktiId, email);
    }
}
