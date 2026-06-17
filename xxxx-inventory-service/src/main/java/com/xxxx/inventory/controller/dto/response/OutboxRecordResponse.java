package com.xxxx.inventory.controller.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO hiển thị một record outbox của inventory-service cho admin.
 *
 * <p>DTO này tập trung vào thông tin vận hành: topic, key nghiệp vụ, trạng thái, retry count và lỗi cuối. Payload không
 * được trả ra để tránh response quá lớn và giảm rủi ro lộ dữ liệu nhạy cảm.</p>
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
