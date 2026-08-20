package com.be.razorpay.payment.dto.request;

import com.be.razorpay.common.entity.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;

public record CreateOrderRequest(

        @NotNull(message = "Amount is required")
        Money amount,

        @Size(max = 100)
        String receipt, // order-id (known to merchant)

        Map<String, Object> notes,

        LocalDateTime expiresAt

//        @Valid
//        CustomerDetails customer
) {

}

