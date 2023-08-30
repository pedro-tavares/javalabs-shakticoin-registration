package org.shaktifdn.registration.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.shaktifdn.registration.constant.Message;

import javax.validation.constraints.NotNull;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletRequest {

    String authorizationBytes;

    @NotNull(message = Message.PASSPHRASE_MANDATORY)
    String passphrase;
}
