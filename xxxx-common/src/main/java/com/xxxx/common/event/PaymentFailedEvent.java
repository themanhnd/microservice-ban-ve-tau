package com.xxxx.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Event published when payment processing fails.
 * Producer: Payment Service
 * Consumer: Order Service
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentFailedEvent extends BaseEvent {

    private String orderId;
    private String transactionId;
    private BigDecimal amount;
    private String reason;
    private String errorCode;

    public PaymentFailedEvent() {
    }

    public PaymentFailedEvent(String orderId, String transactionId, BigDecimal amount,
                              String reason, String errorCode) {
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.reason = reason;
        this.errorCode = errorCode;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PaymentFailedEvent that = (PaymentFailedEvent) o;
        return Objects.equals(orderId, that.orderId) &&
                Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(reason, that.reason) &&
                Objects.equals(errorCode, that.errorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), orderId, transactionId, amount, reason, errorCode);
    }

    @Override
    public String toString() {
        return "PaymentFailedEvent{" +
                "orderId='" + orderId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", amount=" + amount +
                ", reason='" + reason + '\'' +
                ", errorCode='" + errorCode + '\'' +
                "} " + super.toString();
    }
}
