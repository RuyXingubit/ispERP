# Guia Técnico de Customização e Diretrizes para IAs & Desenvolvedores Web

> **Público-Alvo**: Este documento é destinado a Desenvolvedores Web, Designers, Agências e Agentes de Inteligência Artificial (Claude, Cursor, Copilot, ChatGPT, Antigravity) encarregados de customizar, redesenhar ou expandir o **`frontend-site/`**.

---

## 1. Princípios Arquiteturais Obrigatórios

1. **Ultra-Performance e Core Web Vitals**:
   * O site foi projetado para carregar em menos de 0.5s em redes 4G móveis e pesar menos de 25 kB no total.
   * **NÃO instale frameworks pesados** (como React, Vue, Angular ou bibliotecas de UI inchadas).
   * Mantenha a base em **HTML5 Semântico, CSS3 Modular (Vanilla) e TypeScript puro**.
2. **Zero Dados Fictícios / Zero Hardcoding de Planos**:
   * O site **nunca** deve conter planos de internet escritos estaticamente no HTML.
   * Todos os planos de fibra vigentes vêm da API do ispERP (`GET /api/plans/active`).
   * Se o provedor alterar o valor de um plano, a velocidade ou criar uma nova oferta no ERP, o site se atualiza automaticamente.
3. **Identidade Visual Dinâmica Controlada pelo ERP**:
   * O administrador do provedor pode alterar a paleta de cores (cor primária e secundária) diretamente no painel do ispERP.
   * Em runtime, o script [src/main.ts](file:///Users/ruy/Code/ispERP/frontend-site/src/main.ts) sobrescreve as CSS Custom Properties no `:root`:
     * `--primary-color`
     * `--secondary-color`
     * `--primary-hover`
     * `--secondary-hover`
   * **Regra**: Qualquer nova seção ou componente criado deve utilizar `var(--primary-color)` ou `var(--secondary-color)` em vez de cores hexadecimais fixas para botões e destaques.

---

## 2. Mapa Obrigatório de Elementos e Seletores do DOM

Se você (IA ou desenvolvedor) for alterar a estrutura do [index.html](file:///Users/ruy/Code/ispERP/frontend-site/index.html), **você DEVE preservar os seguintes IDs** (ou atualizar simultaneamente os seletores em [src/main.ts](file:///Users/ruy/Code/ispERP/frontend-site/src/main.ts)):

| ID do Elemento HTML | Tipo Esperado | Função / O que o script injeta |
| :--- | :--- | :--- |
| `plans-grid` | Container (`<div>`, `<section>`) | Onde os cards de planos ativos são renderizados via loop. |
| `btn-client-area-header` | Link (`<a>`) | Botão de destaque do cabeçalho que leva à Área do Cliente. |
| `footer-client-area-link` | Link (`<a>`) | Link de rodapé que leva à Área do Cliente. |
| `brand-name` | Texto (`<span>`, `<h1>`, etc.) | Nome fantasia ou razão social da empresa no Header. |
| `footer-brand-name` | Texto (`<span>`, `<h3>`, etc.) | Nome da empresa no Rodapé. |
| `footer-description` | Texto (`<p>`) | Descrição institucional da empresa no Rodapé. |
| `btn-whatsapp-hero` | Link (`<a>`) | Botão de WhatsApp principal com mensagem comercial pronta. |
| `btn-whatsapp-contact` | Link (`<a>`) | Botão de WhatsApp na seção de atendimento. |
| `btn-phone-contact` | Link (`<a>`) | Link `tel:` com o número do SAC para discagem direta. |
| `contact-phone-text` | Texto (`<span>`, `<p>`) | Exibição textual formatada do telefone do SAC. |
| `contact-address-text` | Texto (`<span>`, `<p>`) | Exibição do endereço físico da sede do provedor. |
| `footer-sac` | Texto (`<span>`) | Telefone do SAC no rodapé. |
| `footer-email` | Texto (`<span>`) | E-mail de suporte no rodapé. |
| `footer-company-legal` | Texto (`<span>`) | Razão Social e CNPJ oficial no rodapé legal. |

---

## 3. Contrato das APIs do Backend (ispERP)

O backend do ispERP expõe três rotas públicas de leitura (`GET`) que não exigem autenticação JWT:

### 3.1. `GET /api/site-settings`
Retorna as preferências visuais e institucionais cadastradas no ERP:
```json
{
  "siteTitle": "Nexus Fibra",
  "siteDescription": "Provedor de internet fibra óptica de alta estabilidade.",
  "primaryColor": "#1976d2",
  "secondaryColor": "#dc004e",
  "logoUrl": "https://provedor.com.br/logo.png"
}
```

### 3.2. `GET /api/companies/primary`
Retorna os dados legais e fiscais da empresa provedora:
```json
{
  "id": 1,
  "name": "Nexus Fibra Telecomunicações Ltda.",
  "tradeName": "Nexus Fibra",
  "document": "28.451.983/0001-44",
  "phone": "(93) 3515-2000",
  "email": "contato@nexusfibra.com.br",
  "address": "Av. Brasil, 1500, Centro, Altamira - PA"
}
```

### 3.3. `GET /api/plans/active`
Retorna a lista de planos de fibra ativos para venda:
```json
[
  {
    "id": 1,
    "name": "Fibra 300 Mega Residencial",
    "description": "Ideal para streaming, home office e navegação diária.",
    "downloadSpeed": 300,
    "uploadSpeed": 150,
    "price": 79.90,
    "svaIncluded": "Aplicativo de Filmes e Séries"
  },
  {
    "id": 2,
    "name": "Fibra 600 Mega Ultra Gamer",
    "description": "Latência ultra-baixa com velocidade simétrica para jogos.",
    "downloadSpeed": 600,
    "uploadSpeed": 300,
    "price": 99.90
  }
]
```

---

## 4. Como Fazer Customizações Comuns (Receitas Práticas)

### 4.1. Como Trocar a Logo do Provedor
1. Se for usar arquivo estático:
   - Adicione o arquivo em [public/logo.svg](file:///Users/ruy/Code/ispERP/frontend-site/public/) ou `public/logo.png`.
   - No [index.html](file:///Users/ruy/Code/ispERP/frontend-site/index.html), no bloco `.logo-wrapper`, substitua o ícone svg ou emoji pela tag:
     ```html
     <img src="/logo.svg" alt="Logo Provedor" class="brand-logo-img">
     ```
2. Se o provedor cadastrar a URL da logo no ERP:
   - Em [src/main.ts](file:///Users/ruy/Code/ispERP/frontend-site/src/main.ts), acesse `settings.logoUrl` e injete o atributo `src` da imagem.

### 4.2. Como Adicionar uma Seção Nova (Ex: FAQ / Perguntas Frequentes)
1. No [index.html](file:///Users/ruy/Code/ispERP/frontend-site/index.html), crie a seção dentro de `<main>` usando a estrutura de classes padrão:
   ```html
    <section class="section" id="faq">
      <div class="container">
        <div class="section-title-wrapper">
          <h2 class="section-title">Perguntas Frequentes</h2>
        </div>
       <div class="faq-accordion">
         <details class="faq-item">
           <summary class="faq-question">Qual é o prazo de instalação?</summary>
           <p class="faq-answer">Nossas instalações ocorrem em até 48 horas úteis após a confirmação do pedido.</p>
         </details>
       </div>
     </div>
   </section>
   ```
2. Adicione os estilos em [src/styles/components.css](file:///Users/ruy/Code/ispERP/frontend-site/src/styles/components.css). Use `var(--primary-color)` para o hover e bordas ativas.

### 4.3. Como Customizar a Tipografia (Google Fonts)
1. Altere o `<link>` do Google Fonts em [index.html](file:///Users/ruy/Code/ispERP/frontend-site/index.html) (ex: Montserrat, Plus Jakarta Sans, Outfit).
2. Em [src/styles/variables.css](file:///Users/ruy/Code/ispERP/frontend-site/src/styles/variables.css), atualize:
   ```css
   :root {
     --font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
   }
   ```

### 4.4. Como Configurar a URL da Área do Cliente
Crie ou edite o arquivo `.env` na raiz de `frontend-site/`:
```env
VITE_CUSTOMER_PORTAL_URL=https://central.meuprovedor.com.br
```
Se omitido, o site usa o fallback padrão para desenvolvimento local (`http://localhost:3001` ou subcaminho configurado em [src/config.ts](file:///Users/ruy/Code/ispERP/frontend-site/src/config.ts)).

---

## 5. Regras de Ouro para Agentes de IA (Checklist de Validação)

Antes de concluir qualquer tarefa ou modificação neste projeto, **a IA DEVE seguir rigorosamente este checklist**:

- [ ] **Não quebrar os IDs do DOM**: Verifique se `#plans-grid`, `#btn-client-area-header` e os seletores da tabela da Seção 2 ainda existem.
- [ ] **Prevenção de Estouro de Layout (Anti-Overflow)**: Verifique se nenhuma imagem ou contêiner tem largura fixa em pixels que possa quebrar a visualização em smartphones (`overflow-x: hidden` deve ser respeitado).
- [ ] **Manter Fallbacks Neutros**: Se o backend estiver indisponível, a página NÃO deve exibir erro com tela em branco. As funções em [src/services/api.ts](file:///Users/ruy/Code/ispERP/frontend-site/src/services/api.ts) já tratam exceções e retornam `null` ou `[]`. Mantenha essa resiliência.
- [ ] **Executar a Bateria de Testes e Tipos**:
  ```bash
  npm test
  npm run typecheck
  npm run build
  ```
  O commit ou entrega só é válido se todos os 3 comandos acima passarem com código de saída 0.
