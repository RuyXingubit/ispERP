-- Adicionar campos faltantes na tabela companies para compatibilidade com a entidade Company
ALTER TABLE companies 
ADD COLUMN website VARCHAR(255) AFTER zip_code;

-- Remover campos que não são usados pela entidade Company
ALTER TABLE companies 
DROP COLUMN city,
DROP COLUMN state,
DROP COLUMN zip_code;

-- Modificar campos para corresponder à entidade Company
ALTER TABLE companies 
MODIFY COLUMN address TEXT NULL,
MODIFY COLUMN phone VARCHAR(20) NULL,
MODIFY COLUMN email VARCHAR(255) NULL;