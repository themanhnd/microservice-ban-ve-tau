package com.xxxx.payment.integration;

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

@Testcontainers
class PaymentFlywayMigrationIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("payment_migration_it")
            .withUsername("test")
            .withPassword("test");

    @Test
    void flywayMigration_addsPaymentColumnsIndexesAndOutbox() {
        DataSource dataSource = new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createLegacyPaymentTable(jdbcTemplate);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        assertThat(tableExists(jdbcTemplate, "payment_event_outbox")).isTrue();
        assertThat(columnsOf(jdbcTemplate, "payment_transaction"))
                .contains("txn_ref", "idempotency_key", "failure_reason");
        assertThat(indexExists(jdbcTemplate, "payment_transaction", "idx_txn_ref")).isTrue();
        assertThat(indexExists(jdbcTemplate, "payment_transaction", "idx_idempotency_key")).isTrue();
        assertThat(indexExists(jdbcTemplate, "payment_event_outbox", "idx_payment_outbox_status_next")).isTrue();
        assertThat(indexExists(jdbcTemplate, "payment_event_outbox", "idx_payment_outbox_topic_key")).isTrue();
    }

    private void createLegacyPaymentTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE payment_transaction (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    transaction_id VARCHAR(64) NOT NULL,
                    order_id VARCHAR(64) NOT NULL,
                    user_id VARCHAR(64) NOT NULL,
                    amount DECIMAL(15, 2) NOT NULL,
                    payment_method VARCHAR(32) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    gateway_transaction_id VARCHAR(128) NULL,
                    gateway_response_code VARCHAR(32) NULL,
                    payment_url VARCHAR(1024) NULL,
                    created_at DATETIME NULL,
                    updated_at DATETIME NULL,
                    version BIGINT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY idx_transaction_id (transaction_id),
                    KEY idx_order_id (order_id)
                )
                """);
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?", Integer.class, tableName);
        return count != null && count > 0;
    }

    private List<String> columnsOf(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?", String.class, tableName);
    }

    private boolean indexExists(JdbcTemplate jdbcTemplate, String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?", Integer.class, tableName, indexName);
        return count != null && count > 0;
    }
}
