package com.xxxx.inventory.controller.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO tổng hợp metric outbox của inventory-service.
 *
 * <p>Admin dùng response này để biết outbox có đang kẹt không: có bao nhiêu record theo status/topic, record FAILED cũ
 * nhất đã tồn tại bao lâu và record retry nhiều nhất là bao nhiêu lần.</p>
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
