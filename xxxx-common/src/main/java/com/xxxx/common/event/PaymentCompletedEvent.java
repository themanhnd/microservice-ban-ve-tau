package com.xxxx.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Event published when payment is successfully completed.
 * Nguồn phát: Payment Service.
 * Bên nhận: Order Service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentCompletedEvent extends BaseEvent {

    private String orderId;
    private String transactionId;
    private BigDecimal amount;
    private String paymentMethod;
    private String gatewayTransactionId;

    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(String orderId, String transactionId, BigDecimal amount,
                                 String paymentMethod, String gatewayTransactionId) {
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PaymentCompletedEvent that = (PaymentCompletedEvent) o;
        return Objects.equals(orderId, that.orderId) &&
                Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(paymentMethod, that.paymentMethod) &&
                Objects.equals(gatewayTransactionId, that.gatewayTransactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), orderId, transactionId, amount, paymentMethod, gatewayTransactionId);
    }

    @Override
    public String toString() {
        return "PaymentCompletedEvent{" +
                "orderId='" + orderId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", amount=" + amount +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", gatewayTransactionId='" + gatewayTransactionId + '\'' +
                "} " + super.toString();
    }
}
