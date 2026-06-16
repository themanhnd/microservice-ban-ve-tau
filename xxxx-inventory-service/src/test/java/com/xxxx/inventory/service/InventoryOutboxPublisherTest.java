package com.xxxx.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxx.common.constant.KafkaTopics;
import com.xxxx.common.event.InventoryReservedEvent;
import com.xxxx.inventory.repository.InventoryEventOutboxRepository;
import com.xxxx.inventory.repository.entity.InventoryEventOutboxEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryOutboxPublisherTest {
    @Mock private InventoryEventOutboxRepository outboxRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    private InventoryOutboxPublisher publisher; private ObjectMapper objectMapper;
    @BeforeEach void setUp() { objectMapper = new ObjectMapper(); publisher = new InventoryOutboxPublisher(outboxRepository, kafkaTemplate, objectMapper); ReflectionTestUtils.setField(publisher, "batchSize", 50); ReflectionTestUtils.setField(publisher, "maxAttempts", 3); ReflectionTestUtils.setField(publisher, "retryDelaySeconds", 30L); }
    @Test void publishDueEvents_marksEventPublishedWhenKafkaSendSucceeds() throws Exception {
        InventoryReservedEvent event = new InventoryReservedEvent(); event.setOrderId("ORD-1"); event.setTicketDetailId("42"); event.setQuantity(1);
        InventoryEventOutboxEntity outbox = InventoryEventOutboxEntity.builder().id(1L).topic(KafkaTopics.INVENTORY_RESERVED).eventKey("ORD-1").eventType(InventoryReservedEvent.class.getName()).payload(objectMapper.writeValueAsString(event)).status("PENDING").attemptCount(0).nextAttemptAt(LocalDateTime.now().minusSeconds(1)).build();
        when(outboxRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(), any(), any(Pageable.class))).thenReturn(List.of(outbox));
        when(kafkaTemplate.send(eq(KafkaTopics.INVENTORY_RESERVED), eq("ORD-1"), any())).thenReturn(CompletableFuture.completedFuture(null));
        assertThat(publisher.publishDueEvents()).isEqualTo(1); assertThat(outbox.getStatus()).isEqualTo("PUBLISHED"); verify(outboxRepository).save(outbox);
    }
}
