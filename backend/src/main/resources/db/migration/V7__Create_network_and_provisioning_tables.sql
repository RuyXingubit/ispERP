-- V7: Create network devices and ONU provisioning tables

-- 1. Network Devices Table (OLTs, Concentradores, Servidores de Provisionamento)
CREATE TABLE network_devices (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(150) NOT NULL,
    device_type VARCHAR(50) NOT NULL DEFAULT 'OLT', -- OLT, BRAS_PPPOE, RADIUS_SERVER
    driver_type VARCHAR(50) NOT NULL DEFAULT 'SMARTOLT', -- SMARTOLT, EXTERNAL_MICROSERVICE, MIKROTIK_ROUTEROS, RADIUS, MOCK
    ip_address VARCHAR(100) NOT NULL,
    api_port INT DEFAULT 443,
    api_token VARCHAR(255),
    snmp_community VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial default network device (SmartOLT Central)
INSERT INTO network_devices (id, name, device_type, driver_type, ip_address, api_port, api_token, snmp_community, active)
VALUES
    (uuidv7(), 'SmartOLT Central - POP 01', 'OLT', 'SMARTOLT', 'api.smartolt.com', 443, 'xb_smartolt_demo_token_2026', 'public', TRUE)
ON CONFLICT DO NOTHING;

-- 2. ONU Provisionings Table (ONUs / Modems de Clientes provisionados na rede)
CREATE TABLE onu_provisionings (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    network_device_id UUID REFERENCES network_devices(id),
    onu_mac VARCHAR(50) NOT NULL,
    onu_serial VARCHAR(50) NOT NULL,
    vlan_id INT NOT NULL DEFAULT 100,
    pppoe_user VARCHAR(100),
    pppoe_password VARCHAR(100),
    download_speed INT NOT NULL, -- Mbps
    upload_speed INT NOT NULL,   -- Mbps
    status VARCHAR(30) NOT NULL DEFAULT 'PROVISIONED', -- PROVISIONED, BLOCKED, DEPROVISIONED
    rx_power_dbm NUMERIC(5, 2),
    last_sync_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_network_devices_driver ON network_devices(driver_type);
CREATE INDEX idx_network_devices_active ON network_devices(active);
CREATE INDEX idx_onu_provisionings_contract ON onu_provisionings(contract_id);
CREATE INDEX idx_onu_provisionings_mac ON onu_provisionings(onu_mac);
CREATE INDEX idx_onu_provisionings_status ON onu_provisionings(status);
