package org.shaktifdn.registration.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum UserRegisterStateType {
    GLUU_CREATED,
    KYC_USER_CREATED,
    KYC_USER_WALLET_UPDATED,
    SELFY_ID_ENCRYPTED,
    WALLET_CREATED,
    BOUNTY_CREATED;

    public static List<String> valuesAsString() {
        return Arrays.stream(UserRegisterStateType.values()).map(Enum::name).collect(Collectors.toList());
    }
}
