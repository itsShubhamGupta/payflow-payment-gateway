package com.be.razorpay.payment.gateway.adapter;

import com.be.razorpay.common.enums.PaymentMethod;
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
public class UpiPaymentAdapter implements PaymentAdapter {

    private final PaymentProcessorRouter paymentProcessorRouter;
    @Override
    public PaymentResult initiate(PaymentRequest request) {
        log.info("Initiate Payment with UPI, paymentId: {}", request.paymentId());

        try {
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

                case PaymentProcessorResponse.Success success -> new PaymentResult.Success(success.bankReference());

            };
        } catch(Exception e) {
            log.warn("UPI failed, paymentId: {}", request.paymentId());
            return new PaymentResult.Failure("UPI_FAILED", e.getMessage());
        }
    }

    @Override
    public PaymentResult capture(UUID paymentId) {
        return new PaymentResult.Success("UPI_REF");
    }
}
