package com.be.razorpay.payment.service;

import com.be.razorpay.payment.dto.request.PaymentInitRequest;
import com.be.razorpay.payment.dto.response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {

    PaymentResponse initiate(UUID merchantId, PaymentInitRequest request);
    PaymentResponse capture(UUID merchantId, UUID paymentId);
    void resolveAuthorization(UUID paymentId, boolean approve, String bankRef, String errorCode, String errorDescription);



}
