package org.shaktifdn.registration.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.shaktifdn.registration.constant.Constant;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@Builder
@AllArgsConstructor
@ToString(exclude = {"otp"})
@NoArgsConstructor
public class ConfirmMobileOtp {

    @NotBlank(message = "Country code cannot be empty")
    private String countryCode;

    @NotBlank(message = "Mobile number cannot be empty")
    @Size(min = 8, max = 15)
    @Pattern(regexp = "^([0-9]*)$", message = "mobile number should be a number, even + not allowed here")
    private String mobileNo;

    @NotBlank(message = "OTP Can not be null")
    @Size(min = 4, max = 6)
    private String otp;

    private String requestedFlow = Constant.MOBILE_NO_SERVICE;
}
