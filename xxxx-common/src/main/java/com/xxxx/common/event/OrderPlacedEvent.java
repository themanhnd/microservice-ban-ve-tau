package com.xxxx.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Event published when a new order is placed.
 * Nguồn phát: Order Service.
 * Bên nhận: Inventory Service giữ vé/tồn kho sau khi đơn được tạo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderPlacedEvent extends BaseEvent {

    private String orderId;
    private String userId;
    private String ticketDetailId;
    private int quantity;
    private BigDecimal totalAmount;

    public OrderPlacedEvent() {
    }

    public OrderPlacedEvent(String orderId, String userId, String ticketDetailId,
                            int quantity, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.ticketDetailId = ticketDetailId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTicketDetailId() {
        return ticketDetailId;
    }

    public void setTicketDetailId(String ticketDetailId) {
        this.ticketDetailId = ticketDetailId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OrderPlacedEvent that = (OrderPlacedEvent) o;
        return quantity == that.quantity &&
                Objects.equals(orderId, that.orderId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(ticketDetailId, that.ticketDetailId) &&
                Objects.equals(totalAmount, that.totalAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), orderId, userId, ticketDetailId, quantity, totalAmount);
    }

    @Override
    public String toString() {
        return "OrderPlacedEvent{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", ticketDetailId='" + ticketDetailId + '\'' +
                ", quantity=" + quantity +
                ", totalAmount=" + totalAmount +
                "} " + super.toString();
    }
}
