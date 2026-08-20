package com.be.razorpay.payment.gateway.adapter;

import com.be.razorpay.payment.gateway.PaymentAdapter;
import com.be.razorpay.payment.gateway.dto.PaymentResult;
import com.be.razorpay.payment.gateway.dto.request.PaymentRequest;
import com.be.razorpay.payment.processor.PaymentProcessorRouter;
import com.be.razorpay.payment.processor.dto.PaymentProcessorRequest;
import com.be.razorpay.payment.processor.dto.PaymentProcessorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NetBankingAdapter implements PaymentAdapter {

    private final PaymentProcessorRouter paymentProcessorRouter;
    @Override
    public PaymentResult initiate(PaymentRequest request) {

        try{
            PaymentProcessorRequest paymentProcessorRequest = PaymentProcessorRequest
                    .nonCard(UUID.randomUUID(), request.paymentId(),
                            request.method(), request.amount(),
                            null, null, request.methodDetails());

            PaymentProcessorResponse paymentProcessorResponse =
                    paymentProcessorRouter.charge(paymentProcessorRequest);

            return switch (paymentProcessorResponse) {
                case PaymentProcessorResponse.Failure failure ->
                        new PaymentResult.Failure(failure.errorCode(), failure.errorDescription());

                case PaymentProcessorResponse.Pending pending ->
                        new PaymentResult.Pending(pending.processorReference());

                case PaymentProcessorResponse.Success success ->
                        new PaymentResult.Success(success.bankReference());

            };
        } catch (Exception e) {
            log.warn("NetBanking failed, paymentId: {}", request.paymentId());
            return new PaymentResult.Failure("NBK_FAILED", e.getMessage());
        }
    }

    @Override
    public PaymentResult capture(UUID paymentId) {
        return new PaymentResult.Success("NBK_REF");
    }
}
