package com.xxxx.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Sự kiện được phát khi đơn hàng bị hủy (do lỗi thanh toán hoặc nguyên nhân khác).
 * Producer: Order Service
 * Consumer: Inventory Service (để hoàn lại tồn kho đã giữ), Booking Service (để hủy booking)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderCancelledEvent extends BaseEvent {

    private String orderId;
    private String userId;
    private String ticketDetailId;
    private Integer quantity;
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

    public String getTicketDetailId() {
        return ticketDetailId;
    }

    public void setTicketDetailId(String ticketDetailId) {
        this.ticketDetailId = ticketDetailId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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
                Objects.equals(ticketDetailId, that.ticketDetailId) &&
                Objects.equals(quantity, that.quantity) &&
                Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), orderId, userId, ticketDetailId, quantity, reason, compensationRequired);
    }

    @Override
    public String toString() {
        return "OrderCancelledEvent{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", ticketDetailId='" + ticketDetailId + '\'' +
                ", quantity=" + quantity +
                ", reason='" + reason + '\'' +
                ", compensationRequired=" + compensationRequired +
                "} " + super.toString();
    }
}
