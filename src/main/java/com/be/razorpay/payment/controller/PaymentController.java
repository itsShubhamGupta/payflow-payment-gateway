package com.be.razorpay.payment.controller;

import com.be.razorpay.payment.dto.request.PaymentInitRequest;
import com.be.razorpay.payment.dto.response.PaymentResponse;
import com.be.razorpay.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/v1/payments")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
//    private final MerchantContext merchantContext;
    private final UUID merchantId= UUID.fromString("9fe376e9-0c1e-4dfa-8fa4-b151ad91fd72"); // Replace with actual merchant ID

    @PostMapping
    public ResponseEntity<PaymentResponse> initiate(@Valid @RequestBody PaymentInitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiate(merchantId, request));
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<PaymentResponse> capture(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.capture(merchantId, paymentId));
    }

}
