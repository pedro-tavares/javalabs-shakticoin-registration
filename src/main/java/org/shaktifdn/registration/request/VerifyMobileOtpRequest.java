package org.shaktifdn.registration.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
@ToString(exclude = {"otp"})
public class VerifyMobileOtpRequest {

    @NotBlank(message = "Country code cannot be empty")
    private String countryCode;

    @NotBlank(message = "Mobile number cannot be empty")
    @Size(min = 8, max = 15)
    @Pattern(regexp = "^([0-9]*)$", message = "mobile number should be a number, even + not allowed here")
    private String mobileNo;
    private String shaktiId;
    private String requestedFlow;
    private String otp;
}