package com.xxxx.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Event published when an order is confirmed after successful payment.
 * Producer: Order Service
 * Consumer: Booking Service, Notification
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderConfirmedEvent extends BaseEvent {

    private String orderId;
    private String userId;
    private String ticketDetailId;
    private int quantity;
    private BigDecimal totalAmount;
    private String paymentTransactionId;

    public OrderConfirmedEvent() {
    }

    public OrderConfirmedEvent(String orderId, String userId, String ticketDetailId,
                               int quantity, BigDecimal totalAmount, String paymentTransactionId) {
        this.orderId = orderId;
        this.userId = userId;
        this.ticketDetailId = ticketDetailId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.paymentTransactionId = paymentTransactionId;
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

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public void setPaymentTransactionId(String paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OrderConfirmedEvent that = (OrderConfirmedEvent) o;
        return quantity == that.quantity &&
                Objects.equals(orderId, that.orderId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(ticketDetailId, that.ticketDetailId) &&
                Objects.equals(totalAmount, that.totalAmount) &&
                Objects.equals(paymentTransactionId, that.paymentTransactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), orderId, userId, ticketDetailId, quantity, totalAmount, paymentTransactionId);
    }

    @Override
    public String toString() {
        return "OrderConfirmedEvent{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", ticketDetailId='" + ticketDetailId + '\'' +
                ", quantity=" + quantity +
                ", totalAmount=" + totalAmount +
                ", paymentTransactionId='" + paymentTransactionId + '\'' +
                "} " + super.toString();
    }
}
