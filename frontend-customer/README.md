# ispERP • Central do Assinante (`frontend-customer`)

Este diretório contém a **Central do Assinante (Área do Cliente)** do provedor de internet atendido pelo **ispERP**.

Projetada especificamente para ser **mobile-first**, **ultra-leve (< 30 kB)**, rápida em redes móveis (4G/5G) e instalável como **PWA** (adicionar à tela inicial do celular como aplicativo nativo).

---

## 🚀 Guia Rápido de Comandos

```bash
# Instalar dependências
npm install

# Iniciar servidor local (porta 3001)
npm run dev

# Executar testes unitários automatizados
npm test

# Checagem estrita de tipos TypeScript
npm run typecheck

# Gerar build otimizado para produção (dist/)
npm run build

# Pré-visualizar o build de produção localmente
npm run preview
```

---

## 🏛️ Arquitetura e Funcionalidades

1. **Autenticação Descomplicada**:
   * Login por CPF ou CNPJ com máscara dinâmica.
   * Suporte a PIN de segurança de 4 dígitos numéricos.
2. **Faturas & Pagamento Pix**:
   * Listagem de faturas abertas (vencidas e pendentes) e faturas quitadas.
   * Botão **"Pagar com Pix"** com exibição de QR Code e botão de cópia com 1 clique.
3. **Desbloqueio em Confiança (48h)**:
   * Liberação imediata temporária para assinantes suspensos por pendência financeira com 1 clique e registro no ERP.
4. **Identidade Visual Dinâmica**:
   * Consome as cores e a logomarca do provedor cadastradas no backend via API em tempo real.

---

## 📄 Licença

Este componente (`frontend-customer`) é distribuído sob a licença permissiva **[MIT](LICENSE)**, permitindo empacotamento white-label e publicação nas lojas oficiais (Apple App Store e Google Play) sem restrições.
