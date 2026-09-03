# Especificação Técnica Oficial: Assinatura Eletrônica Baseada em Autenticação Pix

> **Fundamentação Jurídica:** Medida Provisória nº 2.200-2/2001 (Art. 10, § 2º) e Lei Federal nº 14.063/2020 (Art. 4º, II - Assinatura Eletrônica Avançada).  
> **Status:** Implementado, Validado com Testcontainers (PostgreSQL 17) e em Produção (Milestone 32).

---

## 1. Visão Geral & Propósito

O módulo de Assinatura Eletrônica via Pix do ispERP substitui plataformas externas caras (Clicksign, DocuSign, ZapSign) por um mecanismo nativo de autenticação bancária instantânea de valor simbólico (R$ 1,00).

### Os 3 Pilares de Blindagem:
1. **Autenticação Biométrica e KYC Nível 3**: O signatário utiliza a conta corrente pessoal que já passou por checagem documental e biometria no Banco Central.
2. **Bloqueio Inflexível de Divergência de CPF**: Se o Pix for emitido de uma conta de terceiro (cônjuge, parente, amigo), o ERP rejeita a assinatura, impedindo que colaboradores cadastrem contratos fraudulentos em nome de terceiros.
3. **Custo Zero Real para o Cliente**: O valor de R$ 1,00 pago na autenticação é abatido automaticamente como desconto na primeira mensalidade do contrato.

---

## 2. Fluxo Operacional Ponta a Ponta

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Assinante (Smartphone)
    participant ERP_Front as Web Assinante (/sign/:token)
    participant ERP_Back as Backend (Spring Boot 4)
    participant SPI as SPI / Banco Central (BACEN)
    participant ERP_DB as PostgreSQL 17

    Cliente->>ERP_Front: Acessa link seguro (/sign/{token})
    ERP_Front->>ERP_Back: GET /api/public/signatures/{token}
    ERP_Back-->>ERP_Front: Retorna contrato + Termo de Consentimento + QR Code Pix R$ 1,00
    Cliente->>Cliente: Marca aceite da cláusula de consentimento
    Cliente->>SPI: Efetua pagamento de R$ 1,00 no App Bancário
    SPI->>ERP_Back: Webhook Pix com EndToEndId, CPF Pagador e Banco
    
    alt CPF Pagador == CPF Titular
        ERP_Back->>ERP_DB: Status -> SIGNED
        ERP_Back->>ERP_DB: Aplica R$ 1,00 de desconto na primeira fatura PENDING
        ERP_Back->>ERP_DB: Carimba PDF com Folha Pericial e Hash SHA-256
        ERP_Back->>ERP_DB: Dispara evento CONTRACT_SIGNED (Zero-Touch Onboarding)
        ERP_Back-->>SPI: 200 OK
        ERP_Front->>ERP_Front: Exibe confirmação de sucesso + Download do Contrato Pericial
    else CPF Divergente
        ERP_Back->>ERP_DB: Status -> REJECTED_DIVERGENT_DOCUMENT
        ERP_Back-->>SPI: 200 OK
        ERP_Front->>ERP_Front: Exibe motivo da rejeição e oferece 3 Fallbacks (Gov.br, E-mail OTP, Cartório)
    end
```

---

## 3. Folha Pericial Forense (Certificado de Autenticidade Digital)

Todo contrato assinado recebe uma folha anexa que consolida as evidências probatórias aceitas pela jurisprudência brasileira:
- **Identificador E2E do BACEN**: Código irrevogável da liquidação no Sistema de Pagamentos Instantâneos.
- **Hash SHA-256**: Prova matemática de que nenhuma vírgula do contrato foi alterada após o pagamento.
- **Identificação da Instituição Bancária**: Nome do banco e código ISPB.
- **Dados de Conexão**: Endereço IP do cliente, Navegador / User-Agent e carimbo de data/hora no fuso de Brasília.
- **Declaração de Consentimento Expresso**: Enquadramento formal nos diplomas da MP 2.200-2/01 e Lei 14.063/2020.

---

## 4. Rotas Oficiais de Fallback

Caso o titular não disponha de conta bancária com chave Pix ativa em seu próprio CPF, o ERP permite selecionar formalmente:
1. **Gov.br**: Assinatura digital via autenticação única do Governo Federal (nível Prata ou Ouro).
2. **E-mail OTP**: Envio de link protegido por código token temporário no e-mail do titular.
3. **Presencial / Cartório**: Emissão de minuta impressa para assinatura física na recepção do provedor ou firma reconhecida em cartório.
