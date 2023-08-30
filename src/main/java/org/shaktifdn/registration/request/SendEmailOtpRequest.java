package org.shaktifdn.registration.request;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.shaktifdn.registration.constant.Message;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@Builder
@Jacksonized
public class SendEmailOtpRequest {

    @NotBlank(message = Message.EMAIL_MANDATORY)
    @Email(message = Message.INVALID_EMAIL)
    private String email;
    private String requestedFlow;
}
