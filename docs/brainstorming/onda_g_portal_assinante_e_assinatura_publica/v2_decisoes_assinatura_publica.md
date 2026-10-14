# Brainstorming Onda G — Decisões de Arquitetura (Frente 1: Jornada Pública `/sign/{token}`)
**Versão:** v2 (Decisão da Assinatura Pública no `frontend-customer` e Pergunta 2 sobre Dashboard do Assinante)
**Data:** 2026-10-14

## 1. Decisão Tomada — Frente 1: Página Pública `/sign/{token}` no `frontend-customer`
O usuário aprovou a arquitetura recomendada:
- **Detecção de Rota Híbrida (`/sign/{token}` e `?token={token}` / `?sign={token}`):**
  - Ao inicializar o `frontend-customer`, se a URL contiver `/sign/<token>` no `window.location.pathname` ou `?token=<token>` / `?sign=<token>` na query string, a aplicação entra diretamente na **Visão Pública de Assinatura Eletrônica Forense (`#view-signature`)**, sem exigir login/PIN.
  - Também garante compatibilidade com SPA fallback no `nginx` e no Vite.
- **Fluxo de Consentimento Expresso (MP 2.200-2 / Lei 14.063/2020):**
  1. Exibe cabeçalho institucional com Nome da Empresa (`companyName`), Contrato (`contractName`), Titular (`customerName` + `customerDocumentMasked`) e Hash Criptográfico Inicial (`documentSha256Hash`).
  2. Exibe o texto integral da minuta contratual (`renderedContent`).
  3. Exibe o **Checkbox Obrigatório da Cláusula de Consentimento (`consentClause`)**. Somente após marcar o aceite, o painel do **PIX Simbólico de R$ 1,00 (abatido na 1ª fatura)** é desbloqueado com QR Code e botão Copia-e-Cola.
  4. Inicia **polling automático a cada 5s** (`GET /api/public/signatures/{token}/status`) + botão "Já paguei / Verificar agora":
     - Se `SIGNED`: Exibe o cartão verde de **Contrato Assinado com Validade Forense**, dados bancários validados pelo BACEN e botão para baixar o **Certificado Pericial & Contrato em PDF** (`/api/public/signatures/{token}/pdf`).
     - Se `REJECTED_DIVERGENT_DOCUMENT` (ou caso o cliente clique em "Não tenho PIX no meu CPF"): Exibe o alerta de bloqueio anti-fraude por divergência de titularidade e permite escolher uma das **3 Rotas Oficiais de Fallback** (`GOV_BR`, `EMAIL_OTP`, `PHYSICAL_NOTARY`) via `POST /api/public/signatures/{token}/fallback`.

---

## 2. Próxima Decisão — Frente 2: Dashboard Autenticado do Assinante (`frontend-customer`) & Documentação
Definir como apresentar os dados de Contrato, PDF/NFCom das faturas e Upgrade de Plano no painel logado do assinante.
