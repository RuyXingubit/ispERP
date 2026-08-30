-- ==============================================================================
-- afterMigrate.sql (EXECUTADO EXCLUSIVAMENTE NOS AMBIENTES LOCAL / DEV)
-- SEEDER DE HOMOLOGAÇÃO E TESTES - NUNCA EXECUTADO EM PRODUÇÃO
-- ==============================================================================

-- 1. Planos de Internet
INSERT INTO plans (id, name, download_speed, upload_speed, price, description, sva_included, active, created_at, updated_at)
VALUES 
  ('01918a00-0000-7000-8000-000000000001', 'Fibra 300 Mega Residencial', 300, 150, 79.90, 'Ideal para streaming e home office', 'Clube de Vantagens', true, NOW(), NOW()),
  ('01918a00-0000-7000-8000-000000000002', 'Fibra 500 Mega Ultra Gamer', 500, 250, 99.90, 'Alta performance e baixa latência', 'Paramount+, Deezer', true, NOW(), NOW()),
  ('01918a00-0000-7000-8000-000000000003', 'Fibra 1 Giga Dedicado Corporativo', 1000, 500, 199.90, 'IP Fixo e SLA de 4 horas', 'IP Dedicado, Backup Cloud', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. Depósitos do Almoxarifado
INSERT INTO warehouses (id, name, type, active, created_at, updated_at)
VALUES
  ('01918a00-0000-7000-8000-000000000010', 'Almoxarifado Central (Sede)', 'CENTRAL', true, NOW(), NOW()),
  ('01918a00-0000-7000-8000-000000000011', 'Veículo Técnico 01 - Carlos', 'VEHICLE', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
