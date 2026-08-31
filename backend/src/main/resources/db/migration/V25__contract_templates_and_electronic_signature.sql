-- V25: Schema para Templates Customizáveis de Contratos e Assinatura Eletrônica Avançada via Pix (MP 2.200-2/2001 e Lei 14.063/2020)

-- 1. Modelos e Templates de Contratos do Provedor
CREATE TABLE IF NOT EXISTS contract_templates (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    document_type VARCHAR(50) NOT NULL, -- SERVICE_AGREEMENT, LOYALTY_TERM, EQUIPMENT_COMODATO, CUSTOM_TERM
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    content_markdown TEXT NOT NULL,
    consent_clause TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Sessões de Assinatura Eletrônica e Auditoria via Pix
CREATE TABLE IF NOT EXISTS contract_signatures (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    template_id UUID REFERENCES contract_templates(id) ON DELETE SET NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, SIGNED, REJECTED_DIVERGENT_DOCUMENT, EXPIRED, CANCELED
    symbolic_amount NUMERIC(10,2) NOT NULL DEFAULT 1.00,
    pix_txid VARCHAR(100),
    pix_end_to_end_id VARCHAR(100),
    pix_copy_paste TEXT,
    pix_qr_code_base64 TEXT,
    rendered_content_snapshot TEXT,
    document_sha256_hash VARCHAR(64),
    client_ip VARCHAR(50),
    client_user_agent TEXT,
    client_geo_latitude NUMERIC(10,8),
    client_geo_longitude NUMERIC(11,8),
    payer_name VARCHAR(255),
    payer_cpf_cnpj VARCHAR(20),
    payer_bank_name VARCHAR(100),
    payer_bank_ispb VARCHAR(20),
    rejection_reason TEXT,
    signed_pdf_url TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    signed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices de performance
CREATE INDEX IF NOT EXISTS idx_contract_templates_company ON contract_templates(company_id);
CREATE INDEX IF NOT EXISTS idx_contract_signatures_contract ON contract_signatures(contract_id);
CREATE INDEX IF NOT EXISTS idx_contract_signatures_token ON contract_signatures(token);
CREATE INDEX IF NOT EXISTS idx_contract_signatures_txid ON contract_signatures(pix_txid);
