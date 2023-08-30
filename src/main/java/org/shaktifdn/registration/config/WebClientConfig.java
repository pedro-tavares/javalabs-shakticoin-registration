package org.shaktifdn.registration.config;

import gluu.scim2.client.factory.ScimClientFactory;
import gluu.scim2.client.rest.ClientSideService;
import org.shaktifdn.registration.service.GluuService;
import org.shaktifdn.registration.service.XForwardedRemoteAddressResolver;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.UUID;


@Configuration
public class WebClientConfig {

    private static final String domainURL = "https://iamdev.shakticoin.com/identity/restv1";
    private static final String OIDCMetadataUrl = "https://iamdev.shakticoin.com/.well-known/openid-configuration";

    @Bean
    @LoadBalanced
    WebClient.Builder loadBalanced(
            ReactiveClientRegistrationRepository clientRegistrations,
            ReactiveOAuth2AuthorizedClientService authorizedClientService
    ) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                        new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                                clientRegistrations,
                                authorizedClientService
                        )
                );
        oauth.setDefaultClientRegistrationId("gluu");
        return createWebClientBuilder(oauth);
    }

    @LoadBalanced
    @Bean("loadBalancedSameBearerToken")
    WebClient.Builder loadBalancedSameBearerToken() {
        return createWebClientBuilder(new ServerBearerExchangeFilterFunction());
    }

    @Bean
    WebClient.Builder webClient(
            ReactiveClientRegistrationRepository clientRegistrations,
            ReactiveOAuth2AuthorizedClientService authorizedClientService
    ) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                        new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                                clientRegistrations,
                                authorizedClientService
                        )
                );
        oauth.setDefaultClientRegistrationId("gluu");
        return WebClient.builder().filter(oauth)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }

    @Bean
    WebClient.Builder extWebClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }

    @Bean("scimClient")
    @Profile("!testMode")
    ClientSideService scimClient(GluuProperties gluuProperties) {
        return ScimClientFactory.getClient(
                gluuProperties.getGluuUri() + GluuService.GLUU_URL_IDENTITY2,
                gluuProperties.getUmaAatClientId(),
                gluuProperties.getUmaAatClientJksPath().getAbsolutePath(),
                gluuProperties.getUmaAatClientJksPassword(),
                gluuProperties.getUmaAatClientKeyId()
        );

    }

    @Bean("scimClientDev")
    @Profile("testMode")
    ClientSideService scimClientDev() throws Exception {
        return ScimClientFactory.getTestClient(domainURL, OIDCMetadataUrl);
    }

    @Bean
    XForwardedRemoteAddressResolver xForwardedRemoteAddressResolver() {
        return XForwardedRemoteAddressResolver.maxTrustedIndex(1);
    }

    private static WebClient.Builder createWebClientBuilder(ExchangeFilterFunction oauth) {
        ConnectionProvider provider = ConnectionProvider.builder("fixed" + UUID.randomUUID())
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120)).build();
        return WebClient.builder().filter(oauth)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider)))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
