-- ==============================================================================
-- V18: TRANSIÇÕES DE REGIME FISCAL (MUDANÇAS IMEDIATAS E AGENDADAS COM VIGÊNCIA)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS fiscal_regime_transitions (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID NOT NULL REFERENCES fiscal_companies(id) ON DELETE CASCADE,
    previous_regime VARCHAR(30) NOT NULL,
    new_regime VARCHAR(30) NOT NULL,
    effective_date DATE NOT NULL,
    aliquota_icms DECIMAL(5,2) DEFAULT 0.00,
    aliquota_pis DECIMAL(5,2) DEFAULT 0.00,
    aliquota_cofins DECIMAL(5,2) DEFAULT 0.00,
    aliquota_fust DECIMAL(5,2) DEFAULT 0.65,
    aliquota_funttel DECIMAL(5,2) DEFAULT 0.50,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    notes TEXT,
    applied_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fiscal_regime_transitions_company ON fiscal_regime_transitions(company_id);
CREATE INDEX IF NOT EXISTS idx_fiscal_regime_transitions_status ON fiscal_regime_transitions(status);
CREATE INDEX IF NOT EXISTS idx_fiscal_regime_transitions_date ON fiscal_regime_transitions(effective_date);
