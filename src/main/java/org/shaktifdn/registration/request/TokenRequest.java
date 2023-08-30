package org.shaktifdn.registration.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
@AllArgsConstructor
public class TokenRequest {

	private String grant_type;
	private String scope;
	private String username;
	private String password;

	public TokenRequest(String username, String password) {
		this.username = username;
		this.password = password;
		this.grant_type="password";
		this.scope="openid";
	}
}
