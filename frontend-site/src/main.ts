import { config } from './config';
import { getSiteSettings, getPrimaryCompany, type SiteSettings, type Company } from './services/siteService';
import { getActivePlans, type PlanItem } from './services/planService';

/**
 * Formata um valor decimal em moeda brasileira (R$ 99,90)
 */
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'decimal',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

/**
 * Atualiza as variáveis CSS no :root com base nas cores configuradas no ERP
 */
function applyThemeColors(settings: SiteSettings | null) {
  const root = document.documentElement;

  const primary = settings?.primaryColor || config.defaultPrimaryColor;
  const secondary = settings?.secondaryColor || config.defaultSecondaryColor;

  root.style.setProperty('--primary-color', primary);
  root.style.setProperty('--secondary-color', secondary);

  // Calcula cores de hover ligeiramente ajustadas
  root.style.setProperty('--primary-hover', primary);
  root.style.setProperty('--secondary-hover', secondary);
}

/**
 * Vincula a URL da Área do Cliente nos botões do Header e do Footer
 */
function setupCustomerAreaLinks() {
  const headerBtn = document.getElementById('btn-client-area-header') as HTMLAnchorElement | null;
  const footerLink = document.getElementById('footer-client-area-link') as HTMLAnchorElement | null;

  if (headerBtn) {
    headerBtn.href = config.customerAreaUrl;
  }
  if (footerLink) {
    footerLink.href = config.customerAreaUrl;
  }
}

/**
 * Preenche informações institucionais da empresa na página
 */
function renderCompanyInfo(company: Company | null, settings: SiteSettings | null) {
  const companyName = company?.name || settings?.siteTitle || config.defaultTitle;
  const description = settings?.siteDescription || config.defaultDescription;

  document.title = `${companyName} • Internet Fibra Óptica`;

  // Logos e Nomes de Marca
  const brandName = document.getElementById('brand-name');
  if (brandName) brandName.textContent = companyName;

  const footerBrand = document.getElementById('footer-brand-name');
  if (footerBrand) footerBrand.textContent = companyName;

  const footerDesc = document.getElementById('footer-description');
  if (footerDesc) footerDesc.textContent = description;

  // Informações de Contato
  const phone = company?.phone || '(00) 0000-0000';
  const email = company?.email || 'contato@provedor.com.br';
  const documentNumber = company?.document ? `CNPJ: ${company.document}` : '';
  const address = company?.address || 'Consulte nossas unidades físicas';

  const cleanPhone = phone.replace(/\D/g, '');
  const whatsappUrl = `https://wa.me/55${cleanPhone}?text=${encodeURIComponent(`Olá! Gostaria de informações sobre os planos de internet da ${companyName}.`)}`;

  // Botões de WhatsApp
  const btnHeroWa = document.getElementById('btn-whatsapp-hero') as HTMLAnchorElement | null;
  if (btnHeroWa) btnHeroWa.href = whatsappUrl;

  const btnContactWa = document.getElementById('btn-whatsapp-contact') as HTMLAnchorElement | null;
  if (btnContactWa) btnContactWa.href = whatsappUrl;

  const btnPhone = document.getElementById('btn-phone-contact') as HTMLAnchorElement | null;
  if (btnPhone) btnPhone.href = `tel:${cleanPhone}`;

  const contactPhoneText = document.getElementById('contact-phone-text');
  if (contactPhoneText) contactPhoneText.textContent = phone;

  const contactAddressText = document.getElementById('contact-address-text');
  if (contactAddressText) contactAddressText.textContent = address;

  // Rodapé
  const footerSac = document.getElementById('footer-sac');
  if (footerSac) footerSac.textContent = `SAC: ${phone}`;

  const footerEmail = document.getElementById('footer-email');
  if (footerEmail) footerEmail.textContent = `Email: ${email}`;

  const footerLegal = document.getElementById('footer-company-legal');
  if (footerLegal) {
    footerLegal.textContent = `${companyName} ${documentNumber ? `• ${documentNumber}` : ''}`;
  }
}

