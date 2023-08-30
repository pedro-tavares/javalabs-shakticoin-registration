package org.shaktifdn.registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@EnableScheduling
public class ShaktiRegistrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShaktiRegistrationApplication.class, args);
    }

    @Bean
    public Scheduler scheduler() {
        return Schedulers.boundedElastic();
    }
}
