package com.xxxx.order.controller.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO tổng hợp sức khỏe outbox để admin đọc nhanh mà không cần truy vấn trực tiếp DB.
 *
 * <p>Các field chính: số record theo từng trạng thái, record lỗi cũ nhất và số lần retry cao nhất. Nếu {@code hasFailed}
 * bằng {@code true}, đội vận hành cần xem danh sách FAILED để replay hoặc resolve.</p>
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
