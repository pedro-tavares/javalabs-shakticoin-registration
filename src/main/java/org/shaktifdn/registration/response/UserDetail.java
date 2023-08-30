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
public class UserDetail {
	
	private String shaktiId;
	private String email;
	private String countryCode;
	private String mobileNo;
	private String sub;
}
