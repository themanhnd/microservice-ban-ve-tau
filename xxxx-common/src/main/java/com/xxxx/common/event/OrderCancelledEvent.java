package com.xxxx.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Event published when an order is cancelled (due to payment failure or other reasons).
 * Producer: Order Service
 * Consumer: Inventory Service (to release reserved stock)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderCancelledEvent extends BaseEvent {

    private String orderId;
    private String userId;
    private String reason;
    private boolean compensationRequired;

    public OrderCancelledEvent() {
    }

    public OrderCancelledEvent(String orderId, String userId, String reason, boolean compensationRequired) {
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
        this.compensationRequired = compensationRequired;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isCompensationRequired() {
        return compensationRequired;
    }

    public void setCompensationRequired(boolean compensationRequired) {
        this.compensationRequired = compensationRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OrderCancelledEvent that = (OrderCancelledEvent) o;
        return compensationRequired == that.compensationRequired &&
                Objects.equals(orderId, that.orderId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), orderId, userId, reason, compensationRequired);
    }

    @Override
    public String toString() {
        return "OrderCancelledEvent{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", reason='" + reason + '\'' +
                ", compensationRequired=" + compensationRequired +
                "} " + super.toString();
    }
}
