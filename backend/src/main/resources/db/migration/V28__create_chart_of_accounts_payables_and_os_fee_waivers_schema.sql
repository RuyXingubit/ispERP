-- V28: Schema para Plano de Contas Dinâmico de 5 Níveis Telecom, Contas a Pagar e Esteira de Isenção de Taxas de O.S.

-- 1. Tabela de Plano de Contas (Árvore Hierárquica Auto-Referenciada)
CREATE TABLE IF NOT EXISTS chart_of_accounts (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    parent_id UUID REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(30) NOT NULL, -- REVENUE, TAX, DIRECT_COST, OPEX, CAPEX
    dre_category VARCHAR(50) NOT NULL,
    is_synthetic BOOLEAN NOT NULL DEFAULT FALSE,
    is_analytical BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chart_accounts_parent ON chart_of_accounts(parent_id);
CREATE INDEX IF NOT EXISTS idx_chart_accounts_code ON chart_of_accounts(code);
CREATE INDEX IF NOT EXISTS idx_chart_accounts_type ON chart_of_accounts(account_type);

-- 2. Tabela de Contas a Pagar (Payable Invoices)
CREATE TABLE IF NOT EXISTS payable_invoices (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    supplier_name VARCHAR(255) NOT NULL,
    supplier_document VARCHAR(20),
    chart_of_account_id UUID NOT NULL REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
    description VARCHAR(500) NOT NULL,
    invoice_number VARCHAR(100),
    total_amount NUMERIC(15, 2) NOT NULL,
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PARTIALLY_PAID, PAID, CANCELLED
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payables_supplier ON payable_invoices(supplier_name);
CREATE INDEX IF NOT EXISTS idx_payables_account ON payable_invoices(chart_of_account_id);
CREATE INDEX IF NOT EXISTS idx_payables_status ON payable_invoices(status);

-- 3. Tabela de Parcelas de Despesas (Expense Installments - Esteira de Desalavancagem)
CREATE TABLE IF NOT EXISTS expense_installments (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    payable_invoice_id UUID NOT NULL REFERENCES payable_invoices(id) ON DELETE CASCADE,
    installment_number INT NOT NULL DEFAULT 1,
    total_installments INT NOT NULL DEFAULT 1,
    due_date DATE NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    interest_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, OVERDUE, CANCELLED
    paid_at TIMESTAMPTZ,
    paid_amount NUMERIC(15, 2),
    payment_method VARCHAR(50),
    receipt_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_installments_payable ON expense_installments(payable_invoice_id);
CREATE INDEX IF NOT EXISTS idx_installments_due ON expense_installments(due_date);
CREATE INDEX IF NOT EXISTS idx_installments_status ON expense_installments(status);

-- 4. Enriquecimento de WorkOrders com Suporte a Tarifas e Esteira de Isenção
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS standard_fee_amount NUMERIC(10, 2) DEFAULT 0.00;
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS fee_status VARCHAR(50) DEFAULT 'NOT_APPLICABLE'; -- NOT_APPLICABLE, BILLABLE, PENDING_WAIVER_APPROVAL, WAIVED_APPROVED, WAIVED_REJECTED
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS waiver_reason TEXT;
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS waiver_requested_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS waiver_audited_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS waiver_audited_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_wo_fee_status ON work_orders(fee_status);

-- 5. Seeder Oficial: Plano de Contas Canônico de Telecomunicações (5 Grupos Canônicos)

-- GRUPO 01: RECEITAS
INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES ('01a00000-0000-7000-0000-000000000001', NULL, '01', '01. RECEITAS OPERACIONAIS BRUTAS', 'REVENUE', 'GROSS_REVENUE', TRUE, FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES ('01a00000-0000-7000-0000-000000000002', '01a00000-0000-7000-0000-000000000001', '01.01', '01.01. Receita de Telecomunicações (SCM)', 'REVENUE', 'GROSS_REVENUE', TRUE, FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES 
    (uuidv7(), '01a00000-0000-7000-0000-000000000002', '01.01.01', 'Mensalidades Internet Fibra Óptica (Varejo)', 'REVENUE', 'GROSS_REVENUE', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000002', '01.01.02', 'Mensalidades Links Dedicados / PME', 'REVENUE', 'GROSS_REVENUE', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000002', '01.01.03', 'Serviços de Valor Adicionado (SVA / Streaming)', 'REVENUE', 'GROSS_REVENUE', FALSE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES ('01a00000-0000-7000-0000-000000000003', '01a00000-0000-7000-0000-000000000001', '01.02', '01.02. Taxas de Instalação e Serviços Técnicos', 'REVENUE', 'GROSS_REVENUE', TRUE, FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES 
    (uuidv7(), '01a00000-0000-7000-0000-000000000003', '01.02.01', 'Taxa de Instalação / Ativação', 'REVENUE', 'GROSS_REVENUE', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000003', '01.02.02', 'Taxa de Mudança de Endereço', 'REVENUE', 'GROSS_REVENUE', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000003', '01.02.03', 'Taxa de Ponto Adicional / Troca de Cômodo', 'REVENUE', 'GROSS_REVENUE', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000003', '01.02.04', 'Taxa de Visita Técnica Improdutiva', 'REVENUE', 'GROSS_REVENUE', FALSE, TRUE)
ON CONFLICT (code) DO NOTHING;

-- GRUPO 02: DEDUÇÕES E IMPOSTOS
INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES ('01a00000-0000-7000-0000-000000000004', NULL, '02', '02. IMPOSTOS & DEDUÇÕES SOBRE RECEITA', 'TAX', 'TAX_DEDUCTION', TRUE, FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES 
    (uuidv7(), '01a00000-0000-7000-0000-000000000004', '02.01.01', 'Simples Nacional (DAS Telecom)', 'TAX', 'TAX_DEDUCTION', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000004', '02.01.02', 'ICMS Comunicação (NFCom / Convênio 115)', 'TAX', 'TAX_DEDUCTION', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000004', '02.01.03', 'PIS / COFINS Telecom', 'TAX', 'TAX_DEDUCTION', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000004', '02.01.04', 'Fundos Setoriais (FUST / FUNTTEL)', 'TAX', 'TAX_DEDUCTION', FALSE, TRUE)
ON CONFLICT (code) DO NOTHING;

-- GRUPO 03: INTERCONEXÃO & TRANSPORTE
INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES ('01a00000-0000-7000-0000-000000000005', NULL, '03', '03. CUSTOS DIRETOS / INTERCONEXÃO IP', 'DIRECT_COST', 'DIRECT_COST_INTERCONNECTION', TRUE, FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES 
    (uuidv7(), '01a00000-0000-7000-0000-000000000005', '03.01.01', 'Link Trânsito IP Primário', 'DIRECT_COST', 'DIRECT_COST_INTERCONNECTION', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000005', '03.01.02', 'Link Trânsito IP Secundário / Redundância', 'DIRECT_COST', 'DIRECT_COST_INTERCONNECTION', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000005', '03.01.03', 'Transporte de Dados / PTT / IX.br', 'DIRECT_COST', 'DIRECT_COST_INTERCONNECTION', FALSE, TRUE)
ON CONFLICT (code) DO NOTHING;

-- GRUPO 04: CUSTOS FIXOS & OPEX
INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES ('01a00000-0000-7000-0000-000000000006', NULL, '04', '04. DESPESAS OPERACIONAIS & OPEX', 'OPEX', 'OPEX_ADMIN', TRUE, FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES 
    (uuidv7(), '01a00000-0000-7000-0000-000000000006', '04.01.01', 'Compartilhamento de Postes (Concessionária)', 'OPEX', 'OPEX_POLES', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000006', '04.01.02', 'Folha de Pagamento / Salários Operacionais', 'OPEX', 'OPEX_HR', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000006', '04.01.03', 'Combustível e Manutenção de Veículos', 'OPEX', 'OPEX_FLEET', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000006', '04.01.04', 'Marketing e Panfletagem de Vendas', 'OPEX', 'OPEX_MARKETING', FALSE, TRUE)
ON CONFLICT (code) DO NOTHING;

-- GRUPO 05: INVESTIMENTOS & CAPEX
INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES ('01a00000-0000-7000-0000-000000000007', NULL, '05', '05. INVESTIMENTOS & CAPEX (EXPANSÃO)', 'CAPEX', 'CAPEX_NETWORK', TRUE, FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO chart_of_accounts (id, parent_id, code, name, account_type, dre_category, is_synthetic, is_analytical)
VALUES 
    (uuidv7(), '01a00000-0000-7000-0000-000000000007', '05.01.01', 'Cabos Ópticos e Bobinas de Drop', 'CAPEX', 'CAPEX_NETWORK', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000007', '05.01.02', 'Caixas CTO e Caixas de Emenda', 'CAPEX', 'CAPEX_NETWORK', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000007', '05.01.03', 'Equipamentos de Clientes (ONTs Wi-Fi)', 'CAPEX', 'CAPEX_EQUIPMENT', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000007', '05.01.04', 'Máquinas de Fusão e OTDRs', 'CAPEX', 'CAPEX_EQUIPMENT', FALSE, TRUE),
    (uuidv7(), '01a00000-0000-7000-0000-000000000007', '05.01.05', 'Aquisição de Veículos da Frota', 'CAPEX', 'CAPEX_FLEET', FALSE, TRUE)
ON CONFLICT (code) DO NOTHING;
