-- =========================================================================
-- V22: Proteção de Horário Comercial, Dias Úteis e Feriados no Auto-Corte
-- =========================================================================

ALTER TABLE radius_policy_configs
    ADD COLUMN IF NOT EXISTS block_start_hour INTEGER NOT NULL DEFAULT 9,
    ADD COLUMN IF NOT EXISTS block_end_hour INTEGER NOT NULL DEFAULT 11,
    ADD COLUMN IF NOT EXISTS allow_block_on_friday BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS protect_eve_of_holidays BOOLEAN NOT NULL DEFAULT TRUE;
