package org.shaktifdn.registration.response;

import lombok.Data;

@Data
public class CreateWalletResponse {
	
	private String message;
	private String walletBytes;
	private String mainnetWalletID;
	private String testnetWalletID;

}
