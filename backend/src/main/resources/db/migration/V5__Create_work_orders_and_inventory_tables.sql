-- V5: Create work orders, inventory items tables and update sales

-- 1. Update sales table for scheduling preferences
ALTER TABLE sales ADD COLUMN IF NOT EXISTS preferred_period VARCHAR(50) DEFAULT 'MANHA';
ALTER TABLE sales ADD COLUMN IF NOT EXISTS installation_notes TEXT;

-- 2. Create inventory_items table
CREATE TABLE inventory_items (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL, -- ONU_ONT, CABO_DROP, CONECTOR, PTO
    quantity_in_stock INT NOT NULL DEFAULT 0,
    min_quantity INT NOT NULL DEFAULT 10,
    unit VARCHAR(20) NOT NULL DEFAULT 'UN', -- UN, METROS, PACOTE
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial stock items
INSERT INTO inventory_items (id, code, name, category, quantity_in_stock, min_quantity, unit)
VALUES
    (uuidv7(), 'ONT-WIFI6-XPON', 'ONU / ONT XPON Wi-Fi 6 Gigabit', 'ONU_ONT', 150, 20, 'UN'),
    (uuidv7(), 'DROP-OPT-1FO', 'Cabo Drop Óptico Compacto 1FO', 'CABO_DROP', 5000, 500, 'METROS'),
    (uuidv7(), 'CON-SCAPC-FAST', 'Conector Óptico Rápido SC/APC', 'CONECTOR', 800, 100, 'UN'),
    (uuidv7(), 'PTO-ROSETA-OPT', 'Ponto de Terminação Óptica (Roseta)', 'PTO', 300, 50, 'UN')
ON CONFLICT (code) DO NOTHING;

-- 3. Create work_orders table
CREATE TABLE work_orders (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    type VARCHAR(30) NOT NULL DEFAULT 'INSTALACAO', -- INSTALACAO, MANUTENCAO, RETIRADA
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_SCHEDULE', -- PENDING_SCHEDULE, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELED
    scheduled_date DATE,
    scheduled_period VARCHAR(30), -- MANHA, TARDE, NOITE, SABADO_MANHA
    technician_name VARCHAR(150),
    onu_mac VARCHAR(50),
    onu_serial VARCHAR(50),
    fiber_signal_dbm NUMERIC(5, 2),
    notes TEXT,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_inventory_category ON inventory_items(category);
CREATE INDEX idx_work_orders_contract_id ON work_orders(contract_id);
CREATE INDEX idx_work_orders_customer_id ON work_orders(customer_id);
CREATE INDEX idx_work_orders_status ON work_orders(status);
CREATE INDEX idx_work_orders_scheduled_date ON work_orders(scheduled_date);
