package org.shaktifdn.registration.response;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class WalletStatusResponse {

    private String shaktiID;
    private String walletID;
}
