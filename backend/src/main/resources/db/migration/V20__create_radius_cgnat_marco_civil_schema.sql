-- =====================================================================
-- Flyway Migration: V20__create_radius_cgnat_marco_civil_schema.sql
-- Description: FreeRADIUS Multi-Vendor, CGNAT Mapping & Marco Civil Reports Schema
-- Standard: PostgreSQL 17+ with UUIDv7
-- =====================================================================

-- 1. NAS (Network Access Servers / BNGs / Routers)
CREATE TABLE IF NOT EXISTS nas (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    nasname VARCHAR(128) NOT NULL UNIQUE,
    shortname VARCHAR(32),
    type VARCHAR(30) NOT NULL DEFAULT 'other',
    ports INT,
    secret VARCHAR(64) NOT NULL,
    server VARCHAR(64),
    community VARCHAR(64),
    description VARCHAR(200),
    vendor_type VARCHAR(50) NOT NULL DEFAULT 'MIKROTIK', -- MIKROTIK, HUAWEI, JUNIPER, ACCEL_PPP, CISCO, GENERIC
    company_id UUID REFERENCES companies(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_nas_nasname ON nas(nasname);

-- 2. FreeRADIUS Check Attributes (Authentication)
CREATE TABLE IF NOT EXISTS radcheck (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    username VARCHAR(64) NOT NULL,
    attribute VARCHAR(64) NOT NULL,
    op VARCHAR(2) NOT NULL DEFAULT '==',
    value VARCHAR(253) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_radcheck_username ON radcheck(username);

-- 3. FreeRADIUS Reply Attributes (Authorization / Rate-Limit / IP / Routes)
CREATE TABLE IF NOT EXISTS radreply (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    username VARCHAR(64) NOT NULL,
    attribute VARCHAR(64) NOT NULL,
    op VARCHAR(2) NOT NULL DEFAULT '=',
    value VARCHAR(253) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_radreply_username ON radreply(username);

-- 4. FreeRADIUS Group Check Attributes
CREATE TABLE IF NOT EXISTS radgroupcheck (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    groupname VARCHAR(64) NOT NULL,
    attribute VARCHAR(64) NOT NULL,
    op VARCHAR(2) NOT NULL DEFAULT '==',
    value VARCHAR(253) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_radgroupcheck_groupname ON radgroupcheck(groupname);

-- 5. FreeRADIUS Group Reply Attributes (Profiles de Velocidade / Bloqueio)
CREATE TABLE IF NOT EXISTS radgroupreply (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    groupname VARCHAR(64) NOT NULL,
    attribute VARCHAR(64) NOT NULL,
    op VARCHAR(2) NOT NULL DEFAULT '=',
    value VARCHAR(253) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_radgroupreply_groupname ON radgroupreply(groupname);

-- 6. FreeRADIUS User Group Mapping
CREATE TABLE IF NOT EXISTS radusergroup (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    username VARCHAR(64) NOT NULL,
    groupname VARCHAR(64) NOT NULL,
    priority INT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_radusergroup_username ON radusergroup(username);

-- 7. FreeRADIUS Accounting Sessions (radacct)
CREATE TABLE IF NOT EXISTS radacct (
    radacctid BIGSERIAL PRIMARY KEY,
    acctsessionid VARCHAR(64) NOT NULL,
    acctuniqueid VARCHAR(32) NOT NULL UNIQUE,
    username VARCHAR(64) NOT NULL,
    realm VARCHAR(64) DEFAULT '',
    nasipaddress VARCHAR(45) NOT NULL,
    nasportid VARCHAR(32),
    nasporttype VARCHAR(32),
    acctstarttime TIMESTAMP WITH TIME ZONE,
    acctupdatetime TIMESTAMP WITH TIME ZONE,
    acctstoptime TIMESTAMP WITH TIME ZONE,
    acctinterval INT,
    acctsessiontime INT,
    acctauthentic VARCHAR(32),
    connectinfo_start VARCHAR(128),
    connectinfo_stop VARCHAR(128),
    acctinputoctets BIGINT DEFAULT 0,
    acctoutputoctets BIGINT DEFAULT 0,
    calledstationid VARCHAR(50),
    callingstationid VARCHAR(50),
    acctterminatecause VARCHAR(32),
    servicetype VARCHAR(32),
    framedprotocol VARCHAR(32),
    framedipaddress VARCHAR(45),
    framedipv6prefix VARCHAR(45),
    delegatedipv6prefix VARCHAR(45)
);

CREATE INDEX IF NOT EXISTS idx_radacct_username ON radacct(username);
CREATE INDEX IF NOT EXISTS idx_radacct_active ON radacct(acctstoptime) WHERE acctstoptime IS NULL;
CREATE INDEX IF NOT EXISTS idx_radacct_times ON radacct(acctstarttime, acctstoptime);
CREATE INDEX IF NOT EXISTS idx_radacct_ip ON radacct(framedipaddress);
CREATE INDEX IF NOT EXISTS idx_radacct_callingstationid ON radacct(callingstationid);
CREATE INDEX IF NOT EXISTS idx_radacct_nasip ON radacct(nasipaddress);

-- 8. CGNAT Mappings (Deterministic & Script Port Allocations by NAS)
CREATE TABLE IF NOT EXISTS cgnat_mappings (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    nas_id UUID REFERENCES nas(id) ON DELETE CASCADE,
    vendor_type VARCHAR(50) NOT NULL, -- MIKROTIK, HUAWEI, A10, HILLSTONE, CISCO, ACCEL_PPP, GENERIC
    public_ip VARCHAR(45) NOT NULL,
    port_start INT NOT NULL,
    port_end INT NOT NULL,
    private_ip_start VARCHAR(45) NOT NULL,
    private_ip_end VARCHAR(45) NOT NULL,
    protocol VARCHAR(10) NOT NULL DEFAULT 'BOTH', -- TCP, UDP, BOTH
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cgnat_public_ip_ports ON cgnat_mappings(public_ip, port_start, port_end);
CREATE INDEX IF NOT EXISTS idx_cgnat_private_ip ON cgnat_mappings(private_ip_start);
CREATE INDEX IF NOT EXISTS idx_cgnat_nas_id ON cgnat_mappings(nas_id);

-- 9. Marco Civil Investigation Reports (Laudos Periciais Oficiais com Validação Anti-Fraude)
CREATE TABLE IF NOT EXISTS marco_civil_reports (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    validation_token VARCHAR(64) NOT NULL UNIQUE,
    sha256_hash VARCHAR(64) NOT NULL,
    court_order_number VARCHAR(100),
    requester_authority VARCHAR(150),
    queried_ip VARCHAR(45) NOT NULL,
    queried_port INT,
    queried_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    matched_contract_id UUID REFERENCES contracts(id) ON DELETE SET NULL,
    matched_customer_name VARCHAR(150),
    matched_cpf_cnpj VARCHAR(20),
    matched_calling_station_id VARCHAR(50),
    matched_session_start TIMESTAMP WITH TIME ZONE,
    matched_session_stop TIMESTAMP WITH TIME ZONE,
    report_pdf_url TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_marco_civil_token ON marco_civil_reports(validation_token);
CREATE INDEX IF NOT EXISTS idx_marco_civil_hash ON marco_civil_reports(sha256_hash);
