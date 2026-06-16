package com.xxxx.inventory.service.impl;

import com.xxxx.common.event.OrderCancelledEvent;
import com.xxxx.inventory.controller.dto.request.ReleaseStockRequest;
import com.xxxx.inventory.event.consumer.OrderCancelledEventConsumer;
import com.xxxx.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryCompensationConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderCancelledEventConsumer consumer;

    @Test
    void handleOrderCancelled_releasesStockWhenCompensationRequired() {
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId("ORD-1");
        event.setTicketDetailId("42");
        event.setQuantity(3);
        event.setCompensationRequired(true);

        consumer.handleOrderCancelled(event);

        ArgumentCaptor<ReleaseStockRequest> captor = ArgumentCaptor.forClass(ReleaseStockRequest.class);
        verify(inventoryService).releaseStock(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo("ORD-1");
        assertThat(captor.getValue().getTicketDetailId()).isEqualTo(42L);
        assertThat(captor.getValue().getQuantity()).isEqualTo(3);
    }

    @Test
    void handleOrderCancelled_skipsWhenCompensationNotRequired() {
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId("ORD-2");
        event.setTicketDetailId("42");
        event.setQuantity(1);
        event.setCompensationRequired(false);

        consumer.handleOrderCancelled(event);

        verify(inventoryService, never()).releaseStock(org.mockito.ArgumentMatchers.any());
    }
}
