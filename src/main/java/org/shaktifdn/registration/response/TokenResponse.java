package org.shaktifdn.registration.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

	private String access_token;
	private String refresh_token;
	private String scope;
	private String token_type;
	private String expires_in;
	
}
