-- V34: Legal collections and removal work order enhancements

ALTER TABLE work_orders
    ADD COLUMN IF NOT EXISTS unsuccess_reason VARCHAR(100);

CREATE TABLE IF NOT EXISTS legal_collection_records (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    work_order_id UUID REFERENCES work_orders(id),
    total_debt_invoices NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    equipment_indemnity_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_claim_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_BUREAU_SUBMISSION', -- PENDING_BUREAU_SUBMISSION, SENT_TO_SERASA_SPC, IN_LEGAL_LITIGATION, RESOLVED
    evidence_notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_legal_collections_customer_id ON legal_collection_records(customer_id);
CREATE INDEX IF NOT EXISTS idx_legal_collections_contract_id ON legal_collection_records(contract_id);
CREATE INDEX IF NOT EXISTS idx_legal_collections_status ON legal_collection_records(status);
