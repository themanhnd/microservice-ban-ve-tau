package com.xxxx.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.order.repository.OrderEventOutboxRepository;
import com.xxxx.order.repository.entity.OrderEventOutboxEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Worker publish event từ bảng outbox của Order Service ra Kafka.
 *
 * <p>Luồng chuẩn là: service nghiệp vụ ghi event vào DB trong cùng transaction với order, sau đó worker này đọc các
 * record PENDING/RETRY và publish Kafka. Nhờ vậy tránh mất event khi DB commit xong nhưng Kafka tạm lỗi.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxPublisher {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RETRY = "RETRY";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_FAILED = "FAILED";

    private final OrderEventOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${order.outbox.batch-size:50}")
    private int batchSize;

    @Value("${order.outbox.max-attempts:10}")
    private int maxAttempts;

    @Value("${order.outbox.retry-delay-seconds:30}")
    private long retryDelaySeconds;

    /**
     * Quét các event đến hạn publish và gửi Kafka theo batch nhỏ để tránh giữ transaction quá lâu.
     */
    @Scheduled(fixedDelayString = "${order.outbox.worker-delay-ms:5000}")
    @Transactional
    public int publishDueEvents() {
        List<OrderEventOutboxEntity> events = outboxRepository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(STATUS_PENDING, STATUS_RETRY),
                        LocalDateTime.now(),
                        PageRequest.of(0, batchSize)
                );
        int published = 0;
        for (OrderEventOutboxEntity event : events) {
            if (publishOne(event)) {
                published++;
            }
        }
        return published;
    }

    /**
     * Publish một event; thành công thì mark PUBLISHED, thất bại thì lên lịch retry hoặc FAILED.
     */
    private boolean publishOne(OrderEventOutboxEntity outboxEvent) {
        try {
            Object event = objectMapper.readValue(
                    outboxEvent.getPayload(),
                    Class.forName(outboxEvent.getEventType())
            );
            kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getEventKey(), event).get();
            outboxEvent.setStatus(STATUS_PUBLISHED);
            outboxEvent.setPublishedAt(LocalDateTime.now());
            outboxEvent.setLastError(null);
            outboxRepository.save(outboxEvent);
            log.info("Published outbox event: id={}, topic={}, key={}",
                    outboxEvent.getId(), outboxEvent.getTopic(), outboxEvent.getEventKey());
            return true;
        } catch (Exception e) {
            int attempts = outboxEvent.getAttemptCount() == null ? 1 : outboxEvent.getAttemptCount() + 1;
            outboxEvent.setAttemptCount(attempts);
            outboxEvent.setLastError(truncate(e.getMessage()));
            if (attempts >= maxAttempts) {
                outboxEvent.setStatus(STATUS_FAILED);
            } else {
                outboxEvent.setStatus(STATUS_RETRY);
                outboxEvent.setNextAttemptAt(LocalDateTime.now().plusSeconds(retryDelaySeconds));
            }
            outboxRepository.save(outboxEvent);
            log.warn("Failed to publish outbox event: id={}, attempts={}, status={}, error={}",
                    outboxEvent.getId(), attempts, outboxEvent.getStatus(), e.getMessage());
            return false;
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1024 ? message : message.substring(0, 1024);
    }
}
