package com.be.razorpay.payment.gateway;

import com.be.razorpay.payment.gateway.dto.PaymentResult;
import com.be.razorpay.payment.gateway.dto.request.PaymentRequest;

import java.util.UUID;

public interface PaymentAdapter {
    PaymentResult initiate(PaymentRequest request);
    PaymentResult capture(UUID paymentId);

}
