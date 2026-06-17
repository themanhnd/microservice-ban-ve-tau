CREATE TABLE IF NOT EXISTS payment_event_outbox (
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
    INDEX idx_payment_outbox_status_next (status, next_attempt_at),
    INDEX idx_payment_outbox_topic_key (topic, event_key)
);

DELIMITER $$
CREATE PROCEDURE migrate_payment_txn_columns()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'payment_transaction'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'payment_transaction' AND column_name = 'txn_ref') THEN
            ALTER TABLE payment_transaction ADD COLUMN txn_ref VARCHAR(32) NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'payment_transaction' AND column_name = 'idempotency_key') THEN
            ALTER TABLE payment_transaction ADD COLUMN idempotency_key VARCHAR(128) NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'payment_transaction' AND column_name = 'failure_reason') THEN
            ALTER TABLE payment_transaction ADD COLUMN failure_reason VARCHAR(512) NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'payment_transaction' AND index_name = 'idx_txn_ref') THEN
            CREATE UNIQUE INDEX idx_txn_ref ON payment_transaction (txn_ref);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'payment_transaction' AND index_name = 'idx_idempotency_key') THEN
            CREATE UNIQUE INDEX idx_idempotency_key ON payment_transaction (idempotency_key);
        END IF;
    END IF;
END$$
DELIMITER ;
CALL migrate_payment_txn_columns();
DROP PROCEDURE migrate_payment_txn_columns;
