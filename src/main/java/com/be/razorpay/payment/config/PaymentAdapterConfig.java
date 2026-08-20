package com.be.razorpay.payment.config;


import com.be.razorpay.common.enums.PaymentMethod;
import com.be.razorpay.payment.gateway.PaymentAdapter;
import com.be.razorpay.payment.gateway.adapter.CardPaymentAdapter;
import com.be.razorpay.payment.gateway.adapter.NetBankingAdapter;
import com.be.razorpay.payment.gateway.adapter.UpiPaymentAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PaymentAdapterConfig {

    private final CardPaymentAdapter cardPaymentAdapter;
    private final NetBankingAdapter netBankingAdapter;
    private final UpiPaymentAdapter upiPaymentAdapter;

    @Bean
    public Map<PaymentMethod, PaymentAdapter> paymentAdapterMap() {
        // Return a map of PaymentMethod to PaymentAdapter implementations
        return Map.of(
                PaymentMethod.CARD, cardPaymentAdapter,
                PaymentMethod.NETBANKING, netBankingAdapter,
                PaymentMethod.UPI, upiPaymentAdapter
                // Add other payment methods and their corresponding adapters here
        );
    }
}
