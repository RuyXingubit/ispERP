-- ==============================================================================
-- V12: HELPDESK COM PROTOCOLO OBRIGATÓRIO ANATEL & FATURAMENTO FISCAL NFCOM (MODELO 62)
-- ==============================================================================

-- 1. Tabela de Chamados de Atendimento / Suporte (Helpdesk) com Protocolo ANATEL
CREATE TABLE IF NOT EXISTS helpdesk_tickets (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    protocol VARCHAR(32) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    contract_id UUID REFERENCES contracts(id) ON DELETE SET NULL,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    channel VARCHAR(30) NOT NULL DEFAULT 'PHONE',
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    assigned_to_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    work_order_id UUID REFERENCES work_orders(id) ON DELETE SET NULL,
    sla_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    resolution_notes TEXT,
    anatel_satisfaction_rating INTEGER,
    resolved_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_helpdesk_tickets_protocol ON helpdesk_tickets(protocol);
CREATE INDEX IF NOT EXISTS idx_helpdesk_tickets_customer ON helpdesk_tickets(customer_id);
CREATE INDEX IF NOT EXISTS idx_helpdesk_tickets_status ON helpdesk_tickets(status);
CREATE INDEX IF NOT EXISTS idx_helpdesk_tickets_sla ON helpdesk_tickets(sla_deadline);

-- 2. Tabela de Histórico / Interações do Chamado (Públicas e Notas Internas)
CREATE TABLE IF NOT EXISTS ticket_interactions (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    ticket_id UUID NOT NULL REFERENCES helpdesk_tickets(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    is_internal_note BOOLEAN NOT NULL DEFAULT FALSE,
    sender_type VARCHAR(30) NOT NULL,
    sender_name VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    attachment_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ticket_interactions_ticket ON ticket_interactions(ticket_id);

-- 3. Colunas de NFCom (Modelo 62 - Nota Fiscal Fatura de Serviço de Comunicação)
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS nfcom_number INTEGER,
    ADD COLUMN IF NOT EXISTS nfcom_series INTEGER,
    ADD COLUMN IF NOT EXISTS nfcom_key VARCHAR(44),
    ADD COLUMN IF NOT EXISTS nfcom_xml_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS nfcom_pdf_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS nfcom_status VARCHAR(30) NOT NULL DEFAULT 'NOT_APPLICABLE',
    ADD COLUMN IF NOT EXISTS nfcom_issued_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS nfcom_error_message TEXT;

CREATE INDEX IF NOT EXISTS idx_invoices_nfcom_key ON invoices(nfcom_key);
CREATE INDEX IF NOT EXISTS idx_invoices_nfcom_status ON invoices(nfcom_status);
