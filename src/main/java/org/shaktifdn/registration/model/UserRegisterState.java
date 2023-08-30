package org.shaktifdn.registration.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.couchbase.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(expiry = 2, expiryUnit = TimeUnit.DAYS)
public class UserRegisterState {
    public static final String TYPE = "UserRegisterState";
    @Id
    private String id;
    private String _type = TYPE;
    private String shaktiId;
    private String email;
    private String countryCode;
    private String mobileNumber;
    private boolean isMobileUser;
    private Instant createdAt;
    private Instant lastModification;

    @Version
    private long version;

    public static UserRegisterState create(OnboardShaktiUserRequest onboardShaktiUserRequest, Boolean isMobileUser) {
        UserRegisterState userRegisterState = new UserRegisterState();
        userRegisterState.id = UUID.randomUUID().toString();
        userRegisterState.shaktiId = onboardShaktiUserRequest.getShaktiID();
        userRegisterState.email = onboardShaktiUserRequest.getEmail();
        userRegisterState.countryCode = onboardShaktiUserRequest.getCountryCode();
        userRegisterState.mobileNumber = onboardShaktiUserRequest.getMobileNo();
        userRegisterState.isMobileUser = isMobileUser;
        userRegisterState.createdAt = Instant.now();
        return userRegisterState;
    }
}
