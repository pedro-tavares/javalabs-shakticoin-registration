package org.shaktifdn.registration.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.config.ServiceProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractWebClient {

    protected final WebClient.Builder loadBalanced;
    protected final ServiceProperties serviceProperties;

    protected <T> Mono<T> get(String url, Consumer<HttpHeaders> headers, Class<T> response) {
        return loadBalanced.build()
                .get()
                .uri(url)
                .headers(headers)
                .retrieve()
                .bodyToMono(response);
    }
    protected <T> Mono<T> get(String url,  ParameterizedTypeReference<T> response) {
        return loadBalanced.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(response);
    }

    protected <S, T> Mono<T> post(String url, S request, Class<T> response) {
        return post(loadBalanced, url, request, response);
    }

    protected <S, T> Mono<T> post(WebClient.Builder webClientBuilder, String url, S request, Class<T> response) {
        return webClientBuilder.build()
                .post()
                .uri(url)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToMono(response);
    }

}
