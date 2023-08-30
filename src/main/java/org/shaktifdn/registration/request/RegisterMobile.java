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
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RegisterMobile {
    @NotBlank(message = "Country code cannot be empty")
    private String countryCode;

    @NotBlank(message = "Mobile number cannot be empty")
    @Size(min = 8, max = 15)
    @Pattern(regexp = "^([0-9]*)$", message = "mobile number should be a number, even + not allowed here")
    private String mobileNo;

    private String requestedFlow = Constant.MOBILE_REGISTERATION_FLOW;
}
