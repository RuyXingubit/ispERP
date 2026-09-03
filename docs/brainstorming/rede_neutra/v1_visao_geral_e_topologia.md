# Brainstorming & Arquitetura: Rede Neutra & API Inter-Provedores (B2B)

> **Status:** Rascunho Vivo / Em Discussão  
> **Data:** 2026-09-01  
> **Objetivo:** Definir os modelos de negócio, arquitetura técnica, segurança e integração entre provedores (Wholesale FTTH / Neutral Network) no ispERP.

---

## 1. Visão Geral e Contexto
O ecossistema de telecomunicações evoluiu para modelos de infraestrutura compartilhada (Open Fiber / Redes Neutras).
No **ispERP**, o sistema deve estar preparado para atuar de forma bilateral:
1. **ispERP como Operador de Infraestrutura (NetCo / Host):** O ISP dono da rede física (OLTs, cabos, CTOs) expõe APIs seguras para que provedores parceiros (VNOs) vendam e atendam na sua mancha de cobertura.
2. **ispERP como Consumidor de Rede Neutra (ServCo / Inquilino):** O ISP utiliza redes neutras de mercado (V.tal, FiBrasil, I-Systems ou outro ispERP parceiro) para expandir suas vendas sem investimento em infraestrutura passiva.

---

## 2. Tópicos e Decisões em Construção

### A. Modelo de Negócio & Cobrança de Atacado (Wholesale)
- [ ] *Em definição pelo time:* Estrutura de remuneração (aluguel fixo por porta CTO, taxa de ativação de campo, split de mensalidade, franquia de banda).
- [ ] *Em definição pelo time:* Gestão de SLA de reparo e penalidades.

### B. Arquitetura de Transporte & Rede (L2 vs L3)
- [ ] *Em definição pelo time:* Transporte L2 QinQ (SVLAN + CVLAN entregando no BNG/PTT do parceiro).
- [ ] *Em definição pelo time:* Autenticação PPPoE/IPoE (BNG centralizado vs BNG do parceiro com proxy FreeRADIUS / Realm).

### C. Gestão de Equipamentos e Campo
- [ ] *Em definição pelo time:* Fornecimento e homologação de ONUs/ONTs (dono da rede vs parceiro).
- [ ] *Em definição pelo time:* Fluxo de O.S. técnica de instalação e reparo com equipes de campo.

### D. Segurança, Governança & Multi-Tenant B2B
- [ ] Isolamento estrito de dados entre parceiros (Data Segregation).
- [ ] Autenticação B2B via OAuth2 Client Credentials / mTLS.
- [ ] Webhooks assinados com HMAC-SHA256 para eventos de rede e O.S.
- [ ] Rate Limiting para proteção contra raspagem de viabilidade/cobertura.

---

## 3. Próximos Passos
*(Será atualizado em tempo real conforme avançarmos na discussão)*
