package com.xxxx.payment.controller.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO tổng hợp trạng thái outbox payment-service để admin/monitoring đọc nhanh.
 *
 * <p>{@code countByStatus} cho biết tổng số record theo trạng thái; {@code countByTopicStatus} cho biết từng topic đang
 * kẹt ở trạng thái nào. Các trường tuổi FAILED cũ nhất và max retry giúp phát hiện event publish bị kẹt lâu.</p>
 */
@Data
@Builder
public class OutboxMetricsResponse {
    private String serviceName;
    private Map<String, Long> countByStatus;
    private Map<String, Map<String, Long>> countByTopicStatus;
    private LocalDateTime oldestFailedCreatedAt;
    private Long oldestFailedAgeSeconds;
    private Integer maxAttemptCount;
    private boolean hasFailed;
}
