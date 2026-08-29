-- V11: Hierarchical Billing, Smart Payment Rebalance, Trust Unblock Policies, and GeoCEP Routes

-- 1. Regras de Cobrança e Prazos Hierárquicos nos Planos
ALTER TABLE plans ADD COLUMN IF NOT EXISTS suspension_days INTEGER DEFAULT 5;
ALTER TABLE plans ADD COLUMN IF NOT EXISTS always_issue_nfcom BOOLEAN DEFAULT false;

-- 2. Prazo Customizado de Suspensão no Contrato (Ex: Governo 90 dias)
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS custom_suspension_days INTEGER;

-- 3. Flags Fiscais e Governamentais no Cliente
ALTER TABLE customers ADD COLUMN IF NOT EXISTS is_government BOOLEAN DEFAULT false;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS always_issue_nfcom BOOLEAN DEFAULT false;

-- 4. Proteção contra Suspensão, Compensação Cruzada e Avisos Fixos nas Faturas
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS paid_by_cross_credit_id UUID;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS protected_against_suspension BOOLEAN DEFAULT false;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS rebalance_notice TEXT;

-- 5. Histórico e Políticas de Desbloqueio em Confiança (24h)
ALTER TABLE trust_unblocks ADD COLUMN IF NOT EXISTS unblock_type VARCHAR(30) DEFAULT 'BOT_AUTO';
ALTER TABLE trust_unblocks ADD COLUMN IF NOT EXISTS granted_by_user_id UUID;
ALTER TABLE trust_unblocks ADD COLUMN IF NOT EXISTS invoice_id UUID;

-- 6. Rotas Otimizadas de Ordens de Serviço (GeoCEP TSP)
CREATE TABLE IF NOT EXISTS service_routes (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    technician_user_id UUID,
    route_date DATE NOT NULL,
    total_distance_km NUMERIC(8,2),
    estimated_duration_minutes INTEGER,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS service_route_stops (
    id UUID PRIMARY KEY,
    route_id UUID NOT NULL REFERENCES service_routes(id) ON DELETE CASCADE,
    work_order_id UUID NOT NULL REFERENCES work_orders(id),
    sequence_order INTEGER NOT NULL,
    latitude NUMERIC(10,8),
    longitude NUMERIC(11,8),
    customer_name VARCHAR(150),
    address VARCHAR(255),
    completed BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_service_routes_tech_date ON service_routes(technician_user_id, route_date);
CREATE INDEX IF NOT EXISTS idx_service_route_stops_route ON service_route_stops(route_id, sequence_order);
