package com.be.razorpay.operation.entity;

import com.be.razorpay.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity

public class SettlementPayment  extends BaseEntity {
    @EmbeddedId
    private SettlementPaymentId id;

    @MapsId("settlementId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;
}
