# Brainstorming Onda G — Portal Web do Assinante (`frontend-customer`): Página Pública de Assinatura Eletrônica (`/sign/{token}`) e Visão Completa do Contrato/NFCom
**Versão:** v1 (Planejamento Inicial e Mapeamento dos Endpoints do Portal Web)
**Data:** 2026-10-14

## 1. Contexto e Objetivo
Após a auditoria de 100% da documentação (`docs/`) frente ao código implementado, identificamos que o Backend (`backend/`), os Contratos (`contracts/openapi/`), o Site Vitrine (`frontend-site/`) e o App Operacional (`frontend-flutter/`) estão 100% implementados.

Restam no **Portal Web do Assinante (`frontend-customer/`, em Vanilla TypeScript + Vite 6)** duas entregas documentadas em `docs/ASSINATURA_ELETRONICA_PIX_LEGAL.md` e `docs/SDD.md`:
1. **Rota Pública de Assinatura Eletrônica pelo Cliente (`/sign/{token}` ou `?sign={token}`)** sem exigir login prévio na Central, permitindo ao cliente ler o contrato, aceitar a cláusula de consentimento, pagar o PIX Simbólico de R$ 1,00 (`pay.xingubit.com.br`), acompanhar o status (ou selecionar Fallback MP 2.200-2 em caso de divergência de CPF/ausência de PIX) e baixar o Certificado Pericial Forense em PDF (`/api/public/signatures/{token}/pdf`).
2. **Completude do Dashboard Autenticado do Assinante (`frontend-customer`)**:
   - Exibição dos dados do Contrato (`contract.id`, `contract.status`) e botões de download do PDF/NFCom nas faturas (`inv.pdfUrl`) + seção de Planos Disponíveis para Upgrade (`availableUpgradePlans`), além da sincronização dos checkboxes de `docs/IMPLEMENTATION_PLAN.md`.

---

## 2. Mapeamento dos Endpoints Públicos de Assinatura (`contracts/openapi/domains/contracts/signatures.yaml`)
- **`GET /api/public/signatures/{token}`** (`SignaturePublicViewResponse`):
  - Retorna `token`, `contractName`, `customerName`, `customerDocumentMasked`, `companyName`, `renderedContent`, `consentClause`, `status` (`PENDING`, `SIGNED`, `REJECTED_DIVERGENT_DOCUMENT`, `EXPIRED`, `CANCELED`), `symbolicAmount`, `pixCopyPaste`, `pixQrCodeBase64`, `payerName`, `payerBankName`, `rejectionReason`, `signedPdfUrl`, `documentSha256Hash`, `fallbackMethod`, `onboardingCreditAmount`, `forensicCertificatePdfUrl`, `expiresAt`, `signedAt`.
- **`GET /api/public/signatures/{token}/status`** (`SignatureSessionResponse`):
  - Retorna o status atualizado da sessão para polling/sincronização em tempo real.
- **`POST /api/public/signatures/{token}/fallback`**:
  - Permite selecionar `fallbackMethod` (`GOV_BR`, `EMAIL_OTP`, `PHYSICAL_NOTARY`) e `justification`.
- **`GET /api/public/signatures/{token}/pdf`**:
  - Download direto do Contrato + Certificado de Autenticidade Forense em PDF.
