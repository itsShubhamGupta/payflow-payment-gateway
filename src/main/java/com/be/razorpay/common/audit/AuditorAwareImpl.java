package com.be.razorpay.common.audit;

import com.be.razorpay.merchant.security.MerchantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;



@Component("auditorAwareImpl")
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<String> {

    private  final MerchantContext merchantContext;

    @Override
    public Optional<String> getCurrentAuditor() {

        if (merchantContext.getKeyId() != null) {
            return Optional.of("Key id "+merchantContext.getKeyId());
        }
        if (merchantContext.getMerchantId() != null) {
            return Optional.of("merchant id "+merchantContext.getMerchantId().toString());
        }
        return Optional.of("System");
    }
}
