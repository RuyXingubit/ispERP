-- V33: Add quantity and balance_after to custody_logs for immutable Kardex tracking

ALTER TABLE custody_logs
    ADD COLUMN IF NOT EXISTS quantity INTEGER,
    ADD COLUMN IF NOT EXISTS balance_after INTEGER;

CREATE INDEX IF NOT EXISTS idx_custody_logs_item_id ON custody_logs(item_id, created_at DESC);

-- Saldo inicial de implantação para itens já cadastrados sem histórico prévio
INSERT INTO custody_logs (id, item_id, event_type, quantity, balance_after, notes, created_at)
SELECT 
    uuidv7(), 
    i.id, 
    'STOCK_ENTRY', 
    i.quantity_in_stock, 
    i.quantity_in_stock, 
    'Saldo Inicial de Implantação do Almoxarifado', 
    i.created_at
FROM inventory_items i
WHERE NOT EXISTS (
    SELECT 1 FROM custody_logs cl WHERE cl.item_id = i.id
) AND i.quantity_in_stock > 0;
