-- ==============================================================================
-- V13: MULTI-GATEWAY FISCAL, ONBOARDING DE EMPRESA & EMISSÃO DE NFCOM (MODELO 62)
-- ==============================================================================

-- 1. Tabela de Dados Cadastrais & Fiscais das Empresas Emissoras (Matriz e Filiais)
CREATE TABLE IF NOT EXISTS fiscal_companies (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    cnpj VARCHAR(18) NOT NULL UNIQUE,
    razao_social VARCHAR(255) NOT NULL,
    nome_fantasia VARCHAR(255),
    inscricao_estadual VARCHAR(30) NOT NULL,
    inscricao_municipal VARCHAR(30),
    cnae_principal VARCHAR(20) NOT NULL DEFAULT '6110-8/03',
    regime_tributario VARCHAR(30) NOT NULL DEFAULT 'SIMPLES_NACIONAL',
    aliquota_icms DECIMAL(5,2) DEFAULT 0.00,
    aliquota_fust DECIMAL(5,2) DEFAULT 0.65,
    aliquota_funttel DECIMAL(5,2) DEFAULT 0.50,
    aliquota_pis DECIMAL(5,2) DEFAULT 0.65,
    aliquota_cofins DECIMAL(5,2) DEFAULT 3.00,
    logradouro VARCHAR(255) NOT NULL,
    numero VARCHAR(50) NOT NULL,
    complemento VARCHAR(100),
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    cep VARCHAR(10) NOT NULL,
    codigo_ibge VARCHAR(10) NOT NULL,
    telefone VARCHAR(30),
    email_fiscal VARCHAR(255),
    nfcom_ambiente VARCHAR(20) NOT NULL DEFAULT 'HOMOLOGACAO',
    nfcom_serie VARCHAR(10) NOT NULL DEFAULT '1',
    nfcom_proximo_numero INTEGER NOT NULL DEFAULT 1,
    has_certificate BOOLEAN NOT NULL DEFAULT FALSE,
    certificate_expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fiscal_companies_cnpj ON fiscal_companies(cnpj);

-- 2. Tabela de Configuração dos Gateways Fiscais (Multi-Gateway Plugável)
CREATE TABLE IF NOT EXISTS fiscal_gateway_configs (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID NOT NULL REFERENCES fiscal_companies(id) ON DELETE CASCADE,
    gateway_type VARCHAR(50) NOT NULL DEFAULT 'XINGUBIT_PAY',
    environment VARCHAR(20) NOT NULL DEFAULT 'HOMOLOGACAO',
    client_id VARCHAR(255),
    client_secret VARCHAR(255),
    api_key VARCHAR(255),
    base_url VARCHAR(255) DEFAULT 'https://pay.xingubit.com.br',
    webhook_secret VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fiscal_gateway_configs_company ON fiscal_gateway_configs(company_id);
CREATE INDEX IF NOT EXISTS idx_fiscal_gateway_configs_type ON fiscal_gateway_configs(gateway_type);

-- 3. Tabela de Registro Detalhado de NFCom (Modelo 62) Emitidas
CREATE TABLE IF NOT EXISTS nfcom_records (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID NOT NULL REFERENCES fiscal_companies(id) ON DELETE RESTRICT,
    invoice_id UUID REFERENCES invoices(id) ON DELETE SET NULL,
    contract_id UUID REFERENCES contracts(id) ON DELETE SET NULL,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    chave_acesso VARCHAR(44) UNIQUE,
    numero INTEGER NOT NULL,
    serie VARCHAR(10) NOT NULL DEFAULT '1',
    modelo VARCHAR(10) NOT NULL DEFAULT '62',
    tipo_emissao VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    ambiente VARCHAR(20) NOT NULL DEFAULT 'HOMOLOGACAO',
    status VARCHAR(30) NOT NULL DEFAULT 'EMITIDA',
    protocolo_autorizacao VARCHAR(60),
    data_autorizacao TIMESTAMP WITH TIME ZONE,
    digest_value VARCHAR(100),
    valor_total DECIMAL(12,2) NOT NULL,
    valor_icms DECIMAL(12,2) DEFAULT 0.00,
    valor_fust DECIMAL(12,2) DEFAULT 0.00,
    valor_funttel DECIMAL(12,2) DEFAULT 0.00,
    xml_autorizado TEXT,
    danfe_pdf_url VARCHAR(500),
    motivo_cancelamento TEXT,
    data_cancelamento TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_nfcom_records_chave ON nfcom_records(chave_acesso);
CREATE INDEX IF NOT EXISTS idx_nfcom_records_company ON nfcom_records(company_id);
CREATE INDEX IF NOT EXISTS idx_nfcom_records_invoice ON nfcom_records(invoice_id);
CREATE INDEX IF NOT EXISTS idx_nfcom_records_status ON nfcom_records(status);
