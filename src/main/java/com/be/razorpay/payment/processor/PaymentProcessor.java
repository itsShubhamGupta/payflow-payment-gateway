package com.be.razorpay.payment.processor;

import com.be.razorpay.payment.processor.dto.PaymentProcessorRequest;
import com.be.razorpay.payment.processor.dto.PaymentProcessorResponse;

public interface PaymentProcessor {
    PaymentProcessorResponse charge(PaymentProcessorRequest request);

}
