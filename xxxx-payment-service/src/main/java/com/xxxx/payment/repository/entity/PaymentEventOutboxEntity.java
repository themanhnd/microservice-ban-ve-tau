package com.xxxx.payment.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/** Outbox lưu event cần publish Kafka của payment-service. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor @Entity
@Table(name = "payment_event_outbox", indexes = {
        @Index(name = "idx_payment_outbox_status_next", columnList = "status,next_attempt_at"),
        @Index(name = "idx_payment_outbox_topic_key", columnList = "topic,event_key")
})
public class PaymentEventOutboxEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "topic", nullable = false, length = 128) private String topic;
    @Column(name = "event_key", nullable = false, length = 128) private String eventKey;
    @Column(name = "event_type", nullable = false, length = 128) private String eventType;
    @Lob @Column(name = "payload", nullable = false) private String payload;
    @Column(name = "status", nullable = false, length = 32) private String status;
    @Column(name = "attempt_count", nullable = false) private Integer attemptCount;
    @Column(name = "next_attempt_at", nullable = false) private LocalDateTime nextAttemptAt;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "last_error", length = 1024) private String lastError;
    @Version @Column(name = "version_id") private Long versionId;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
