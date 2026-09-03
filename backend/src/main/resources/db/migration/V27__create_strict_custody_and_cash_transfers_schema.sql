-- V27: Schema para Blindagem Patrimonial, Custódia Material e Dinheiro Vivo por CPF
-- Princípio Mandatório: Veículo não tem CPF. Todo bem e valor possui responsabilidade civil vinculada a um CPF.

-- 1. Enriquecimento de Usuários com CPF
ALTER TABLE users ADD COLUMN IF NOT EXISTS cpf VARCHAR(14);
CREATE INDEX IF NOT EXISTS idx_users_cpf ON users(cpf);

-- 2. Enriquecimento de Invoices com rastreabilidade de baixa em espécie
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS settled_in_cash_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(50);
CREATE INDEX IF NOT EXISTS idx_invoices_settled_by_user ON invoices(settled_in_cash_by_user_id);

-- 3. Tabela de Custódia de Dinheiro Vivo por Colaborador (Livro-Caixa Individual)
CREATE TABLE IF NOT EXISTS user_cash_custodies (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    cpf VARCHAR(14),
    current_balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cash_custody_user ON user_cash_custodies(user_id);
CREATE INDEX IF NOT EXISTS idx_cash_custody_cpf ON user_cash_custodies(cpf);

-- 4. Tabela de Histórico de Transferências de Dinheiro entre Colaboradores (Duplo Aceite)
CREATE TABLE IF NOT EXISTS cash_transfer_logs (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    sender_user_id UUID NOT NULL REFERENCES users(id),
    receiver_user_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(15, 2) NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_ACCEPTANCE', -- PENDING_ACCEPTANCE, ACCEPTED, REJECTED, CANCELLED
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMPTZ,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_cash_transfer_sender ON cash_transfer_logs(sender_user_id);
CREATE INDEX IF NOT EXISTS idx_cash_transfer_receiver ON cash_transfer_logs(receiver_user_id);
CREATE INDEX IF NOT EXISTS idx_cash_transfer_status ON cash_transfer_logs(status);

-- 5. Tabela de Confirmações de Depósito Bancário (Segregação de Funções - CFO/Auditor)
CREATE TABLE IF NOT EXISTS bank_deposit_confirmations (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    depositor_user_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(15, 2) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    bank_agency VARCHAR(50),
    bank_account VARCHAR(50),
    receipt_file_url VARCHAR(500) NOT NULL,
    deposit_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_AUDIT', -- PENDING_AUDIT, CONFIRMED_IN_BANK, REJECTED
    audited_by_user_id UUID REFERENCES users(id),
    audited_at TIMESTAMPTZ,
    notes TEXT,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bank_deposit_depositor ON bank_deposit_confirmations(depositor_user_id);
CREATE INDEX IF NOT EXISTS idx_bank_deposit_status ON bank_deposit_confirmations(status);
CREATE INDEX IF NOT EXISTS idx_bank_deposit_auditor ON bank_deposit_confirmations(audited_by_user_id);

-- 6. Tabela de Custódia Material por CPF (ONTs, Drop, Ferramentas)
CREATE TABLE IF NOT EXISTS user_material_custodies (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_name VARCHAR(255) NOT NULL,
    item_type VARCHAR(50) NOT NULL, -- ONT, DROP_CABLE, FAST_CONNECTOR, FUSION_MACHINE, OTDR, TOOL, OTHER
    serial_number VARCHAR(100),
    mac_address VARCHAR(50),
    quantity NUMERIC(10, 2) NOT NULL DEFAULT 1.0,
    unit VARCHAR(20) NOT NULL DEFAULT 'UN',
    allocated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_mat_custody_user ON user_material_custodies(user_id);
CREATE INDEX IF NOT EXISTS idx_mat_custody_type ON user_material_custodies(item_type);
CREATE INDEX IF NOT EXISTS idx_mat_custody_serial ON user_material_custodies(serial_number);

-- 7. Tabela de Transferência de Materiais entre Técnicos (Duplo Aceite de Carga)
CREATE TABLE IF NOT EXISTS material_transfer_logs (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    sender_user_id UUID NOT NULL REFERENCES users(id),
    receiver_user_id UUID NOT NULL REFERENCES users(id),
    material_custody_id UUID NOT NULL REFERENCES user_material_custodies(id) ON DELETE CASCADE,
    quantity NUMERIC(10, 2) NOT NULL DEFAULT 1.0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_ACCEPTANCE', -- PENDING_ACCEPTANCE, ACCEPTED, REJECTED
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMPTZ,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_mat_transfer_sender ON material_transfer_logs(sender_user_id);
CREATE INDEX IF NOT EXISTS idx_mat_transfer_receiver ON material_transfer_logs(receiver_user_id);
CREATE INDEX IF NOT EXISTS idx_mat_transfer_status ON material_transfer_logs(status);
