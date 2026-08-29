-- V10: Create multi-warehouse, serialized asset custody, inter-warehouse transfers, and tool promissory agreements

-- 1. Warehouses Table
CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL DEFAULT 'PA',
    address VARCHAR(255),
    responsible_user_id UUID REFERENCES users(id),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial warehouses
INSERT INTO warehouses (id, code, name, city, state, address)
VALUES
    (uuidv7(), 'DEP-ATM-CENTRAL', 'Depósito Central Altamira', 'Altamira', 'PA', 'Av. Tancredo Neves, 1200'),
    (uuidv7(), 'ESC-VTX-APOIO', 'Escritório & Ponto de Apoio Vitória do Xingu', 'Vitória do Xingu', 'PA', 'Rua Principal, 450')
ON CONFLICT (code) DO NOTHING;

-- 2. Warehouse Stock for Bulk Consumables (Cabos, Conectores, PTOs)
CREATE TABLE warehouse_stock (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES inventory_items(id) ON DELETE CASCADE,
    quantity_in_stock INT NOT NULL DEFAULT 0,
    min_quantity INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_warehouse_item UNIQUE (warehouse_id, item_id)
);

-- 3. Serialized Assets Table (ONTs, Roteadores, Máquinas de Fusão, OTDR)
CREATE TABLE serialized_assets (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    mac_address VARCHAR(50),
    serial_number VARCHAR(100) NOT NULL UNIQUE,
    brand_model VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL, -- ONU_ONT, ROUTER_MESH, TOOL_FUSION_MACHINE, TOOL_OTDR, SWITCH, OLT
    replacement_value NUMERIC(12, 2) NOT NULL DEFAULT 0.00, -- Valor venal / Nota Promissória
    current_warehouse_id UUID REFERENCES warehouses(id),
    current_holder_user_id UUID REFERENCES users(id), -- CPF/Pessoa Física com a custódia
    current_customer_id UUID REFERENCES customers(id), -- Em comodato com cliente
    current_contract_id UUID REFERENCES contracts(id),
    status VARCHAR(50) NOT NULL DEFAULT 'DISPONIVEL_DEPOSITO', -- DISPONIVEL_DEPOSITO, CUSTODIA_COLABORADOR, EM_TRANSITO, INSTALADO_CLIENTE, RETIRADO_PENDENTE_DEVOLUCAO, DEFEITO_TRIAGEM
    last_movement_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_serialized_assets_mac ON serialized_assets(mac_address);
CREATE INDEX idx_serialized_assets_serial ON serialized_assets(serial_number);
CREATE INDEX idx_serialized_assets_holder ON serialized_assets(current_holder_user_id);
CREATE INDEX idx_serialized_assets_warehouse ON serialized_assets(current_warehouse_id);
CREATE INDEX idx_serialized_assets_status ON serialized_assets(status);

-- 4. Stock Transfers (Transferências Intermunicipais com Duplo Aceite / Handshake)
CREATE TABLE stock_transfers (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    code VARCHAR(50) NOT NULL UNIQUE,
    origin_warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    destination_warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    carrier_user_id UUID REFERENCES users(id), -- Colaborador / Portador responsável
    carrier_name VARCHAR(150) NOT NULL,
    carrier_document VARCHAR(20) NOT NULL, -- CPF ou CNPJ
    carrier_type VARCHAR(30) NOT NULL DEFAULT 'COLABORADOR', -- COLABORADOR, TERCEIRO, TRANSPORTADORA
    dispatched_by_user_id UUID REFERENCES users(id),
    received_by_user_id UUID REFERENCES users(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_TRANSIT, RECEIVED, CANCELED
    dispatch_photo_url TEXT,
    receipt_photo_url TEXT,
    notes TEXT,
    dispatched_at TIMESTAMP WITHOUT TIME ZONE,
    received_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stock_transfer_items (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    transfer_id UUID NOT NULL REFERENCES stock_transfers(id) ON DELETE CASCADE,
    asset_id UUID REFERENCES serialized_assets(id),
    item_id UUID REFERENCES inventory_items(id),
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Tool Custody Agreements (Termo de Cautela com Força de Nota Promissória)
CREATE TABLE tool_custody_agreements (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    code VARCHAR(50) NOT NULL UNIQUE,
    work_order_id UUID REFERENCES work_orders(id),
    holder_user_id UUID REFERENCES users(id),
    holder_name VARCHAR(150) NOT NULL,
    holder_cpf VARCHAR(20) NOT NULL,
    is_third_party BOOLEAN NOT NULL DEFAULT false,
    total_promissory_value NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, RETURNED_OK, RETURNED_DAMAGED, EXECUTED_JUDICIALLY
    agreement_text TEXT NOT NULL,
    dispatch_photo_url TEXT,
    return_photo_url TEXT,
    notes TEXT,
    signed_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    returned_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Custody Logs (Kardex e Auditoria Imutável de Movimentações)
CREATE TABLE custody_logs (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    asset_id UUID REFERENCES serialized_assets(id),
    item_id UUID REFERENCES inventory_items(id),
    from_user_id UUID REFERENCES users(id),
    to_user_id UUID REFERENCES users(id),
    from_warehouse_id UUID REFERENCES warehouses(id),
    to_warehouse_id UUID REFERENCES warehouses(id),
    work_order_id UUID REFERENCES work_orders(id),
    event_type VARCHAR(50) NOT NULL, -- TRANSFER_DISPATCH, TRANSFER_RECEIPT, CHECKOUT_TO_TECH, RETURN_FROM_TECH, INSTALLED_AT_CLIENT, RECOVERED_FROM_CLIENT
    photo_url TEXT,
    notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. Add Installation Photo to Work Orders
ALTER TABLE work_orders
    ADD COLUMN IF NOT EXISTS installation_photo_url TEXT,
    ADD COLUMN IF NOT EXISTS tool_agreement_id UUID REFERENCES tool_custody_agreements(id);
