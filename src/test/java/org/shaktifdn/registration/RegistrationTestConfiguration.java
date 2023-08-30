package org.shaktifdn.registration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@TestConfiguration
public class RegistrationTestConfiguration {

    @Bean
    public Scheduler scheduler() {
        return Schedulers.boundedElastic();
    }

    @Bean("loadBalanced")
    public WebClient.Builder loadBalanced() {
        return WebClient.builder();
    }

    @Bean("loadBalancedSameBearerToken")
    public WebClient.Builder loadBalancedSameBearerToken() {
        return WebClient.builder();
    }
}
