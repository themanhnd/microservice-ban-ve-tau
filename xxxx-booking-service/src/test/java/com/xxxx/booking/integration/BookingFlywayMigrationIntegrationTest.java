package com.xxxx.booking.integration;

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
class BookingFlywayMigrationIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("booking_migration_it")
            .withUsername("test")
            .withPassword("test");

    @Test
    void flywayMigration_addsOrderNoAndUniqueIndexToExistingBookingTable() {
        DataSource dataSource = new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createLegacyBookingTable(jdbcTemplate);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        assertThat(columnsOf(jdbcTemplate, "booking")).contains("order_no");
        assertThat(indexExists(jdbcTemplate, "booking", "idx_booking_order_no")).isTrue();
    }

    private void createLegacyBookingTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE booking (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    booking_no VARCHAR(255) NOT NULL,
                    user_id BIGINT NOT NULL,
                    ticket_id BIGINT NOT NULL,
                    ticket_detail_id BIGINT NULL,
                    event_id BIGINT NULL,
                    quantity INT NOT NULL,
                    total_amount DECIMAL(12, 2) NULL,
                    status VARCHAR(20) NOT NULL,
                    notes VARCHAR(500) NULL,
                    created_at DATETIME NULL,
                    updated_at DATETIME NULL,
                    version BIGINT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY idx_booking_no (booking_no),
                    KEY idx_booking_user_id (user_id),
                    KEY idx_booking_ticket_id (ticket_id)
                )
                """);
    }

    private List<String> columnsOf(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?", String.class, tableName);
    }

    private boolean indexExists(JdbcTemplate jdbcTemplate, String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?", Integer.class, tableName, indexName);
        return count != null && count > 0;
    }
}
