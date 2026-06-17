package com.xxxx.payment.service;

import com.xxxx.payment.controller.dto.response.OutboxMetricsResponse;
import com.xxxx.payment.controller.dto.response.OutboxRecordResponse;
import com.xxxx.payment.repository.PaymentEventOutboxRepository;
import com.xxxx.payment.repository.entity.PaymentEventOutboxEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service admin để xem, replay hoặc ignore DLQ nội bộ của payment-service.
 *
 * <p>Replay đưa record về {@code PENDING} để worker gửi Kafka lại. Ignore chuyển record sang {@code IGNORED} và lưu lý
 * do trong {@code lastError}, tránh người vận hành sau này không biết vì sao record không được gửi lại.</p>
 */
@Service
@RequiredArgsConstructor
public class OutboxAdminService {

    private static final String SERVICE_NAME = "payment-service";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RETRY = "RETRY";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_IGNORED = "IGNORED";

    private final PaymentEventOutboxRepository outboxRepository;

    @Transactional(readOnly = true)
    public List<OutboxRecordResponse> listFailed(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return outboxRepository.findByStatusOrderByCreatedAtDesc(STATUS_FAILED, pageable)
                .stream()
                .map(this::toRecordResponse)
                .toList();
    }

    @Transactional
    public OutboxRecordResponse replay(Long id) {
        PaymentEventOutboxEntity event = getEvent(id);
        event.setStatus(STATUS_PENDING);
        event.setNextAttemptAt(LocalDateTime.now());
        event.setLastError(null);
        return toRecordResponse(outboxRepository.save(event));
    }

    @Transactional
    public OutboxRecordResponse ignore(Long id, String reason) {
        PaymentEventOutboxEntity event = getEvent(id);
        event.setStatus(STATUS_IGNORED);
        event.setLastError(truncate("Ignored by admin: " + reason));
        return toRecordResponse(outboxRepository.save(event));
    }

    @Transactional(readOnly = true)
    public OutboxMetricsResponse metrics() {
        Map<String, Long> countByStatus = new LinkedHashMap<>();
        countByStatus.put(STATUS_PENDING, outboxRepository.countByStatus(STATUS_PENDING));
        countByStatus.put(STATUS_RETRY, outboxRepository.countByStatus(STATUS_RETRY));
        countByStatus.put(STATUS_FAILED, outboxRepository.countByStatus(STATUS_FAILED));
        countByStatus.put(STATUS_IGNORED, outboxRepository.countByStatus(STATUS_IGNORED));
        countByStatus.put(STATUS_PUBLISHED, outboxRepository.countByStatus(STATUS_PUBLISHED));
        Map<String, Map<String, Long>> countByTopicStatus = buildTopicStatusMetrics();
        LocalDateTime oldestFailed = outboxRepository.findFirstByStatusOrderByCreatedAtAsc(STATUS_FAILED)
                .map(PaymentEventOutboxEntity::getCreatedAt)
                .orElse(null);
        Integer maxAttemptCount = outboxRepository.findTopByOrderByAttemptCountDesc()
                .map(PaymentEventOutboxEntity::getAttemptCount)
                .orElse(0);
        return OutboxMetricsResponse.builder()
                .serviceName(SERVICE_NAME)
                .countByStatus(countByStatus)
                .countByTopicStatus(countByTopicStatus)
                .oldestFailedCreatedAt(oldestFailed)
                .oldestFailedAgeSeconds(oldestFailed == null ? null
                        : Duration.between(oldestFailed, LocalDateTime.now()).toSeconds())
                .maxAttemptCount(maxAttemptCount)
                .hasFailed(countByStatus.get(STATUS_FAILED) > 0)
                .build();
    }

    private Map<String, Map<String, Long>> buildTopicStatusMetrics() {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        for (Object[] row : outboxRepository.countGroupByTopicAndStatus()) {
            String topic = String.valueOf(row[0]);
            String status = String.valueOf(row[1]);
            Long count = ((Number) row[2]).longValue();
            result.computeIfAbsent(topic, ignored -> new LinkedHashMap<>()).put(status, count);
        }
        return result;
    }

    private PaymentEventOutboxEntity getEvent(Long id) {
        return outboxRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy outbox record id=" + id));
    }

    private OutboxRecordResponse toRecordResponse(PaymentEventOutboxEntity event) {
        return OutboxRecordResponse.builder()
                .id(event.getId())
                .serviceName(SERVICE_NAME)
                .topic(event.getTopic())
                .eventKey(event.getEventKey())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .attemptCount(event.getAttemptCount())
                .nextAttemptAt(event.getNextAttemptAt())
                .publishedAt(event.getPublishedAt())
                .lastError(event.getLastError())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1024 ? message : message.substring(0, 1024);
    }
}
