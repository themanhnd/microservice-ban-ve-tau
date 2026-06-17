package com.xxxx.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Event published when inventory is successfully reserved for an order.
 * Nguồn phát: Inventory Service.
 * Bên nhận: Order Service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryReservedEvent extends BaseEvent {

    private String orderId;
    private String ticketDetailId;
    private int quantity;
    private int remainingStock;

    public InventoryReservedEvent() {
    }

    public InventoryReservedEvent(String orderId, String ticketDetailId, int quantity, int remainingStock) {
        this.orderId = orderId;
        this.ticketDetailId = ticketDetailId;
        this.quantity = quantity;
        this.remainingStock = remainingStock;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getRemainingStock() {
        return remainingStock;
    }

    public void setRemainingStock(int remainingStock) {
        this.remainingStock = remainingStock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InventoryReservedEvent that = (InventoryReservedEvent) o;
        return quantity == that.quantity &&
                remainingStock == that.remainingStock &&
                Objects.equals(orderId, that.orderId) &&
                Objects.equals(ticketDetailId, that.ticketDetailId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), orderId, ticketDetailId, quantity, remainingStock);
    }

    @Override
    public String toString() {
        return "InventoryReservedEvent{" +
                "orderId='" + orderId + '\'' +
                ", ticketDetailId='" + ticketDetailId + '\'' +
                ", quantity=" + quantity +
                ", remainingStock=" + remainingStock +
                "} " + super.toString();
    }
}
