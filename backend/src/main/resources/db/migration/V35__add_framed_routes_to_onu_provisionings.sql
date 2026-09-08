-- V35: Add fixed IP, IPv6 prefix, and framed routes (IPv4/IPv6) to onu_provisionings

ALTER TABLE onu_provisionings
    ADD COLUMN IF NOT EXISTS fixed_ip VARCHAR(50),
    ADD COLUMN IF NOT EXISTS ipv6_prefix VARCHAR(100),
    ADD COLUMN IF NOT EXISTS framed_route VARCHAR(150),
    ADD COLUMN IF NOT EXISTS framed_ipv6_route VARCHAR(150);

COMMENT ON COLUMN onu_provisionings.fixed_ip IS 'IP fixo IPv4 de WAN atribuído ao assinante via Framed-IP-Address';
COMMENT ON COLUMN onu_provisionings.ipv6_prefix IS 'Prefixo IPv6 delegado ao assinante via Delegated-IPv6-Prefix (ex: /56 ou /64)';
COMMENT ON COLUMN onu_provisionings.framed_route IS 'Rota estática adicional IPv4 entregue via Framed-Route (ex: bloco /29 corporativo)';
COMMENT ON COLUMN onu_provisionings.framed_ipv6_route IS 'Rota estática adicional IPv6 entregue via Framed-IPv6-Route (ex: bloco /48 corporativo)';
