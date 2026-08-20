package com.be.razorpay.vault.controller;

import com.be.razorpay.vault.dto.request.TokenizeRequest;
import com.be.razorpay.vault.dto.response.TokenizeResponse;
import com.be.razorpay.vault.service.VaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/vault")
public class VaultController {

    private final VaultService vaultService;
//    private final MerchantContext merchantContext;
private final UUID merchantId= UUID.fromString("e3f1c8b0-5d6a-4f9b-8c2e-1a2b3c4d5e6f"); // Replace with actual merchant ID


    @PostMapping("/tokenize")
    public ResponseEntity<TokenizeResponse> tokenize(@Valid @RequestBody TokenizeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vaultService.tokenize(request,merchantId));
    }
}
