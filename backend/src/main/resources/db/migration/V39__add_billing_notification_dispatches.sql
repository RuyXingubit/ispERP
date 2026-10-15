-- V39: Régua de Notificação Ativa Omnichannel com trilha auditável e idempotência por (invoice_id, stage, channel) (Onda J)

CREATE TABLE IF NOT EXISTS billing_notification_dispatches (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    customer_name VARCHAR(200),
    stage VARCHAR(40) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    recipient VARCHAR(180) NOT NULL,
    message_preview TEXT NOT NULL,
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'SENT',
    dispatched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_billing_notif_invoice_stage_channel UNIQUE (invoice_id, stage, channel)
);

CREATE INDEX IF NOT EXISTS idx_billing_notif_invoice_id
    ON billing_notification_dispatches(invoice_id, dispatched_at DESC);

CREATE INDEX IF NOT EXISTS idx_billing_notif_dispatched_at
    ON billing_notification_dispatches(dispatched_at DESC);
