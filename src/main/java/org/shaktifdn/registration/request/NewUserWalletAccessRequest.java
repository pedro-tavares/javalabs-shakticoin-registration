package org.shaktifdn.registration.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@Jacksonized
@Builder
public class NewUserWalletAccessRequest {

	@NotBlank(message = "ShaktiId is required")
	private String shaktiId;
	@NotBlank(message = "Device is required")
	private String deviceId;
	private String location;
	private String ipAddress;
}
