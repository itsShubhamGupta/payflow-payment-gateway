package com.be.razorpay.merchant.service.Impl;

import com.be.razorpay.common.exception.ResourceNotFoundException;
import com.be.razorpay.common.util.RandomizerUtil;
import com.be.razorpay.merchant.dto.request.CreateApiKeyRequest;
import com.be.razorpay.merchant.dto.response.ApiKeyCreateResponse;
import com.be.razorpay.merchant.dto.response.ApiKeyResponse;
import com.be.razorpay.merchant.entity.ApiKey;
import com.be.razorpay.merchant.entity.Merchant;
import com.be.razorpay.merchant.repository.ApiKeyRepository;
import com.be.razorpay.merchant.repository.MerchantRepository;
import com.be.razorpay.merchant.service.ApiKeyService;
import com.be.razorpay.merchant.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApiKeyServiceImpl implements ApiKeyService {
    private final ApiKeyRepository apiKeyRepository;
    private final MerchantRepository merchantRepository;

    @Override
    @Transactional
    public ApiKeyCreateResponse create(UUID merchantId, CreateApiKeyRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("merchant", merchantId));

        String keyId = "rzp_"+request.environment().name().toLowerCase()+"_"+ RandomizerUtil.randomBase64(24);
        String rawSecret = RandomizerUtil.randomBase64(40);
        log.info("Creating API Key for merchantId: {}, keyId: {}, environment: {}", merchantId, keyId, request.environment(),rawSecret);

        ApiKey apiKey = ApiKey.builder()
                .merchant(merchant)
                .keyId(keyId)
                .keySecretHash(rawSecret)
                .environment(request.environment())
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        log.info("keyId: {} ",apiKey.getId());

        return new ApiKeyCreateResponse(apiKey.getId(), keyId, rawSecret, request.environment());    }

    @Override
    public List<ApiKeyResponse> listByMerchant(UUID merchantId) {
        return apiKeyRepository.findByMerchant_Id(merchantId).stream()
                .map(apiKey -> new ApiKeyResponse(apiKey.getId(), apiKey.getKeyId(), apiKey.getEnvironment()
                     , apiKey.isEnabled(), apiKey.getLastUsedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void revoke(UUID merchantId, UUID keyId) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .filter(k -> k.getMerchant().getId().equals(merchantId))
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId));

        key.setEnabled(false);
    }

    @Override
    @Transactional
    public ApiKeyCreateResponse rotate(UUID merchantId, UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .filter(k -> k.getMerchant().getId().equals(merchantId))
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId));

        if(!apiKey.isEnabled()) throw new RuntimeException("Cannot rotate a disabled key");

        String newRawSecret = RandomizerUtil.randomBase64(40);
        apiKey.setPreviousKeySecretHash(apiKey.getKeySecretHash());
        apiKey.setKeySecretHash(newRawSecret);
        apiKey.setRotatedAt(LocalDateTime.now());
        apiKey.setGracePeriodExpiresAt(LocalDateTime.now().plusHours(24));
        apiKey = apiKeyRepository.save(apiKey);

//        apiKeyCache.evict(apiKey.getKeyId());

        return new ApiKeyCreateResponse(apiKey.getId(), apiKey.getKeyId(),
                newRawSecret, apiKey.getEnvironment());    }
}
