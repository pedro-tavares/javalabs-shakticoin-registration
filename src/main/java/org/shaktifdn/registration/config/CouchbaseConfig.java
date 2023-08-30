package org.shaktifdn.registration.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.repository.auditing.EnableReactiveCouchbaseAuditing;


@Configuration
@Slf4j
@EnableReactiveCouchbaseAuditing
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {
	private final String bucketName;
	private final String connectionString;
	private final CouchbaseProperties couchbaseProperties;

	public CouchbaseConfig(@Value("${spring.data.couchbase.bucket-name}") String bucketName,
                           @Value("${spring.couchbase.connection-string}") String connectionString,
                           CouchbaseProperties couchbaseProperties) {
		this.bucketName = bucketName;
		this.connectionString = connectionString;
		this.couchbaseProperties = couchbaseProperties;
	}

	@Override
	public String getConnectionString() {
		return connectionString;
	}

	@Override
	public String getUserName() {
		return couchbaseProperties.getUsername();
	}

	@Override
	public String getPassword() {
		return couchbaseProperties.getPassword();
	}

	@Override
	public String getBucketName() {
		return bucketName;
	}

	@Bean
	public NativeAuditorAware nativeAuditorAware() {
		return new NativeAuditorAware();
	}

	@Override
	public String toString() {
		return "CouchbaseConfig{" +
				"bucketName='" + bucketName + '\'' +
				", connectionString='" + connectionString + '\'' +
				", couchbaseProperties=" + couchbaseProperties.getUsername() +
				'}';
	}
}
