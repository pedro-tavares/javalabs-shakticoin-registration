package org.shaktifdn.registration.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.request.WalletRequest;
import org.shaktifdn.registration.response.ShaktiResponse;
import org.shaktifdn.registration.service.WalletService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet", description = "Create wallet for Web Browser flow. Do not use this on Mobile App")
public class WalletController {

    private final WalletService walletService;

    /**
     * @param walletRequest wallet request
     * @return ResponseBean mono of response bean
     * @apiNote This API wll create a wallet for the logged-in user if the wallet does not exist.
     */

    @PostMapping
    public Mono<ShaktiResponse<String>> create(@RequestBody @Valid WalletRequest walletRequest) {
        return walletService
                .create(walletRequest)
                .map(response -> ShaktiResponse
                        .<String>builder()
                        .status(true)
                        .message("Your wallet creation request is in progress")
                        .data(response.getWalletBytes())
                        .build()
                );
    }

}