/**
 * Renderiza dinamicamente os cards de planos ativos vindos do backend
 */
function renderPlans(plans: PlanItem[], company: Company | null) {
  const container = document.getElementById('plans-grid');
  if (!container) return;

  if (plans.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <h3>Nenhum plano disponível no momento</h3>
        <p>Entre em contato com nossa equipe comercial para consultar ofertas sob medida.</p>
      </div>
    `;
    return;
  }

  // Identifica o plano intermediário para destacar como "Mais Popular"
  const featuredIndex = plans.length >= 3 ? Math.floor(plans.length / 2) : 0;

  const cleanPhone = (company?.phone || '').replace(/\D/g, '');
  const companyName = company?.name || 'Provedor';

  container.innerHTML = plans
    .map((plan, index) => {
      const isFeatured = index === featuredIndex;
      const speedMbps = plan.downloadSpeed;
      const uploadMbps = plan.uploadSpeed;
      const priceFormatted = formatCurrency(plan.price);

      const contractMessage = encodeURIComponent(
        `Olá! Tenho interesse no plano ${plan.name} (${speedMbps} Mega por R$ ${priceFormatted}/mês) da ${companyName}. Poderiam verificar a disponibilidade para o meu endereço?`
      );
      const subscribeUrl = cleanPhone 
        ? `https://wa.me/55${cleanPhone}?text=${contractMessage}` 
        : '#contato';

      return `
        <article class="plan-card ${isFeatured ? 'featured' : ''}" id="plan-${plan.id}">
          ${isFeatured ? '<span class="featured-tag">Mais Escolhido</span>' : ''}
          
          <div class="plan-header">
            <h3 class="plan-name">${plan.name}</h3>
            ${plan.description ? `<p class="section-subtitle" style="font-size: 0.85rem;">${plan.description}</p>` : ''}
            
            <div class="plan-speed-wrapper">
              <span class="plan-speed-number">${speedMbps}</span>
              <span class="plan-speed-unit">MEGA</span>
            </div>
            <div class="plan-upload">Upload até ${uploadMbps} Mbps • 100% Fibra</div>
          </div>

          <div class="plan-price-wrapper">
            <span class="plan-price-label">Mensalidade</span>
            <div class="plan-price-value">
              <span class="plan-currency">R$</span>
              <span class="plan-amount">${priceFormatted}</span>
              <span class="plan-period">/mês</span>
            </div>
          </div>

          <ul class="plan-features">
            <li class="plan-feature-item">
              <span class="plan-feature-icon">✓</span>
              <span>Instalação Grátis com Roteador Wi-Fi</span>
            </li>
            <li class="plan-feature-item">
              <span class="plan-feature-icon">✓</span>
              <span>Ultraestabilidade para Home Office</span>
            </li>
            <li class="plan-feature-item">
              <span class="plan-feature-icon">✓</span>
              <span>Suporte Técnico Prioritário Local</span>
            </li>
            ${plan.svaIncluded ? `
            <li class="plan-feature-item">
              <span class="plan-feature-icon">★</span>
              <span>Benefício Incluso: ${plan.svaIncluded}</span>
            </li>
            ` : ''}
          </ul>

          <a href="${subscribeUrl}" target="_blank" rel="noopener noreferrer" class="btn ${isFeatured ? 'btn-primary' : 'btn-secondary'} plan-cta">
            Contratar Agora
          </a>
        </article>
      `;
    })
    .join('');
}

/**
 * Inicialização principal do site institucional
 */
async function bootstrap() {
  setupCustomerAreaLinks();

  // 1. Busca configurações e dados institucionais em paralelo
  const [settings, company] = await Promise.all([
    getSiteSettings(),
    getPrimaryCompany(),
  ]);

  // 2. Aplica paleta de cores no :root e preenche dados cadastrais
  applyThemeColors(settings);
  renderCompanyInfo(company, settings);

  // 3. Busca catálogo de planos ativos
  const plans = await getActivePlans();
  renderPlans(plans, company);
}

// Inicializa quando o DOM estiver pronto
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', bootstrap);
} else {
  bootstrap();
}
