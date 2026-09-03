-- V32: Add portal pin and force change flag to customers for secure Client Portal access
ALTER TABLE customers 
ADD COLUMN IF NOT EXISTS portal_pin VARCHAR(255),
ADD COLUMN IF NOT EXISTS pin_force_change BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_customers_cpf ON customers(cpf);
