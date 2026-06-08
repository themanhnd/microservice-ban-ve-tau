package com.xxxx.order.service.impl;

import com.xxxx.common.event.OrderPlacedEvent;
import com.xxxx.order.controller.dto.request.PlaceOrderRequest;
import com.xxxx.order.controller.dto.response.OrderResponse;
import com.xxxx.order.event.producer.OrderEventProducer;
import com.xxxx.order.repository.OrderQueueRepository;
import com.xxxx.order.repository.OrderRepository;
import com.xxxx.order.repository.entity.OrderEntity;
import com.xxxx.order.repository.entity.OrderQueueEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderWaitingRoomTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderQueueRepository orderQueueRepository;
    @Mock
    private OrderEventProducer orderEventProducer;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, orderQueueRepository, orderEventProducer, stringRedisTemplate);
        ReflectionTestUtils.setField(service, "maxProcessingOrders", 2);
        ReflectionTestUtils.setField(service, "queueTokenTtlMinutes", 15L);
    }

    @Test
    void placeOrder_enqueuesWaitingToken_withoutPublishingOrderPlacedImmediately() {
        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .userId("user-1")
                .ticketDetailId(42L)
                .quantity(2)
                .totalAmount(BigDecimal.valueOf(100_000))
                .build();

        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId(10L);
            return order;
        });
        when(orderQueueRepository.countByStatus(0)).thenReturn(3L);
        when(orderQueueRepository.save(any(OrderQueueEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = service.placeOrder(request);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("QUEUED");
        assertThat(orderCaptor.getValue().getSagaStatus()).isEqualTo("WAITING_ROOM");

        ArgumentCaptor<OrderQueueEntity> queueCaptor = ArgumentCaptor.forClass(OrderQueueEntity.class);
        verify(orderQueueRepository).save(queueCaptor.capture());
        assertThat(queueCaptor.getValue().getOrderId()).isEqualTo(10L);
        assertThat(queueCaptor.getValue().getStatus()).isEqualTo(0);
        assertThat(queueCaptor.getValue().getPriority()).isEqualTo(0);
        assertThat(queueCaptor.getValue().getToken()).isNotBlank();

        verify(orderEventProducer, never()).publishOrderPlaced(any(OrderPlacedEvent.class));
        assertThat(response.getStatus()).isEqualTo("QUEUED");
        assertThat(response.getQueueStatus()).isEqualTo("WAITING");
        assertThat(response.getQueueToken()).isNotBlank();
        assertThat(response.getQueuePosition()).isEqualTo(4);
    }

    @Test
    void processWaitingRoomBatch_respectsConcurrentLimitAndPublishesInPriorityOrder() {
        OrderEntity vipOrder = OrderEntity.builder()
                .id(1L)
                .orderNo("ORD-VIP")
                .userId("vip")
                .ticketDetailId(42L)
                .quantity(1)
                .totalAmount(BigDecimal.TEN)
                .status("QUEUED")
                .sagaStatus("WAITING_ROOM")
                .correlationId("corr-vip")
                .build();
        OrderQueueEntity vipQueue = OrderQueueEntity.builder()
                .id(100L)
                .orderId(1L)
                .token("token-vip")
                .status(0)
                .priority(-10)
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();
        OrderEntity normalOrder = OrderEntity.builder()
                .id(2L)
                .orderNo("ORD-NORMAL")
                .userId("normal")
                .ticketDetailId(43L)
                .quantity(1)
                .totalAmount(BigDecimal.ONE)
                .status("QUEUED")
                .sagaStatus("WAITING_ROOM")
                .correlationId("corr-normal")
                .build();
        OrderQueueEntity normalQueue = OrderQueueEntity.builder()
                .id(101L)
                .orderId(2L)
                .token("token-normal")
                .status(0)
                .priority(0)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(orderQueueRepository.markExpiredWaitingTokens(any(LocalDateTime.class))).thenReturn(0);
        when(orderQueueRepository.countByStatus(1)).thenReturn(1L);
        when(orderQueueRepository.findWaitingQueue(any(Pageable.class))).thenReturn(List.of(vipQueue));
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(vipOrder));
        when(orderQueueRepository.save(any(OrderQueueEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = service.processWaitingRoomBatch();

        assertThat(processed).isEqualTo(1);
        assertThat(vipQueue.getStatus()).isEqualTo(1);
        assertThat(vipOrder.getStatus()).isEqualTo("PENDING");
        assertThat(vipOrder.getSagaStatus()).isEqualTo("STARTED");
        verify(orderEventProducer).publishOrderPlaced(any(OrderPlacedEvent.class));
        verify(orderRepository, never()).save(eq(normalOrder));
    }
}
