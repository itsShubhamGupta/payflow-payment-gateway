package com.be.razorpay.payment.processor.dto;

import com.be.razorpay.common.entity.Money;
import com.be.razorpay.common.enums.PaymentMethod;

import java.util.Map;
import java.util.UUID;

public record PaymentProcessorRequest(
        UUID processingId,
        UUID paymentId,
        PaymentMethod method,
        Money amount,
        String pan,
        String expiry,
        Map<String, Object> methodDetails
) {

    public static PaymentProcessorRequest card(UUID processingId, UUID paymentId, PaymentMethod method, Money amount, String pan, String expiry, Map<String, Object> methodDetails) {
        return new PaymentProcessorRequest(processingId, paymentId, method, amount, pan, expiry, methodDetails);
    }
    public static PaymentProcessorRequest nonCard(UUID processingId, UUID paymentId, PaymentMethod method, Money amount, String pan, String expiry, Map<String, Object> methodDetails) {
        return new PaymentProcessorRequest(processingId, paymentId, method, amount, null, null, methodDetails);
    }
}