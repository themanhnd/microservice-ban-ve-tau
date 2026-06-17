package com.xxxx.payment.controller.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO hiển thị một event outbox của payment-service cho admin vận hành DLQ.
 *
 * <p>Payment-service thường phát các event như {@code payment.completed} hoặc {@code payment.failed}. Khi publish Kafka
 * lỗi quá số lần retry, record sẽ ở trạng thái {@code FAILED}; DTO này giúp admin biết record nào cần replay/ignore.</p>
 */
@Data
@Builder
public class OutboxRecordResponse {
    private Long id;
    private String serviceName;
    private String topic;
    private String eventKey;
    private String eventType;
    private String status;
    private Integer attemptCount;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime publishedAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
