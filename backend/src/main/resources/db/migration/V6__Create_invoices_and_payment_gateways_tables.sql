-- V6: Create invoices, payment gateway configs, and transactions tables

-- 1. Payment Gateway Configs Table
CREATE TABLE payment_gateway_configs (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id),
    gateway_type VARCHAR(50) NOT NULL, -- XINGUBIT_PAY, ASAAS, GERENCIANET, MERCADO_PAGO
    name VARCHAR(150) NOT NULL,
    api_key VARCHAR(255),
    secret_key VARCHAR(255),
    webhook_secret VARCHAR(255),
    pix_key VARCHAR(100),
    sandbox BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed default Xingubit Pay configuration
INSERT INTO payment_gateway_configs (id, gateway_type, name, api_key, secret_key, webhook_secret, pix_key, sandbox, active)
VALUES
    (uuidv7(), 'XINGUBIT_PAY', 'Xingubit Pay (Padrão)', 'xb_live_api_demo_key', 'xb_sec_demo_secret_2026', 'whsec_xb_pay_signature_key', 'pix@xingubit.com.br', TRUE, TRUE)
ON CONFLICT DO NOTHING;

-- 2. Invoices Table
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    gateway_config_id UUID REFERENCES payment_gateway_configs(id),
    gateway_type VARCHAR(50) NOT NULL DEFAULT 'XINGUBIT_PAY',
    external_transaction_id VARCHAR(100),
    amount NUMERIC(10, 2) NOT NULL,
    discount_amount NUMERIC(10, 2) DEFAULT 0.00,
    penalty_amount NUMERIC(10, 2) DEFAULT 0.00,
    interest_amount NUMERIC(10, 2) DEFAULT 0.00,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, OVERDUE, CANCELED
    pix_qrcode_url TEXT,
    pix_copia_e_cola TEXT,
    barcode VARCHAR(100),
    digitable_line VARCHAR(100),
    paid_at TIMESTAMP WITHOUT TIME ZONE,
    paid_amount NUMERIC(10, 2),
    payment_method VARCHAR(30), -- PIX, BOLETO, CARTAO
    pdf_url TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Payment Transactions Log Table
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    invoice_id UUID REFERENCES invoices(id),
    gateway_type VARCHAR(50) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL, -- CHARGE_CREATION, WEBHOOK_NOTIFICATION, STATUS_POLL
    raw_payload JSONB,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_invoices_contract_id ON invoices(contract_id);
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_invoices_external_tx ON invoices(external_transaction_id);
CREATE INDEX idx_gateway_configs_type ON payment_gateway_configs(gateway_type);
CREATE INDEX idx_gateway_configs_active ON payment_gateway_configs(active);
