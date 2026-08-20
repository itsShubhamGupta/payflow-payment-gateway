package com.be.razorpay.vault.service;

import com.be.razorpay.common.entity.Money;
import com.be.razorpay.payment.processor.dto.PaymentProcessorResponse;
import com.be.razorpay.vault.dto.request.TokenizeRequest;
import com.be.razorpay.vault.dto.response.TokenizeResponse;
import com.be.razorpay.vault.entity.CardToken;

import java.util.Map;
import java.util.UUID;

public interface VaultService {
    TokenizeResponse tokenize(TokenizeRequest request, UUID merchantId);
    public PaymentProcessorResponse charge(UUID paymentId, String token,
                                           Money amount, Map<String, Object> methodDetails) ;
}
