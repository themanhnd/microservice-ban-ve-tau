CREATE TABLE IF NOT EXISTS inventory_event_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    topic VARCHAR(128) NOT NULL,
    event_key VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL,
    next_attempt_at DATETIME NOT NULL,
    published_at DATETIME NULL,
    last_error VARCHAR(1024) NULL,
    version_id BIGINT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    INDEX idx_inventory_outbox_status_next (status, next_attempt_at),
    INDEX idx_inventory_outbox_topic_key (topic, event_key)
);
