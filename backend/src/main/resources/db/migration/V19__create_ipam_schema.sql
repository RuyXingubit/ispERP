-- =====================================================================
-- Flyway Migration: V19__create_ipam_schema.sql
-- Description: IPAM (IP Address Management) Core Subsystem Schema (ASNs, VRFs, Subnets, IP Inventory)
-- Standard: PostgreSQL 17+ with UUIDv7
-- =====================================================================

-- 1. ASNs (Autonomous System Numbers)
CREATE TABLE IF NOT EXISTS ipam_asns (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id) ON DELETE SET NULL,
    asn BIGINT NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    rir VARCHAR(50) NOT NULL DEFAULT 'REGISTRO_BR', -- REGISTRO_BR, LACNIC, ARIN, RIPE, APNIC, AFRINIC
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ipam_asns_asn ON ipam_asns(asn);

-- 2. VRFs (Virtual Routing and Forwarding)
CREATE TABLE IF NOT EXISTS ipam_vrfs (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    rd VARCHAR(50), -- Route Distinguisher (ex: 65000:1)
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ipam_vrfs_name_company ON ipam_vrfs(name, COALESCE(company_id, '00000000-0000-0000-0000-000000000000'::UUID));

-- 3. Subnets / Prefixes (Hierarchical Subnet Tree for IPv4 and IPv6)
CREATE TABLE IF NOT EXISTS ipam_subnets (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    parent_id UUID REFERENCES ipam_subnets(id) ON DELETE SET NULL,
    vrf_id UUID REFERENCES ipam_vrfs(id) ON DELETE SET NULL,
    asn_id UUID REFERENCES ipam_asns(id) ON DELETE SET NULL,
    company_id UUID REFERENCES companies(id) ON DELETE SET NULL,
    cidr VARCHAR(64) NOT NULL,
    ip_version VARCHAR(4) NOT NULL, -- IPV4, IPV6
    network_address VARCHAR(45) NOT NULL,
    broadcast_address VARCHAR(45),
    prefix_length INT NOT NULL,
    total_hosts BIGINT NOT NULL DEFAULT 0,
    is_pool BOOLEAN NOT NULL DEFAULT FALSE,
    pool_name VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, RESERVED, DEPRECATED
    category VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER_ACCESS', -- CUSTOMER_ACCESS, CGNAT, MANAGEMENT, INFRASTRUCTURE, PTP, LOOPBACK
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ipam_subnets_cidr ON ipam_subnets(cidr);
CREATE INDEX IF NOT EXISTS idx_ipam_subnets_parent ON ipam_subnets(parent_id);
CREATE INDEX IF NOT EXISTS idx_ipam_subnets_vrf ON ipam_subnets(vrf_id);
CREATE INDEX IF NOT EXISTS idx_ipam_subnets_version ON ipam_subnets(ip_version);

-- 4. IP Addresses Inventory (Individual / Allocated IP Address Records)
CREATE TABLE IF NOT EXISTS ipam_ip_addresses (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    subnet_id UUID NOT NULL REFERENCES ipam_subnets(id) ON DELETE CASCADE,
    ip_address VARCHAR(45) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, ALLOCATED, RESERVED, DHCP_POOL
    assigned_to_type VARCHAR(50), -- CONTRACT, NETWORK_DEVICE, INFRASTRUCTURE, CGNAT_POOL, ROUTED_SUBNET
    assigned_to_id UUID,
    dns_name VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ipam_ip_addresses_unique ON ipam_ip_addresses(subnet_id, ip_address);
CREATE INDEX IF NOT EXISTS idx_ipam_ip_addresses_ip ON ipam_ip_addresses(ip_address);
CREATE INDEX IF NOT EXISTS idx_ipam_ip_addresses_assigned ON ipam_ip_addresses(assigned_to_type, assigned_to_id);
