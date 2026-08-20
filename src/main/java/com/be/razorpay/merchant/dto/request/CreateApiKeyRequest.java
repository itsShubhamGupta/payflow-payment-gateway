package com.be.razorpay.merchant.dto.request;

import com.be.razorpay.common.enums.Environment;

public record CreateApiKeyRequest(
        Environment environment
) {
}
