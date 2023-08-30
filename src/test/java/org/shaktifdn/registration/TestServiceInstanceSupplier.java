package org.shaktifdn.registration;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;
import java.util.Map;


public class TestServiceInstanceSupplier implements ServiceInstanceListSupplier {
    @Override
    public String getServiceId() {
        return "test-service-discovery";
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        ServiceInstance kycService = new ServiceInstance() {

            @Override
            public String getServiceId() {
                return "kyc-service";
            }

            @Override
            public String getHost() {
                return "localhost";
            }

            @Override
            public int getPort() {
                return 8999;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public URI getUri() {
                return URI.create("http://localhost:8999");
            }

            @Override
            public Map<String, String> getMetadata() {
                return null;
            }
        };

        ServiceInstance iamService = new ServiceInstance() {

            @Override
            public String getServiceId() {
                return "iam-service";
            }

            @Override
            public String getHost() {
                return "localhost";
            }

            @Override
            public int getPort() {
                return 8999;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public URI getUri() {
                return URI.create("http://localhost:8999");
            }

            @Override
            public Map<String, String> getMetadata() {
                return null;
            }
        };
        return Flux.just(List.of(kycService, iamService));
    }
}
