package org.shaktifdn.registration.config;


import org.shaktifdn.registration.security.AuthoritiesConstants;
import org.shaktifdn.registration.security.SecurityUtils;
import org.shaktifdn.registration.security.oauth2.JwtGrantedAuthorityConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Profile("!test")
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .securityMatcher(new NegatedServerWebExchangeMatcher(new OrServerWebExchangeMatcher(
                        pathMatchers("/swagger-ui/**", "/swagger-resources/**", "/v2/api-docs/**", "/v3/api-docs/**", "/test/**"),
                        pathMatchers(HttpMethod.OPTIONS, "/**")
                )))
                .csrf()
                .disable()
                .exceptionHandling()
                .and()
                .headers()
                .contentSecurityPolicy("default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com https://www.google-analytics.com; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://www.google-analytics.com; font-src 'self' data:")
                .and()
                .referrerPolicy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
                .permissionsPolicy(permissionsPolicySpec -> permissionsPolicySpec.policy("geolocation 'none'; midi 'none'; sync-xhr 'none'; microphone 'none'; camera 'none'; magnetometer 'none'; gyroscope 'none'; fullscreen 'self'; payment 'none'"))
                .frameOptions().disable()
                .and()
                .requestCache()
                .requestCache(NoOpServerRequestCache.getInstance())
                .and()
                .authorizeExchange((exchanges) -> exchanges.matchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll())
                .authorizeExchange()
                .pathMatchers("/swagger*").permitAll()
                .pathMatchers("/webjars/**").permitAll()
                .pathMatchers("/api-docs/**").permitAll()
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/actuator/health/**").permitAll()
                .pathMatchers("/actuator/info").permitAll()
                .pathMatchers("/actuator/prometheus").permitAll()
                .pathMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
                .pathMatchers("/**").authenticated()
                .and()
                .oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                .and().and()
                .oauth2Client();
        // @formatter:on
        return http.build();
    }

    /**
     * Map authorities from "groups" or "roles" claim in ID Token.
     *
     * @return a {@link ReactiveOAuth2UserService} that has the groups from the IdP.
     */
    @Bean
    public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcReactiveOAuth2UserService delegate = new OidcReactiveOAuth2UserService();

        return userRequest -> {
            // Delegate to the default implementation for loading a user
            return delegate
                    .loadUser(userRequest)
                    .map(
                            user -> {
                                Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

                                user
                                        .getAuthorities()
                                        .forEach(
                                                authority -> {
                                                    if (authority instanceof OidcUserAuthority) {
                                                        OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) authority;
                                                        mappedAuthorities.addAll(
                                                                SecurityUtils.extractAuthorityFromClaims(oidcUserAuthority.getUserInfo().getClaims())
                                                        );
                                                    }
                                                }
                                        );

                                return new DefaultOidcUser(mappedAuthorities, user.getIdToken(), user.getUserInfo());
                            }
                    );
        };
    }

    Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtGrantedAuthorityConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder jwtDecoder = (NimbusReactiveJwtDecoder) ReactiveJwtDecoders.fromOidcIssuerLocation(issuerUri);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        jwtDecoder.setClaimSetConverter(new UsernameSubClaimAdapter());
        jwtDecoder.setJwtValidator(withIssuer);
        return jwtDecoder;
    }

    private static class UsernameSubClaimAdapter implements Converter<Map<String, Object>, Map<String, Object>> {
        private final MappedJwtClaimSetConverter delegate =
                MappedJwtClaimSetConverter.withDefaults(Collections.emptyMap());

        public Map<String, Object> convert(Map<String, Object> claims) {
            Map<String, Object> convertedClaims = this.delegate.convert(claims);
            String username = (String) convertedClaims.getOrDefault("preferred_username", "user_management_service");
            convertedClaims.put("sub", username);
            return convertedClaims;
        }
    }

}
