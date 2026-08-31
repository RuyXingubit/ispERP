-- =========================================================================
-- V23: Schema de Documentação FTTH, Gestão de Fibras, Fusões e CTOs
-- =========================================================================

-- 1. POPs / Centrais / Headends
CREATE TABLE ftth_pops (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id),
    name VARCHAR(150) NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    address VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Postes / Infraestrutura Passiva Georreferenciada
CREATE TABLE ftth_poles (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id),
    code VARCHAR(50) NOT NULL, -- Ex: P-10492
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    pole_type VARCHAR(50) DEFAULT 'CONCRETO' NOT NULL, -- CONCRETO, MADEIRA, METALICO
    reservation_meters INT DEFAULT 0 NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 3. Cabos Ópticos com Padrão de Cores (ABNT NBR 14106 ou TIA/EIA-598)
CREATE TABLE ftth_cables (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id),
    name VARCHAR(150) NOT NULL, -- Ex: CAB-TRONCAL-01
    cable_type VARCHAR(50) DEFAULT 'DISTRIBUICAO' NOT NULL, -- ALIMENTADOR, DISTRIBUICAO, DROP
    fiber_count INT DEFAULT 12 NOT NULL, -- 6, 12, 24, 36, 72, 144 FO
    tube_count INT DEFAULT 1 NOT NULL, -- Número de tubos loose
    color_standard VARCHAR(30) DEFAULT 'ABNT_NBR_14106' NOT NULL, -- ABNT_NBR_14106, TIA_EIA_598
    length_meters DECIMAL(10, 2) DEFAULT 0.00 NOT NULL,
    path_coordinates JSONB, -- Array de coordenadas [ [lng, lat], ... ]
    source_pop_id UUID REFERENCES ftth_pops(id),
    source_pole_id UUID REFERENCES ftth_poles(id),
    target_pole_id UUID REFERENCES ftth_poles(id),
    attenuation_db_per_km DECIMAL(4, 2) DEFAULT 0.35 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 4. Caixas de Emenda Óptica (CEO / FOSC)
CREATE TABLE ftth_closures (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id),
    name VARCHAR(150) NOT NULL, -- Ex: CEO-AV-BRASIL-01
    pole_id UUID REFERENCES ftth_poles(id),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    closure_type VARCHAR(50) DEFAULT 'DOMO' NOT NULL, -- DOMO, RETANGULAR, SUBTERRANEA
    tray_count INT DEFAULT 4 NOT NULL,
    capacity_fusions INT DEFAULT 48 NOT NULL,
    status VARCHAR(30) DEFAULT 'ATIVA' NOT NULL, -- ATIVA, MANUTENCAO, ESGOTADA
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 5. Splitters Ópticos (Balanceados PLC e Desbalanceados FBT)
CREATE TABLE ftth_splitters (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id),
    closure_id UUID REFERENCES ftth_closures(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL, -- Ex: SPL-1:8-01 ou SPL-FBT-80/20
    splitter_type VARCHAR(50) DEFAULT 'BALANCED_1_8' NOT NULL,
    -- BALANCED_1_2, BALANCED_1_4, BALANCED_1_8, BALANCED_1_16, BALANCED_1_32
    -- UNBALANCED_95_05, UNBALANCED_90_10, UNBALANCED_80_20, UNBALANCED_70_30, UNBALANCED_60_40, UNBALANCED_50_50
    input_cable_id UUID REFERENCES ftth_cables(id),
    input_fiber_number INT,
    attenuation_db DECIMAL(4, 2) DEFAULT 10.50 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 6. Caixas de Terminação Óptica (CTO / NAP)
CREATE TABLE ftth_ctos (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id),
    name VARCHAR(150) NOT NULL, -- Ex: CTO-CENTRO-08
    pole_id UUID REFERENCES ftth_poles(id),
    closure_id UUID REFERENCES ftth_closures(id), -- Caixa de emenda alimentadora
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    total_ports INT DEFAULT 16 NOT NULL, -- 8 ou 16 portas
    splitter_type VARCHAR(50) DEFAULT 'BALANCED_1_16' NOT NULL,
    status VARCHAR(30) DEFAULT 'ATIVA' NOT NULL, -- ATIVA, MANUTENCAO, ESGOTADA
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 7. Portas de Atendimento da CTO (SC-APC)
CREATE TABLE ftth_cto_ports (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    cto_id UUID NOT NULL REFERENCES ftth_ctos(id) ON DELETE CASCADE,
    port_number INT NOT NULL, -- 1 a 16
    status VARCHAR(30) DEFAULT 'LIVRE' NOT NULL, -- LIVRE, OCUPADA, RESERVADA, DEFEITO
    onu_provisioning_id UUID REFERENCES onu_provisionings(id) ON DELETE SET NULL,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_cto_port_number UNIQUE (cto_id, port_number)
);

-- 8. Fusões Fibra-a-Fibra / Splitters (Diagrama Unifilar)
CREATE TABLE ftth_fusions (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_id UUID REFERENCES companies(id),
    closure_id UUID NOT NULL REFERENCES ftth_closures(id) ON DELETE CASCADE,
    tray_number INT DEFAULT 1 NOT NULL,
    
    -- Fibra de Origem (Entrada)
    source_cable_id UUID NOT NULL REFERENCES ftth_cables(id) ON DELETE CASCADE,
    source_fiber_number INT NOT NULL, -- 1 a N
    
    -- Destino A: Outra Fibra de Saída
    target_cable_id UUID REFERENCES ftth_cables(id) ON DELETE CASCADE,
    target_fiber_number INT,
    
    -- Destino B: Entrada de Splitter
    target_splitter_id UUID REFERENCES ftth_splitters(id) ON DELETE CASCADE,
    
    -- Destino C: Alimentação Direta de CTO
    target_cto_id UUID REFERENCES ftth_ctos(id) ON DELETE CASCADE,
    
    loss_db DECIMAL(4, 2) DEFAULT 0.05 NOT NULL, -- Atenuação da fusão em dB
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 9. Índices para Otimização de Consultas Espaciais e Grafos
CREATE INDEX idx_ftth_cables_type ON ftth_cables(cable_type);
CREATE INDEX idx_ftth_cables_color_standard ON ftth_cables(color_standard);
CREATE INDEX idx_ftth_closures_pole ON ftth_closures(pole_id);
CREATE INDEX idx_ftth_ctos_pole ON ftth_ctos(pole_id);
CREATE INDEX idx_ftth_cto_ports_cto ON ftth_cto_ports(cto_id);
CREATE INDEX idx_ftth_cto_ports_status ON ftth_cto_ports(status);
CREATE INDEX idx_ftth_cto_ports_onu ON ftth_cto_ports(onu_provisioning_id);
CREATE INDEX idx_ftth_fusions_closure ON ftth_fusions(closure_id);
CREATE INDEX idx_ftth_fusions_source_cable ON ftth_fusions(source_cable_id);
CREATE INDEX idx_ftth_fusions_target_cable ON ftth_fusions(target_cable_id);
