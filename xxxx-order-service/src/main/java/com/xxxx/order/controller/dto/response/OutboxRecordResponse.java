package com.xxxx.order.controller.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO trả thông tin một record outbox cho màn hình/admin API vận hành DLQ.
 *
 * <p>DTO này cố ý không trả toàn bộ payload vì payload có thể dài hoặc chứa dữ liệu nhạy cảm. Admin vẫn có đủ thông tin
 * quan trọng để xác định event bị lỗi: service/topic, key nghiệp vụ, class event, trạng thái, số lần retry và lỗi cuối.</p>
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
