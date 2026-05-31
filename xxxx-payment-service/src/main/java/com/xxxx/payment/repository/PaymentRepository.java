package com.xxxx.payment.repository;

import com.xxxx.payment.repository.entity.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for PaymentTransactionEntity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransactionEntity, Long> {

    Optional<PaymentTransactionEntity> findByTransactionId(String transactionId);

    Optional<PaymentTransactionEntity> findByOrderId(String orderId);

    Optional<PaymentTransactionEntity> findByIdempotencyKey(String idempotencyKey);
}
