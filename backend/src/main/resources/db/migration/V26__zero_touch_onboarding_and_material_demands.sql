-- V26: Schema para Demanda de Materiais FTTH, Despacho Inteligente e Orquestrador Zero-Touch Onboarding

-- 1. Tabela de Demanda e Alocação de Materiais por Ordem de Serviço
CREATE TABLE IF NOT EXISTS installation_material_demands (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    cto_id UUID REFERENCES ftth_ctos(id) ON DELETE SET NULL,
    cto_port_number INT,
    estimated_drop_meters INT NOT NULL DEFAULT 50,
    onu_model_required VARCHAR(100) NOT NULL DEFAULT 'ONT Wi-Fi Dual-Band GPON',
    fast_connectors_count INT NOT NULL DEFAULT 2,
    pto_rosette_count INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_ALLOCATION', -- PENDING_ALLOCATION, ALLOCATED_VEHICLE, ALLOCATED_CENTRAL, CONSUMED
    allocated_warehouse_id UUID REFERENCES warehouses(id) ON DELETE SET NULL,
    allocated_technician_name VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices de busca rápida
CREATE INDEX IF NOT EXISTS idx_inst_demands_wo ON installation_material_demands(work_order_id);
CREATE INDEX IF NOT EXISTS idx_inst_demands_contract ON installation_material_demands(contract_id);
CREATE INDEX IF NOT EXISTS idx_inst_demands_cto ON installation_material_demands(cto_id);

-- 2. Enriquecimento de WorkOrders com suporte ao ciclo de ativação
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS onu_rx_power_dbm NUMERIC(5,2);
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS radius_authenticated BOOLEAN DEFAULT false;
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS allocated_warehouse_id UUID REFERENCES warehouses(id) ON DELETE SET NULL;
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS cto_id UUID REFERENCES ftth_ctos(id) ON DELETE SET NULL;
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS cto_port_number INT;

-- 3. Enriquecimento de Contratos com CTO e Porta de Ativação
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS cto_id UUID REFERENCES ftth_ctos(id) ON DELETE SET NULL;
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS cto_port_number INT;
