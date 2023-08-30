package org.shaktifdn.registration.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "clients")
@Slf4j
@Getter
@Setter
public class ServiceProperties {

	private String emailService;
	private String smsService;
	private String walletService;
	private String kycService;
	private String selfyIdService;
	private String bizVaultService;

	@PostConstruct
	public void postInit() {
		log.info("loaded external emailService properties: {}", getEmailService());
		log.info("loaded external mobileOtpService properties: {}", getSmsService());
		log.info("loaded external walletService properties: {}", getWalletService());
		log.info("loaded external kycService properties: {}", getKycService());
		log.info("loaded external selfyIdService properties: {}", getSelfyIdService());
		log.info("loaded external bizVaultService properties: {}", getBizVaultService());

	}
}
