package com.xxxx.payment.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a payment transaction.
 * Maps to the payment_transaction table in payment_db.
 */
@Entity
@Table(name = "payment_transaction", indexes = {
        @Index(name = "idx_transaction_id", columnList = "transactionId", unique = true),
        @Index(name = "idx_txn_ref", columnList = "txnRef", unique = true),
        @Index(name = "idx_order_id", columnList = "orderId"),
        @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(nullable = false, unique = true, length = 32)
    private String txnRef;

    @Column(nullable = false, length = 64)
    private String orderId;

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String paymentMethod = "VNPAY";

    /**
     * Payment status: PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(length = 128)
    private String gatewayTransactionId;

    @Column(length = 32)
    private String gatewayResponseCode;

    @Column(length = 1024)
    private String paymentUrl;

    @Column(unique = true, length = 128)
    private String idempotencyKey;

    @Column(length = 512)
    private String failureReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
