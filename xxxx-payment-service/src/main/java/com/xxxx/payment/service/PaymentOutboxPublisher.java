package com.xxxx.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.payment.repository.PaymentEventOutboxRepository;
import com.xxxx.payment.repository.entity.PaymentEventOutboxEntity;
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

/** Worker publish outbox event của payment-service sang Kafka, có retry/backoff. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOutboxPublisher {
    private static final String STATUS_PENDING = "PENDING", STATUS_RETRY = "RETRY", STATUS_PUBLISHED = "PUBLISHED", STATUS_FAILED = "FAILED";
    private final PaymentEventOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Value("${payment.outbox.batch-size:50}") private int batchSize;
    @Value("${payment.outbox.max-attempts:10}") private int maxAttempts;
    @Value("${payment.outbox.retry-delay-seconds:30}") private long retryDelaySeconds;

    @Scheduled(fixedDelayString = "${payment.outbox.worker-delay-ms:5000}")
    @Transactional
    public int publishDueEvents() {
        List<PaymentEventOutboxEntity> events = outboxRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(List.of(STATUS_PENDING, STATUS_RETRY), LocalDateTime.now(), PageRequest.of(0, batchSize));
        int published = 0; for (PaymentEventOutboxEntity event : events) { if (publishOne(event)) published++; } return published;
    }

    private boolean publishOne(PaymentEventOutboxEntity outboxEvent) {
        try {
            Object event = objectMapper.readValue(outboxEvent.getPayload(), Class.forName(outboxEvent.getEventType()));
            kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getEventKey(), event).get();
            outboxEvent.setStatus(STATUS_PUBLISHED); outboxEvent.setPublishedAt(LocalDateTime.now()); outboxEvent.setLastError(null); outboxRepository.save(outboxEvent); return true;
        } catch (Exception e) {
            int attempts = outboxEvent.getAttemptCount() == null ? 1 : outboxEvent.getAttemptCount() + 1;
            outboxEvent.setAttemptCount(attempts); outboxEvent.setLastError(truncate(e.getMessage()));
            if (attempts >= maxAttempts) { outboxEvent.setStatus(STATUS_FAILED); } else { outboxEvent.setStatus(STATUS_RETRY); outboxEvent.setNextAttemptAt(LocalDateTime.now().plusSeconds(retryDelaySeconds)); }
            outboxRepository.save(outboxEvent); log.warn("Failed to publish payment outbox event: id={}, attempts={}, status={}, error={}", outboxEvent.getId(), attempts, outboxEvent.getStatus(), e.getMessage()); return false;
        }
    }

    private String truncate(String message) { if (message == null) return null; return message.length() <= 1024 ? message : message.substring(0, 1024); }
}
