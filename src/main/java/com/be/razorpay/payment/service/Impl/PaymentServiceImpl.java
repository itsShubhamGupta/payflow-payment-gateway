package com.be.razorpay.payment.service.Impl;

import com.be.razorpay.common.enums.OrderStatus;
import com.be.razorpay.common.enums.PaymentEvent;
import com.be.razorpay.common.enums.PaymentStatus;
import com.be.razorpay.common.exception.BusinessRuleViolationException;
import com.be.razorpay.common.exception.ResourceNotFoundException;
import com.be.razorpay.payment.dto.request.PaymentInitRequest;
import com.be.razorpay.payment.dto.response.PaymentResponse;
import com.be.razorpay.payment.entity.OrderRecord;
import com.be.razorpay.payment.entity.Payment;
import com.be.razorpay.payment.gateway.PaymentGatewayRouter;
import com.be.razorpay.payment.gateway.dto.PaymentResult;
import com.be.razorpay.payment.gateway.dto.request.PaymentRequest;
import com.be.razorpay.payment.mapper.PaymentMapper;
import com.be.razorpay.payment.repository.OrderRepository;
import com.be.razorpay.payment.repository.PaymentRepository;
import com.be.razorpay.payment.service.PaymentService;
import com.be.razorpay.payment.statemachine.PaymentTransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayRouter paymentGatewayRouter;
    private final PaymentMapper paymentMapper;
    private  final PaymentTransitionService paymentTransitionService;
    @Override
    public PaymentResponse initiate(UUID merchantId, PaymentInitRequest request) {

        OrderRecord order = orderRepository.findByIdAndMerchantId(request.orderId(), merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.orderId()));

        if(order.getOrderStatus() != OrderStatus.CREATED && order.getOrderStatus() != OrderStatus.ATTEMPTED) {
            throw new BusinessRuleViolationException("ORDER_NOT_PAYABLE",
                    "Order cannot accept payment in status: "+order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.ATTEMPTED);
        order.setAttempts(order.getAttempts()+1);

        Payment payment = Payment.builder()
                .order(order)
                .merchantId(merchantId)
                .amount(order.getAmount())
                .status(PaymentStatus.CREATED)
                .method(request.method())
                .idempotencyKey(UUID.randomUUID().toString()) //TODO: idempotency
                .methodDetails(request.methodDetails())
                .build();
        payment = paymentRepository.save(payment);

        PaymentRequest paymentRequest = new PaymentRequest(payment.getId(),
                request.orderId(), merchantId,
                order.getAmount(), request.method(),
                request.methodDetails());

        paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_ATTEMPT);
        PaymentResult result= paymentGatewayRouter.initiate(paymentRequest);
        switch (result) {
            case PaymentResult.Pending pending ->
                    payment.setProcessorReference(pending.registrationRef());
            case PaymentResult.Failure failure -> {
//                payment.setStatus(PaymentStatus.FAILED);
                paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_FAIL);
                payment.setErrorCode(failure.errorCode());
                payment.setErrorDescription(failure.errorDescription());
            }
            case PaymentResult.Success success -> {
                log.warn("Invalid state");
                return null;
            }
        }

        payment = paymentRepository.save(payment);
        orderRepository.save(order);

        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse capture(UUID merchantId, UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndMerchantId (paymentId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));


//        payment.setStatus(PaymentStatus.CAPTURING);
        paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_REQUEST);
        PaymentResult paymentResult = paymentGatewayRouter.capture(payment.getMethod(), paymentId);

        if(paymentResult instanceof  PaymentResult.Success success) {
            paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_SUCCESS);
//            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setCapturedAt(LocalDateTime.now());
            log.info("Payment captured, paymentID: {}", paymentId);
        } else if(paymentResult instanceof  PaymentResult.Failure failure) {
            paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_FAIL);
            payment.setErrorCode(failure.errorCode());
            payment.setErrorDescription(failure.errorDescription());
            log.warn("Payment capture failed, paymentID: {}", paymentId);
        }

        payment = paymentRepository.save(payment);

        return paymentMapper.toResponse(payment);

    }

    @Transactional
    @Override
    public void resolveAuthorization(UUID paymentId, boolean approve, String bankRef, String errorCode, String errorDescription) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
//
//        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
//                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (payment.getStatus() != PaymentStatus.AUTHORIZING) {
            log.warn("Payment is not in Authorizing state, paymentID: {}, status: {}", paymentId, payment.getStatus());
            return;
        }

        OrderRecord orderRecord = payment.getOrder();

        if (approve) {
            paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_SUCCESS);
            payment.setBankReference(bankRef);
            payment.setAuthorizedAt(LocalDateTime.now());

            // Auto-capture
            paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_REQUEST);
            PaymentResult captureResult = paymentGatewayRouter.capture(payment.getMethod(), paymentId);

            if(captureResult instanceof PaymentResult.Success success) {
                paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_SUCCESS);
                payment.setCapturedAt(LocalDateTime.now());
                orderRecord.setOrderStatus(OrderStatus.PAID);
            } else if (captureResult instanceof  PaymentResult.Failure failure){
                paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_FAIL);
                payment.setErrorCode(failure.errorCode());
                payment.setErrorDescription(failure.errorDescription());
            }
        } else {
            paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_FAIL);
            payment.setErrorCode(errorCode);
            payment.setErrorDescription(errorDescription);
        }

        paymentRepository.save(payment);
        orderRepository.save(orderRecord);


    }
}
