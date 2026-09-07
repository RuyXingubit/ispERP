# ispERP • Site Institucional do Provedor (`frontend-site`)

Este diretório contém o **Site Institucional Público (WWW)** do provedor de internet atendido pelo **ispERP**.

Ele foi projetado especificamente para ser **ultra-leve (< 25 kB)**, de **carregamento instantâneo** (< 0.5s), com nota máxima no **Google PageSpeed / Core Web Vitals** e **100% dinâmico** em relação ao ERP central.

---

## 🚀 Guia Rápido de Comandos

```bash
# Instalar dependências
npm install

# Iniciar servidor de desenvolvimento local
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

## 📖 Documentação Completa para Desenvolvedores e IAs

Para desenvolvedores web, agências de marketing ou assistentes de Inteligência Artificial (Claude, Cursor, ChatGPT, Gemini, Copilot) que forem personalizar ou evoluir este site, consulte o guia oficial:

👉 **[GUIA COMPLETO DE CUSTOMIZAÇÃO E INSTRUÇÕES PARA IA (AI_AND_DEV_GUIDE.md)](./AI_AND_DEV_GUIDE.md)**

---

## 🏛️ Arquitetura e Decisões de Design

1. **Desacoplamento Triplo**:
   * `frontend-flutter/`: ERP Operacional interno (dashboard, NOC, faturamento, técnicos).
   * `frontend-site/` (este projeto): Vitrine institucional pública, personalizável pelo provedor à vontade.
   * `frontend-customer/`: Central do Assinante / Área do Cliente segura (faturas Pix, auto-desbloqueio).
2. **Zero Frameworks Pesados**:
   * Construído com **HTML5 Semântico**, **CSS3 Puro (Vanilla)** e **TypeScript** empacotado pelo **Vite**.
   * Sem React, sem Next.js e sem dependências pesadas na vitrine pública.
3. **Consumo 100% Dinâmico do Backend**:
   * O site não contém planos hardcoded. Ele consome o catálogo oficial de planos ativos, os dados cadastrais da empresa e a paleta de cores configurada no ERP em tempo real.

---

## 📄 Licença

Este componente (`frontend-site`) é distribuído sob a licença permissiva **[MIT](LICENSE)**, garantindo total liberdade para o provedor, designers e agências de marketing personalizarem layout, marca, estilos e componentes sem restrições de copyleft.
