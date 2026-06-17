package com.xxxx.payment.config;

import com.xxxx.payment.service.OutboxAdminService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

/** Đăng ký gauge outbox của payment-service cho Actuator/Prometheus. */
@Configuration
public class OutboxMetricsConfig {

    public OutboxMetricsConfig(MeterRegistry meterRegistry, OutboxAdminService outboxAdminService) {
        Gauge.builder("app.outbox.records", outboxAdminService,
                        service -> service.metrics().getCountByStatus().getOrDefault("PENDING", 0L).doubleValue())
                .description("Số outbox record đang chờ publish")
                .tag("service", "payment-service")
                .tag("status", "PENDING")
                .register(meterRegistry);
        Gauge.builder("app.outbox.records", outboxAdminService,
                        service -> service.metrics().getCountByStatus().getOrDefault("RETRY", 0L).doubleValue())
                .description("Số outbox record đang retry")
                .tag("service", "payment-service")
                .tag("status", "RETRY")
                .register(meterRegistry);
        Gauge.builder("app.outbox.records", outboxAdminService,
                        service -> service.metrics().getCountByStatus().getOrDefault("FAILED", 0L).doubleValue())
                .description("Số outbox record đã rơi vào DLQ nội bộ")
                .tag("service", "payment-service")
                .tag("status", "FAILED")
                .register(meterRegistry);
        Gauge.builder("app.outbox.records", outboxAdminService,
                        service -> service.metrics().getCountByStatus().getOrDefault("IGNORED", 0L).doubleValue())
                .description("Số outbox record đã được admin bỏ qua")
                .tag("service", "payment-service")
                .tag("status", "IGNORED")
                .register(meterRegistry);
        Gauge.builder("app.outbox.oldest_failed_age_seconds", outboxAdminService,
                        service -> service.metrics().getOldestFailedAgeSeconds() == null ? 0D : service.metrics().getOldestFailedAgeSeconds().doubleValue())
                .description("Tuổi của outbox FAILED cũ nhất tính theo giây")
                .tag("service", "payment-service")
                .register(meterRegistry);
        Gauge.builder("app.outbox.max_attempt_count", outboxAdminService,
                        service -> service.metrics().getMaxAttemptCount() == null ? 0D : service.metrics().getMaxAttemptCount().doubleValue())
                .description("Số lần retry lớn nhất đang có trong outbox")
                .tag("service", "payment-service")
                .register(meterRegistry);
    }
}
