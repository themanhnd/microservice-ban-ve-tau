package com.xxxx.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for all domain events.
 * Provides common fields for event identification, tracing, and versioning.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseEvent {

    /**
     * Unique identifier for this event instance.
     */
    private String eventId;

    /**
     * Loại sự kiện, thường dùng tên class để consumer biết payload đại diện cho nghiệp vụ nào.
     */
    private String eventType;

    /**
     * Thời điểm sự kiện được tạo, giúp debug thứ tự xử lý và độ trễ qua Kafka.
     */
    private LocalDateTime timestamp;

    /**
     * Correlation ID for distributed tracing across services.
     */
    private String correlationId;

    /**
     * Tên service phát sinh sự kiện, dùng để truy vết nguồn dữ liệu trong luồng saga.
     */
    private String sourceService;

    /**
     * Phiên bản schema của sự kiện, mặc định là 1 để hỗ trợ nâng cấp payload về sau.
     */
    private int version;

    public BaseEvent() {
    }

    public BaseEvent(String eventId, String eventType, LocalDateTime timestamp,
                     String correlationId, String sourceService, int version) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
        this.sourceService = sourceService;
        this.version = version;
    }

    /**
     * Initialize common event fields with defaults.
     * Subclasses should call this after construction to set eventId, eventType, timestamp, and version.
     */
    public void initDefaults(String sourceService, String correlationId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.timestamp = LocalDateTime.now();
        this.sourceService = sourceService;
        this.correlationId = correlationId;
        this.version = 1;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEvent baseEvent = (BaseEvent) o;
        return version == baseEvent.version &&
                Objects.equals(eventId, baseEvent.eventId) &&
                Objects.equals(eventType, baseEvent.eventType) &&
                Objects.equals(timestamp, baseEvent.timestamp) &&
                Objects.equals(correlationId, baseEvent.correlationId) &&
                Objects.equals(sourceService, baseEvent.sourceService);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventType, timestamp, correlationId, sourceService, version);
    }

    @Override
    public String toString() {
        return "BaseEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                ", correlationId='" + correlationId + '\'' +
                ", sourceService='" + sourceService + '\'' +
                ", version=" + version +
                '}';
    }
}
