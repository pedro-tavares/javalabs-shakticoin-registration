package org.shaktifdn.registration.config;

import io.jaegertracing.internal.MDCScopeManager;
import io.opentracing.contrib.java.spring.jaeger.starter.TracerBuilderCustomizer;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition()
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI().components(new Components())
				.info(new Info().title("Registration APIs").version("1.1").description("Registration APIs")
						.termsOfService("https://www.shakticoin.com/")
						.license(new License().name("Apache 2.0").url("https://springdoc.org")));
	}

	@Bean
	public TracerBuilderCustomizer mdcBuilderCustomizer() {
		return builder -> builder.withScopeManager(new MDCScopeManager.Builder().build());
	}

}