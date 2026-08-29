-- V4: Create plans, sales, and contracts tables

-- 1. Plans Table
CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(150) NOT NULL,
    download_speed INT NOT NULL, -- Mbps
    upload_speed INT NOT NULL,   -- Mbps
    price NUMERIC(10, 2) NOT NULL,
    description TEXT,
    sva_included VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Sales Table
CREATE TABLE sales (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    plan_id UUID NOT NULL REFERENCES plans(id),
    customer_name VARCHAR(255) NOT NULL,
    customer_cpf VARCHAR(14) NOT NULL,
    customer_email VARCHAR(255),
    customer_phone VARCHAR(20) NOT NULL,
    installation_address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    preferred_due_date INT NOT NULL DEFAULT 10,
    notification_channel VARCHAR(30) NOT NULL DEFAULT 'WHATSAPP',
    seller_name VARCHAR(150),
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Contracts Table
CREATE TABLE contracts (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    plan_id UUID NOT NULL REFERENCES plans(id),
    sale_id UUID REFERENCES sales(id),
    contract_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_INSTALLATION',
    monthly_fee NUMERIC(10, 2) NOT NULL,
    due_day INT NOT NULL DEFAULT 10,
    installation_address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_plans_active ON plans(active);
CREATE INDEX idx_sales_customer_cpf ON sales(customer_cpf);
CREATE INDEX idx_sales_status ON sales(status);
CREATE INDEX idx_contracts_customer_id ON contracts(customer_id);
CREATE INDEX idx_contracts_status ON contracts(status);
CREATE INDEX idx_contracts_contract_number ON contracts(contract_number);
