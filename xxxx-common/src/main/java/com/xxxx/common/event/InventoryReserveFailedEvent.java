package com.xxxx.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Event published when inventory reservation fails due to insufficient stock.
 * Nguồn phát: Inventory Service.
 * Bên nhận: Order Service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryReserveFailedEvent extends BaseEvent {

    private String orderId;
    private String ticketDetailId;
    private int requestedQuantity;
    private int availableStock;
    private String reason;

    public InventoryReserveFailedEvent() {
    }

    public InventoryReserveFailedEvent(String orderId, String ticketDetailId, int requestedQuantity,
                                       int availableStock, String reason) {
        this.orderId = orderId;
        this.ticketDetailId = ticketDetailId;
        this.requestedQuantity = requestedQuantity;
        this.availableStock = availableStock;
        this.reason = reason;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getTicketDetailId() {
        return ticketDetailId;
    }

    public void setTicketDetailId(String ticketDetailId) {
        this.ticketDetailId = ticketDetailId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InventoryReserveFailedEvent that = (InventoryReserveFailedEvent) o;
        return requestedQuantity == that.requestedQuantity &&
                availableStock == that.availableStock &&
                Objects.equals(orderId, that.orderId) &&
                Objects.equals(ticketDetailId, that.ticketDetailId) &&
                Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), orderId, ticketDetailId, requestedQuantity, availableStock, reason);
    }

    @Override
    public String toString() {
        return "InventoryReserveFailedEvent{" +
                "orderId='" + orderId + '\'' +
                ", ticketDetailId='" + ticketDetailId + '\'' +
                ", requestedQuantity=" + requestedQuantity +
                ", availableStock=" + availableStock +
                ", reason='" + reason + '\'' +
                "} " + super.toString();
    }
}
