package com.be.razorpay.payment.processor.strategy;


import com.be.razorpay.payment.processor.PaymentProcessor;
import com.be.razorpay.payment.processor.dto.PaymentProcessorRequest;
import com.be.razorpay.payment.processor.dto.PaymentProcessorResponse;
import org.springframework.stereotype.Component;

@Component
public class UpiPaymentProcessor  implements PaymentProcessor {



    @Override
    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {
        return null;
    }
}
