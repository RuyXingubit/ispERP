-- V9: Add GPS coordinates to customers and work orders for technician field capture

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 8),
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(11, 8);

ALTER TABLE work_orders
    ADD COLUMN IF NOT EXISTS technician_latitude DECIMAL(10, 8),
    ADD COLUMN IF NOT EXISTS technician_longitude DECIMAL(11, 8),
    ADD COLUMN IF NOT EXISTS gps_captured_at TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_customers_coords ON customers(latitude, longitude);
