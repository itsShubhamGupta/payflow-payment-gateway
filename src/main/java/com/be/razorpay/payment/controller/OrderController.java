package com.be.razorpay.payment.controller;

import com.be.razorpay.payment.dto.request.CreateOrderRequest;
import com.be.razorpay.payment.dto.response.OrderResponse;
import com.be.razorpay.payment.service.OrderService;
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
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

//    private final MerchantContext merchantContext;

    private final OrderService orderService;
    private  final UUID merchantId= UUID.fromString("9fe376e9-0c1e-4dfa-8fa4-b151ad91fd72");

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody @Valid CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(merchantId, request));
    }
}
