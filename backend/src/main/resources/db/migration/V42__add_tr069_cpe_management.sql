CREATE TABLE IF NOT EXISTS tr069_cpe_configs (
    id UUID PRIMARY KEY,
    contract_id UUID NOT NULL UNIQUE REFERENCES contracts(id) ON DELETE CASCADE,
    onu_id UUID,
    onu_serial VARCHAR(64),
    cpe_model VARCHAR(120) NOT NULL DEFAULT 'ONT Dual-Band Wi-Fi 6 Gigabit',
    firmware_version VARCHAR(64) NOT NULL DEFAULT 'V5R020C00S280',
    uptime_seconds BIGINT NOT NULL DEFAULT 86400,
    ssid_24ghz VARCHAR(32) NOT NULL,
    password_24ghz VARCHAR(128) NOT NULL,
    channel_24ghz VARCHAR(16) NOT NULL DEFAULT 'AUTO',
    ssid_5ghz VARCHAR(32) NOT NULL,
    password_5ghz VARCHAR(128) NOT NULL,
    channel_5ghz VARCHAR(16) NOT NULL DEFAULT 'AUTO',
    band_steering_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    security_mode VARCHAR(32) NOT NULL DEFAULT 'WPA2-PSK/WPA3-SAE',
    acs_sync_status VARCHAR(32) NOT NULL DEFAULT 'SYNCED_EMBEDDED_ACS',
    last_inform_at TIMESTAMP,
    last_reboot_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tr069_cpe_configs_contract_id ON tr069_cpe_configs(contract_id);

CREATE TABLE IF NOT EXISTS tr069_command_logs (
    id UUID PRIMARY KEY,
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    onu_serial VARCHAR(64),
    command_type VARCHAR(32) NOT NULL,
    origin_channel VARCHAR(32) NOT NULL,
    requested_by VARCHAR(120),
    status VARCHAR(32) NOT NULL,
    summary_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tr069_command_logs_contract_id ON tr069_command_logs(contract_id, created_at DESC);
