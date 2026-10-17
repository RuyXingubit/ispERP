-- V40: Adiciona coluna ibge_code na tabela contracts para cache do código IBGE municipal (Onda K - DICI/SICI Anatel)
ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS ibge_code VARCHAR(7);

CREATE INDEX IF NOT EXISTS idx_contracts_ibge_code ON contracts(ibge_code);
