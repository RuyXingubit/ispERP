-- =====================================================================
-- Flyway Migration: V36__add_radpostauth_and_radacct_missing_columns.sql
-- Description: Adiciona tabela radpostauth, nasreload e colunas padrão
--              FreeRADIUS PostgreSQL (framedipv6address, framedinterfaceid, class)
-- Standard: PostgreSQL 17+ with UUIDv7
-- =====================================================================

-- 1. Tabela de logs de autenticação pós-processamento (FreeRADIUS post-auth)
CREATE TABLE IF NOT EXISTS radpostauth (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    pass VARCHAR(64),
    reply VARCHAR(32),
    calledstationid VARCHAR(50),
    callingstationid VARCHAR(50),
    authdate TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    class VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_radpostauth_username ON radpostauth(username);
CREATE INDEX IF NOT EXISTS idx_radpostauth_authdate ON radpostauth(authdate);

-- 2. Colunas complementares padrão RFC / FreeRADIUS para a tabela radacct
ALTER TABLE radacct
    ADD COLUMN IF NOT EXISTS framedipv6address VARCHAR(45),
    ADD COLUMN IF NOT EXISTS framedinterfaceid VARCHAR(45),
    ADD COLUMN IF NOT EXISTS class VARCHAR(64);

COMMENT ON COLUMN radacct.framedipv6address IS 'Endereço IPv6 atribuído ao túnel PPP ou IPoE';
COMMENT ON COLUMN radacct.framedinterfaceid IS 'Interface Identifier IPv6 (RFC 3162)';
COMMENT ON COLUMN radacct.class IS 'Atributo RADIUS Class para rastreamento de bilhetagem e auditoria';

-- 3. Tabela auxiliar para recarregamento dinâmico de NAS (FreeRADIUS nasreload)
CREATE TABLE IF NOT EXISTS nasreload (
    nasipaddress VARCHAR(45) PRIMARY KEY,
    reloadtime TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
