# Xingubit Pay API - Guia de Integração Oficial (ispERP)

> **Base URL:** `https://pay.xingubit.com.br`  
> **Versão da API:** `v1 (1.23.0)`  
> **Documentação Oficial:** `https://pay.xingubit.com.br/doc` e `/xingubit-pay-api-llms.md`

O **Xingubit Pay** atua como infraestrutura unificada de **Pagamentos Pix (COB/COBV)**, **Motor Fiscal Eletrônico (NFCom, NFS-e, NF-e)**, **Auto-Cashout** e **GeoCEP**.

---

## 1. Autenticação (OAuth 2.0)
- `POST /v1/oauth/token`
- Header: `Authorization: Bearer <access_token>` (Validade: 1 hora / 3600s).

---

## 2. Gestão do Certificado Digital A1 & Configurações Fiscais
Para emitir NFCom (Modelo 62), NFS-e e NF-e através do Xingubit Pay:

1. **Upload do Certificado Digital A1 (`.pfx`):**
   - `POST /v1/merchants/me/certificate` (Multipart form com arquivo `.pfx` e `password`).
   - O gateway armazena o certificado no cofre HSM/KMS na nuvem e executa a assinatura digital XMLDSig e transmissão SEFAZ/Prefeitura.
2. **Configuração Fiscal da Empresa (CNPJ):**
   - `POST /v1/empresas` (Cadastro de dados fiscais da empresa/filial).
   - `PUT /v1/empresas/{cnpj}/config/nfcom` (Ambiente, série, lote e número inicial de NFCom).
   - `PUT /v1/empresas/{cnpj}/config/nfse` (Ambiente, série, lote e número inicial de NFS-e).
   - `PUT /v1/empresas/{cnpj}/config/nfe` (Ambiente, série, lote e número inicial de NF-e).

---

## 3. Emissão de NFCom (Modelo 62) e Documentos Fiscais
A emissão pode ocorrer de duas formas:

### 3.1. Emissão Automática Vinculada ao Pix CobV
Ao criar a cobrança mensal do assinante com `POST /v1/charges`:
```json
{
  "paymentMethod": "PIX",
  "description": "Mensalidade Fibra Óptica",
  "amount": { "original": 99.90 },
  "calendar": { "dueDate": "2026-09-10" },
  "payer": {
    "name": "Maria Silva",
    "document": "52998224725",
    "address": { "street": "Av Tancredo Neves", "city": "Altamira", "state": "PA", "zipCode": "68370000" }
  },
  "invoice": true,
  "invoiceType": "NFCOM",
  "metadata": { "externalContractId": "CTR-001" }
}
```
- A NFCom é processada e vinculada à cobrança.
- Download do DANFE em PDF: `GET /v1/charges/{id}/invoice/pdf` ou `GET /v1/invoices/nfcom/{id}/pdf`.
- Download do XML protocolado: `GET /v1/invoices/nfcom/{id}/xml`.

### 3.2. Emissão Avulsa por API
- `POST /v1/invoices/nfcom`
- `GET /v1/invoices/nfcom/{id}`
- `POST /v1/invoices/nfcom/{id}/cancelamento`

---

## 4. Cobranças Pix (COB e COBV) & Carnês Parcelados
- **Pix Imediato (COB):** `POST /v1/charges` (sem `calendar.dueDate`).
- **Pix com Vencimento (COBV):** `POST /v1/charges` com juros (`interest`), multa (`fine`), desconto (`discount`) e vencimento (`calendar.dueDate`).
- **Carnê Pix:** `POST /v1/charges` com `installments: 2 a 24`.

---

## 5. Webhooks de Pagamento
O gateway envia `POST https://seu-isperp/api/webhooks/payments/pix`:
```json
{
  "status": "PAID",
  "txId": "01915f203b997221a476880628203c94",
  "e2eId": "E904008882026082212000001",
  "amount": 99.90,
  "paidAt": "2026-08-29T15:14:39.120Z",
  "externalContractId": "CTR-001",
  "metadata": { "externalContractId": "CTR-001" }
}
```

---

## 6. Cashout & Auto-Cashout
- `POST /v1/cashouts` (Saque via Pix para conta do provedor).
- `PUT /v1/cashouts/config` (Configuração de sweep automático diário às 18h ou instantâneo).
