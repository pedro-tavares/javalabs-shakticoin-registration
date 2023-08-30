package org.shaktifdn.registration;

import com.couchbase.client.java.Cluster;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.shaktifdn.registration.service.UserInfoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * @author Ganesh
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.yml")
@ContextConfiguration(classes = {AbstractIntegrationTest.TestLoadBalancerConfig.class})
@Testcontainers
@Slf4j
public abstract class AbstractIntegrationTest {
    @SuppressWarnings("rawtypes")
    @Container
    public static DockerComposeContainer composeContainer =
            new DockerComposeContainer(new File("src/test/resources/docker-compose-test.yml"))
                    .withExposedService("couchbase", 11210)
                    .waitingFor("couchbase", Wait.defaultWaitStrategy());
    private static Boolean hasInit = false;

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    private CouchbaseTemplate couchbaseTemplate;

    @MockBean
    private UserInfoClient userInfoClient;

    @BeforeEach
    public void beforeEach() {
        if (!hasInit) {
            // Waiting for query manager
            await().atMost(3, TimeUnit.SECONDS)
                    .with()
                    .pollInterval(Duration.ofSeconds(2))
                    .until(() -> true);
            Cluster cluster = couchbaseTemplate
                    .getCouchbaseClientFactory()
                    .getCluster();

            cluster.queryIndexes().createPrimaryIndex("services");
            cluster.queryIndexes().createIndex(
                    "services",
                    "adv_userRegisterStateId_type",
                    List.of("userRegisterStateId")
            );
            hasInit = true;
        }
    }

    // as service discovery is disabled for test
    @TestConfiguration
    @LoadBalancerClients({
            @LoadBalancerClient(name = "kyc-service", configuration = LoadBalancerConfig.class)
    }
    )
    protected static class TestLoadBalancerConfig {

        @Bean
        public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier() {
            return new TestServiceInstanceSupplier();
        }

        @Bean
        @LoadBalanced
        @Primary
        WebClient.Builder loadBalanced() {
            return WebClient.builder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }

        @Bean
        WebClient.Builder webClient() {
            return WebClient.builder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }


    }

    // as service discovery is disabled for test
    protected static class LoadBalancerConfig {
        @Bean
        public RoundRobinLoadBalancer roundRobinContextLoadBalancer(LoadBalancerClientFactory clientFactory,
                                                                    Environment env) {
            String serviceId = clientFactory.getName(env);
            return new RoundRobinLoadBalancer(
                    clientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class),
                    serviceId,
                    -1
            );
        }
    }
}