-- =============================================================================
-- Migration V41: Viabilidade Comercial por CTO e Agenda Técnica por Turno (Onda L)
-- =============================================================================

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS cto_id UUID,
    ADD COLUMN IF NOT EXISTS cto_port_number INTEGER,
    ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 8),
    ADD COLUMN IF NOT EXISTS longitude NUMERIC(11, 8),
    ADD COLUMN IF NOT EXISTS preferred_schedule_date DATE,
    ADD COLUMN IF NOT EXISTS preferred_schedule_period VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_sales_cto_id ON sales (cto_id);
CREATE INDEX IF NOT EXISTS idx_work_orders_schedule_shift ON work_orders (scheduled_date, scheduled_period, technician_name);
