-- V24: Schema para Telemetria de OLTs, Portas PON e Alarmística de Incidentes/Rompimentos FTTH

-- 1. Portas PON das OLTs
CREATE TABLE IF NOT EXISTS olt_pon_ports (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID,
    network_device_id UUID NOT NULL REFERENCES network_devices(id) ON DELETE CASCADE,
    slot_number INT NOT NULL DEFAULT 0,
    port_number INT NOT NULL DEFAULT 1,
    pon_name VARCHAR(100) NOT NULL, -- Ex: GPON 0/1/1
    admin_status VARCHAR(20) NOT NULL DEFAULT 'UP', -- UP, DOWN
    oper_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DEGRADED, FAULT
    tx_power_dbm NUMERIC(5,2) DEFAULT 4.00,
    temperature_celsius NUMERIC(5,2) DEFAULT 42.50,
    total_onus INT NOT NULL DEFAULT 0,
    online_onus INT NOT NULL DEFAULT 0,
    los_onus INT NOT NULL DEFAULT 0,
    dying_gasp_onus INT NOT NULL DEFAULT 0,
    offline_onus INT NOT NULL DEFAULT 0,
    connected_cable_id UUID REFERENCES ftth_cables(id) ON DELETE SET NULL,
    last_polled_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_olt_slot_port UNIQUE (network_device_id, slot_number, port_number)
);

-- 2. Incidentes e Alarmes de Rompimento de Fibra / Falta de Luz
CREATE TABLE IF NOT EXISTS ftth_incidents (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID,
    network_device_id UUID REFERENCES network_devices(id) ON DELETE SET NULL,
    olt_pon_port_id UUID REFERENCES olt_pon_ports(id) ON DELETE SET NULL,
    incident_type VARCHAR(50) NOT NULL, -- FIBER_CUT_PROBABLE, POWER_OUTAGE_PROBABLE, MASSIVE_LOS_PON, CTO_OFFLINE, DEGRADED_SIGNAL
    severity VARCHAR(20) NOT NULL DEFAULT 'CRITICAL', -- CRITICAL, MAJOR, WARNING, INFO
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INVESTIGATING, DISPATCHED, RESOLVED
    title VARCHAR(255) NOT NULL,
    description TEXT,
    affected_customers_count INT NOT NULL DEFAULT 0,
    affected_ctos_ids JSONB DEFAULT '[]'::jsonb,
    affected_cable_id UUID REFERENCES ftth_cables(id) ON DELETE SET NULL,
    estimated_cut_latitude NUMERIC(10,8),
    estimated_cut_longitude NUMERIC(11,8),
    estimated_cut_details VARCHAR(255),
    work_order_id UUID REFERENCES work_orders(id) ON DELETE SET NULL,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatched_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    root_cause_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Histórico de Telemetria e Níveis de Sinal de ONUs
CREATE TABLE IF NOT EXISTS onu_telemetry_records (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID,
    onu_provisioning_id UUID NOT NULL REFERENCES onu_provisionings(id) ON DELETE CASCADE,
    rx_power_dbm NUMERIC(5,2),
    tx_power_dbm NUMERIC(5,2),
    signal_status VARCHAR(30) NOT NULL DEFAULT 'GOOD', -- GOOD, WARNING, CRITICAL_LOW, CRITICAL_HIGH, LOS, DYING_GASP, OFFLINE
    distance_meters INT DEFAULT 0,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para consultas rápidas
CREATE INDEX IF NOT EXISTS idx_olt_pon_ports_device ON olt_pon_ports(network_device_id);
CREATE INDEX IF NOT EXISTS idx_ftth_incidents_status ON ftth_incidents(status);
CREATE INDEX IF NOT EXISTS idx_ftth_incidents_detected_at ON ftth_incidents(detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_onu_telemetry_onu_id ON onu_telemetry_records(onu_provisioning_id, recorded_at DESC);
