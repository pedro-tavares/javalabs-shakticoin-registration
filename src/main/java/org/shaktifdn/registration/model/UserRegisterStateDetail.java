package org.shaktifdn.registration.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(expiry = 2, expiryUnit = TimeUnit.DAYS)
public class UserRegisterStateDetail {
    public static final String TYPE = "UserRegisterStateDetail";
    @Id
    private String id;
    private String _type = TYPE;
    private String userRegisterStateId;
    private UserRegisterStateType stateType;
    private Instant createdAt;

    public static UserRegisterStateDetail create(UserRegisterState state, UserRegisterStateType type) {
        UserRegisterStateDetail detail = new UserRegisterStateDetail();
        detail.id = UUID.randomUUID().toString();
        detail.userRegisterStateId = state.getId();
        detail.stateType = type;
        detail.createdAt = Instant.now();
        return detail;
    }
}
