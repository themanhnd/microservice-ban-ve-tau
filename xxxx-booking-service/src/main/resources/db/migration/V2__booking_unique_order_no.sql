DELIMITER $$
CREATE PROCEDURE migrate_booking_order_no()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'booking'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'booking' AND column_name = 'order_no') THEN
            ALTER TABLE booking ADD COLUMN order_no VARCHAR(255) NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'booking' AND index_name = 'idx_booking_order_no') THEN
            CREATE UNIQUE INDEX idx_booking_order_no ON booking (order_no);
        END IF;
    END IF;
END$$
DELIMITER ;
CALL migrate_booking_order_no();
DROP PROCEDURE migrate_booking_order_no;
