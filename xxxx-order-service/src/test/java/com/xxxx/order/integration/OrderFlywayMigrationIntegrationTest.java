package com.xxxx.order.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test chạy Flyway migration của order-service trên MySQL thật.
 *
 * <p>Các integration test nghiệp vụ đang tắt Flyway và để Hibernate tạo schema nhằm chạy nhanh. Test này bù vào khoảng
 * trống đó: nó không boot Spring context, chỉ dựng MySQL bằng Testcontainers, tự tạo schema cũ tối thiểu rồi chạy Flyway
 * để xác minh script migration thật sự chạy được và tạo đúng bảng/cột/index quan trọng.</p>
 */
@Testcontainers
class OrderFlywayMigrationIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("order_migration_it")
            .withUsername("test")
            .withPassword("test");

    @Test
    void flywayMigration_addsSagaOutboxColumnsAndIndexesToExistingSchema() {
        DataSource dataSource = new DriverManagerDataSource(
                mysql.getJdbcUrl(),
                mysql.getUsername(),
                mysql.getPassword()
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createLegacyOrderTable(jdbcTemplate);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        assertThat(tableExists(jdbcTemplate, "order_event_outbox")).isTrue();
        assertThat(columnsOf(jdbcTemplate, "ticker_order"))
                .contains(
                        "idempotency_key",
                        "payment_expires_at",
                        "payment_transaction_id",
                        "payment_url",
                        "failure_reason",
                        "correlation_id",
                        "saga_status",
                        "version_id"
                );
        assertThat(indexExists(jdbcTemplate, "ticker_order", "uk_order_user_idempotency")).isTrue();
        assertThat(indexExists(jdbcTemplate, "order_event_outbox", "idx_order_outbox_status_next")).isTrue();
        assertThat(indexExists(jdbcTemplate, "order_event_outbox", "idx_order_outbox_topic_key")).isTrue();
    }

    /**
     * Tạo schema mô phỏng DB cũ trước khi có các cột saga/idempotency/outbox.
     *
     * <p>Migration dùng điều kiện {@code IF EXISTS ticker_order}, vì vậy test cần có bảng cũ để chứng minh các ALTER TABLE
     * thật sự được thực thi, không chỉ tạo mỗi bảng outbox.</p>
     */
    private void createLegacyOrderTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE ticker_order (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    order_no VARCHAR(64) NOT NULL,
                    user_id VARCHAR(64) NOT NULL,
                    ticket_detail_id BIGINT NOT NULL,
                    quantity INT NOT NULL,
                    total_amount DECIMAL(12, 2) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at DATETIME NULL,
                    updated_at DATETIME NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_order_no (order_no)
                )
                """);
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.tables
                        WHERE table_schema = DATABASE() AND table_name = ?
                        """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private List<String> columnsOf(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.queryForList(
                """
                        SELECT column_name FROM information_schema.columns
                        WHERE table_schema = DATABASE() AND table_name = ?
                        """,
                String.class,
                tableName
        );
    }

    private boolean indexExists(JdbcTemplate jdbcTemplate, String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.statistics
                        WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
                        """,
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }
}
