package com.be.razorpay.payment.repository;

import com.be.razorpay.common.enums.PaymentStatus;
import com.be.razorpay.payment.entity.OrderRecord;
import com.be.razorpay.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
 List<Payment> findByOrder_Id(UUID orderId);

Optional<Payment> findByIdAndMerchantId(UUID paymentId, UUID merchantId);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus paymentStatus, LocalDateTime globalWindow);
}
