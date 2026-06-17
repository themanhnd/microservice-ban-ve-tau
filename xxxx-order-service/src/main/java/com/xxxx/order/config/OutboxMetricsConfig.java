package com.xxxx.order.config;

import com.xxxx.order.service.OutboxAdminService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký các gauge quan trọng của outbox để Prometheus/Grafana theo dõi tự động.
 *
 * <p>Gauge đọc dữ liệu trực tiếp từ {@link OutboxAdminService}, nên giá trị luôn bám theo DB hiện tại mỗi lần hệ thống
 * scrape metric. Người mới chỉ cần xem tên metric là hiểu: số record theo trạng thái, tuổi FAILED cũ nhất và retry lớn nhất.</p>
 */
@Configuration
public class OutboxMetricsConfig {

    public OutboxMetricsConfig(MeterRegistry meterRegistry, OutboxAdminService outboxAdminService) {
        Gauge.builder("app.outbox.records", outboxAdminService,
                        service -> service.metrics().getCountByStatus().getOrDefault("PENDING", 0L).doubleValue())
                .description("Số outbox record đang chờ publish")
                .tag("service", "order-service")
                .tag("status", "PENDING")
                .register(meterRegistry);
        Gauge.builder("app.outbox.records", outboxAdminService,
                        service -> service.metrics().getCountByStatus().getOrDefault("RETRY", 0L).doubleValue())
                .description("Số outbox record đang retry")
                .tag("service", "order-service")
                .tag("status", "RETRY")
                .register(meterRegistry);
        Gauge.builder("app.outbox.records", outboxAdminService,
                        service -> service.metrics().getCountByStatus().getOrDefault("FAILED", 0L).doubleValue())
                .description("Số outbox record đã rơi vào DLQ nội bộ")
                .tag("service", "order-service")
                .tag("status", "FAILED")
                .register(meterRegistry);
        Gauge.builder("app.outbox.records", outboxAdminService,
                        service -> service.metrics().getCountByStatus().getOrDefault("IGNORED", 0L).doubleValue())
                .description("Số outbox record đã được admin bỏ qua")
                .tag("service", "order-service")
                .tag("status", "IGNORED")
                .register(meterRegistry);
        Gauge.builder("app.outbox.oldest_failed_age_seconds", outboxAdminService,
                        service -> service.metrics().getOldestFailedAgeSeconds() == null ? 0D : service.metrics().getOldestFailedAgeSeconds().doubleValue())
                .description("Tuổi của outbox FAILED cũ nhất tính theo giây")
                .tag("service", "order-service")
                .register(meterRegistry);
        Gauge.builder("app.outbox.max_attempt_count", outboxAdminService,
                        service -> service.metrics().getMaxAttemptCount() == null ? 0D : service.metrics().getMaxAttemptCount().doubleValue())
                .description("Số lần retry lớn nhất đang có trong outbox")
                .tag("service", "order-service")
                .register(meterRegistry);
    }
}
