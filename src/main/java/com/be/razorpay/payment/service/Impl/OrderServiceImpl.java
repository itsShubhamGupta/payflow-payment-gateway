package com.be.razorpay.payment.service.Impl;

import com.be.razorpay.common.enums.OrderStatus;
import com.be.razorpay.common.exception.BusinessRuleViolationException;
import com.be.razorpay.common.exception.DuplicateResourceException;
import com.be.razorpay.common.exception.ResourceNotFoundException;
import com.be.razorpay.payment.dto.request.CreateOrderRequest;
import com.be.razorpay.payment.dto.response.OrderResponse;
import com.be.razorpay.payment.entity.OrderRecord;
import com.be.razorpay.payment.entity.Payment;
import com.be.razorpay.payment.mapper.OrderMapper;
import com.be.razorpay.payment.mapper.PaymentMapper;
import com.be.razorpay.payment.repository.OrderRepository;
import com.be.razorpay.payment.repository.PaymentRepository;
import com.be.razorpay.payment.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.be.razorpay.payment.dto.response.PaymentResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private  int defaultOrderExpiryMinutes = 15;

    @Override
    @Transactional
    public OrderResponse create(UUID merchantId, CreateOrderRequest request) {
        if (request.receipt() != null && orderRepository.existsByMerchantIdAndReceipt(merchantId, request.receipt())) {
            throw new DuplicateResourceException("ORDER_RECEIPT_DUPLICATE", "Order with receipt already exists: " + request.receipt());
        }
//
//        UUID customerId = null;
//        if (request.customer() != null) {
//            customerId = customerService.findOrCreate(merchantId,
//                    request.customer().email(),
//                    request.customer().name(),
//                    request.customer().phone()
//            );
//        }

        OrderRecord order = OrderRecord.builder()
                .receipt(request.receipt())
                .amount(request.amount())
                .notes(request.notes())
                .merchantId(merchantId)
                .orderStatus(OrderStatus.CREATED)
                .expiresAt(request.expiresAt() != null ? request.expiresAt() :
                        LocalDateTime.now().plusMinutes(defaultOrderExpiryMinutes))
                .build();

        order = orderRepository.save(order);


        return new OrderResponse(
                order.getId(),
                order.getMerchantId(),
                order.getCustomerId(),
                order.getReceipt(),
                order.getAmount(),
                order.getOrderStatus(),
                order.getAttempts(),
                order.getNotes(),
                order.getExpiresAt(),
                null
        );
    }

    @Override
    public OrderResponse getById(UUID merchantId, UUID orderId) {
        OrderRecord order = orderRepository.findByIdAndMerchantId(orderId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancel(UUID merchantId, UUID orderId) {
        OrderRecord order = orderRepository.findByIdAndMerchantId(orderId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if(order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.PAID) {
            throw new BusinessRuleViolationException("ORDER_CANNOT_CANCEL",
                    "Cannot cancel order with status: "+order.getOrderStatus().name());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

//        eventPublisher.publish(EventAggregateType.ORDER, order.getId(), "ORDER_CANCELLED",
//                Map.of("orderId", order.getId(),
//                        "merchantId", merchantId.toString(),
//                        "orderStatus", order.getOrderStatus().name(),
//                        "amountUnits", order.getAmount().getAmountUnits(),
//                        "amountCurrency", order.getAmount().getCurrency()
//                )
//        );

        return orderMapper.toResponse(order);
    }

    @Override
    public List<PaymentResponse> listPayments(UUID merchantId, UUID orderId) {
        OrderRecord order = orderRepository.findByIdAndMerchantId(orderId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        List<Payment> paymentList = paymentRepository.findByOrder_Id(order.getId());

//        return paymentList.stream().map(
//                payment -> paymentMapper.toResponse(payment)
//        ).collect(Collectors.toList());

        return paymentMapper.toResponseList(paymentList);
    }
}
