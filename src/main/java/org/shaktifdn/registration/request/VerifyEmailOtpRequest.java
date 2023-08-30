package org.shaktifdn.registration.request;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.shaktifdn.registration.constant.Message;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Builder
@Jacksonized
public class VerifyEmailOtpRequest {

    @Email(message = Message.EMAIL_MANDATORY)
    @NotBlank(message = Message.INVALID_EMAIL)
    String email;

    @NotBlank(message = "OTP Can not be null")
    @Size(min = 4, max = 6)
    String otp;

    String requestedFlow;
}
