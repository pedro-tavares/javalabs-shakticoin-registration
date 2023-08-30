package org.shaktifdn.registration.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "spring.couchbase")
@Slf4j
@Getter
@Setter
public class CouchbaseProperties {

	private String username;
	private String password;

	@PostConstruct
	public void postInit() {
		log.info("loaded couchbase properties from vault: {}, {}", Base64.getEncoder().encode(getPassword().getBytes(StandardCharsets.UTF_8)), getUsername());

	}
}
