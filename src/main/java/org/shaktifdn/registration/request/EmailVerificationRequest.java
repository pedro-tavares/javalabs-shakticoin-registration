package org.shaktifdn.registration.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.shaktifdn.registration.constant.Message;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class EmailVerificationRequest {
    @NotBlank(message = Message.EMAIL_MANDATORY)
    @Email(message = Message.INVALID_EMAIL)
    String email;
}
