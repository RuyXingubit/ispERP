-- V8: Create notification configs, logs, trust unblocks, and plan upgrade requests tables

-- 1. Notification Configs Table (Twilio, Evolution API, Z-API, SMTP)
CREATE TABLE notification_configs (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id),
    provider_type VARCHAR(50) NOT NULL, -- TWILIO, EVOLUTION_API, Z_API, MOCK
    name VARCHAR(150) NOT NULL,
    api_url VARCHAR(255),
    api_token VARCHAR(255),
    account_sid VARCHAR(255), -- Para Twilio
    auth_token VARCHAR(255),  -- Para Twilio
    from_phone_number VARCHAR(50), -- Ex: whatsapp:+14155238886 ou 5511999998888
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed default WhatsApp notification configs
INSERT INTO notification_configs (id, provider_type, name, account_sid, auth_token, from_phone_number, active)
VALUES
    (uuidv7(), 'TWILIO', 'Twilio WhatsApp Oficial', 'AC_twilio_demo_sid_2026', 'auth_token_demo_secret', 'whatsapp:+14155238886', TRUE),
    (uuidv7(), 'EVOLUTION_API', 'Evolution API (Self-Hosted)', NULL, 'xb_evolution_token_demo', '5511999998888', FALSE),
    (uuidv7(), 'Z_API', 'Z-API Gateway', NULL, 'xb_zapi_token_demo', '5511999997777', FALSE)
ON CONFLICT DO NOTHING;

-- 2. Notification Logs Table
CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    customer_id UUID REFERENCES customers(id),
    channel VARCHAR(30) NOT NULL, -- WHATSAPP, EMAIL, SMS
    destination VARCHAR(100) NOT NULL, -- Telefone ou E-mail
    message_type VARCHAR(50) NOT NULL, -- INVOICE_PIX, PAYMENT_CONFIRMATION, WELCOME_CREDENTIALS, PLAN_UPGRADED
    status VARCHAR(30) NOT NULL, -- SENT, FAILED
    external_id VARCHAR(100),
    payload TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Trust Unblocks Table (Promessa de Pagamento / Desbloqueio em Confiança por 48h)
CREATE TABLE trust_unblocks (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    requested_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, CANCELLED
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Plan Upgrade Requests Table
CREATE TABLE plan_upgrade_requests (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    old_plan_id UUID REFERENCES plans(id),
    new_plan_id UUID NOT NULL REFERENCES plans(id),
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED', -- REQUESTED, COMPLETED, REJECTED
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_notif_configs_provider ON notification_configs(provider_type);
CREATE INDEX idx_notif_logs_customer ON notification_logs(customer_id);
CREATE INDEX idx_notif_logs_created ON notification_logs(created_at);
CREATE INDEX idx_trust_unblocks_contract ON trust_unblocks(contract_id);
CREATE INDEX idx_plan_upgrades_contract ON plan_upgrade_requests(contract_id);
