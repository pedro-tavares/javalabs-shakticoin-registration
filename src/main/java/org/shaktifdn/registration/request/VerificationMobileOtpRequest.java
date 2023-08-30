package org.shaktifdn.registration.request;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@Builder
@Jacksonized
public class VerificationMobileOtpRequest {

	@Size(min = 8, max = 15)
	@Pattern(regexp = "^([0-9]*)$", message = "mobile number should be a number, even + not allowed here")
	private String mobileNo;
	@NotBlank(message = "Country code cannot be empty")
	private String countryCode;
	private String requestedFlow;

}