// SPDX-License-Identifier: MIT
import { config } from './config';
import { loginClient, getCurrentSession, logoutClient } from './services/authService';
import {
  fetchDashboard,
  requestTrustUnblock,
  fetchThemeSettings,
  fetchCompanyData,
  setCustomerPin,
  type ClientPortalDashboard,
  type InvoiceItem,
} from './services/portalService';
import {
  fetchPublicSignatureView,
  fetchPublicSignatureStatus,
  selectPublicSignatureFallback,
  getSignedContractPdfUrl,
  type SignaturePublicView,
  type FallbackMethodType,
} from './services/signatureService';

// Estado da Aplicação
let currentDashboard: ClientPortalDashboard | null = null;
let activeTab: 'pending' | 'paid' = 'pending';
let pendingCustomerIdForPin: string | null = null;
let isForceChangeMode: boolean = false;
let currentSignatureView: SignaturePublicView | null = null;
let signaturePollingTimer: ReturnType<typeof setInterval> | null = null;

import {
  formatDocument,
  formatCurrency,
  formatDate,
  validatePin,
  extractSignatureToken,
  formatContractStatus,
  formatFallbackMethodLabel,
} from './utils/formatters';
export {
  formatDocument,
  formatCurrency,
  formatDate,
  validatePin,
  extractSignatureToken,
  formatContractStatus,
  formatFallbackMethodLabel,
};

/**
 * Aplica paleta de cores no :root
 */
function applyTheme(primary?: string, secondary?: string) {
  const root = document.documentElement;
  const p = primary || config.defaultPrimaryColor;
  const s = secondary || config.defaultSecondaryColor;
  root.style.setProperty('--primary-color', p);
  root.style.setProperty('--secondary-color', s);
}

/**
 * Inicialização e carregamento de identidade visual
 */
async function loadBranding() {
  const [theme, company] = await Promise.all([
    fetchThemeSettings(),
    fetchCompanyData(),
  ]);

  if (theme) {
    applyTheme(theme.primaryColor, theme.secondaryColor);
  }

  const companyName = company?.tradeName || company?.name || config.defaultTitle;
  const brandTitle = document.getElementById('brand-title');
  if (brandTitle) brandTitle.textContent = companyName;

  const loginBrand = document.getElementById('login-brand-name');
  if (loginBrand) loginBrand.textContent = companyName;

  document.title = `${companyName} • Central do Assinante`;
}

/**
 * Exibe alertas na tela de login
 */
function showLoginAlert(message: string, isError: boolean = true) {
  const alert = document.getElementById('login-alert');
  if (!alert) return;
  alert.textContent = message;
  alert.className = `alert-message ${isError ? 'alert-error' : 'alert-success'}`;
  alert.style.display = 'block';
}

function hideLoginAlert() {
  const alert = document.getElementById('login-alert');
  if (alert) alert.style.display = 'none';
}

/**
 * Renderiza os planos disponíveis para upgrade quando retornados pela API
 */
function renderUpgradePlans() {
  const section = document.getElementById('upgrade-plans-section');
  const grid = document.getElementById('upgrade-plans-grid');
  if (!section || !grid || !currentDashboard) return;

  const plans = currentDashboard.availableUpgradePlans || [];
  if (plans.length === 0) {
    section.style.display = 'none';
    grid.innerHTML = '';
    return;
  }

  section.style.display = 'block';
  grid.innerHTML = plans
    .map(
      (plan) => `
      <div class="upgrade-plan-card">
        <div>
          <div style="font-weight: 800; font-size: 0.98rem; color: var(--text-main);">${plan.name}</div>
          <div class="upgrade-plan-speeds">
            ⬇ ${plan.downloadSpeed} Mbps • ⬆ ${plan.uploadSpeed} Mbps
          </div>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: baseline; gap: 8px; flex-wrap: wrap;">
          <span class="upgrade-plan-price">R$ ${formatCurrency(plan.price ?? plan.monthlyPrice ?? 0)}<small style="font-size: 0.72rem; color: var(--text-muted); font-weight: 500;">/mês</small></span>
          <span style="font-size: 0.75rem; color: var(--success-color); font-weight: 700;">Disponível</span>
        </div>
      </div>
    `
    )
    .join('');
}

/**
 * Renderiza o painel de faturas com base na aba ativa
 */
function renderInvoices() {
  const grid = document.getElementById('invoices-grid');
  if (!grid || !currentDashboard) return;

  const pending = [
    ...(currentDashboard.overdueInvoices || []),
    ...(currentDashboard.pendingInvoices || []),
  ];
  const paid = currentDashboard.paidInvoices || [];

  const list: InvoiceItem[] = activeTab === 'pending' ? pending : paid;

  if (list.length === 0) {
    grid.innerHTML = `
      <div class="empty-invoices">
        <p>${activeTab === 'pending' ? 'Você não possui faturas em aberto no momento. Parabéns!' : 'Nenhuma fatura quitada encontrada no histórico.'}</p>
      </div>
    `;
    return;
  }

  grid.innerHTML = list.map((inv) => {
    const isOverdue = inv.status === 'OVERDUE';
    const isPaid = inv.status === 'PAID';
    const tagClass = isOverdue ? 'tag-overdue' : isPaid ? 'tag-paid' : 'tag-pending';
    const cardClass = isOverdue ? 'overdue' : isPaid ? 'paid' : 'pending';
    const statusLabel = isOverdue ? 'Vencida' : isPaid ? 'Paga' : 'Em Aberto';
    const formattedAmount = formatCurrency(inv.amount);
    const formattedDate = formatDate(inv.dueDate);

    const chips: string[] = [];
    if (inv.discountAmount && inv.discountAmount > 0) {
      chips.push(`<span class="invoice-breakdown-chip chip-discount">Desconto: -R$ ${formatCurrency(inv.discountAmount)}</span>`);
    }
    if (inv.penaltyAmount && inv.penaltyAmount > 0) {
      chips.push(`<span class="invoice-breakdown-chip chip-penalty">Multa: +R$ ${formatCurrency(inv.penaltyAmount)}</span>`);
    }
    if (inv.interestAmount && inv.interestAmount > 0) {
      chips.push(`<span class="invoice-breakdown-chip chip-penalty">Juros: +R$ ${formatCurrency(inv.interestAmount)}</span>`);
    }

    const pdfButtonHtml = inv.pdfUrl
      ? `<a href="${inv.pdfUrl}" target="_blank" rel="noopener noreferrer" class="btn-pdf-download">
           📄 2ª Via PDF / Nota Fiscal NFCom
         </a>`
      : '';

    return `
      <div class="invoice-card ${cardClass}" id="invoice-${inv.id}">
        <div class="invoice-top">
          <div>
            <span style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Valor</span>
            <div class="invoice-amount">R$ ${formattedAmount}</div>
            ${chips.length > 0 ? `<div class="invoice-breakdown">${chips.join('')}</div>` : ''}
          </div>
          <span class="invoice-status-tag ${tagClass}">${statusLabel}</span>
        </div>

        <div class="invoice-details">
          <div>Vencimento: <strong style="color: var(--text-main);">${formattedDate}</strong></div>
          ${inv.paidAt ? `<div>Pago em: ${formatDate(inv.paidAt.split('T')[0])}</div>` : ''}
        </div>

        <div class="invoice-actions">
          ${!isPaid ? `
            <button type="button" class="btn-pix-pay" data-invoice-id="${inv.id}">
              ⚡ Pagar com Pix
            </button>
          ` : `
            <div style="color: var(--success-color); font-size: 0.85rem; font-weight: 700; text-align: center;">
              ✓ Pagamento Confirmado
            </div>
          `}
          ${pdfButtonHtml}
        </div>
      </div>
    `;
  }).join('');

  // Adiciona listeners aos botões de Pix
  grid.querySelectorAll<HTMLButtonElement>('.btn-pix-pay').forEach((btn) => {
    btn.addEventListener('click', () => {
      const invId = btn.getAttribute('data-invoice-id');
      const invoice = list.find((i) => i.id === invId);
      if (invoice) openPixModal(invoice);
    });
  });
}

/**
 * Abre o Modal de Pagamento Pix
 */
function openPixModal(invoice: InvoiceItem) {
  const modal = document.getElementById('modal-pix');
  const qrImg = document.getElementById('pix-qr-img') as HTMLImageElement | null;
  const pixText = document.getElementById('pix-code-text');
  const copyFeedback = document.getElementById('pix-copy-feedback');

  if (!modal || !pixText) return;

  const pixCode = invoice.pixCopiaECola || `00020126580014br.gov.bcb.pix0136${invoice.id}520400005303986540${invoice.amount}5802BR5910NexusFibra6008Altamira62070503***6304`;
  pixText.textContent = pixCode;

  if (qrImg) {
    if (invoice.pixQrCodeUrl) {
      qrImg.src = invoice.pixQrCodeUrl;
    } else {
      // Fallback para renderização de QR Code rápida via API pública segura
      qrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(pixCode)}`;
    }
  }

  if (copyFeedback) copyFeedback.style.display = 'none';
  modal.style.display = 'flex';
}

function closePixModal() {
  const modal = document.getElementById('modal-pix');
  if (modal) modal.style.display = 'none';
}

/**
 * Copia o código Pix para a área de transferência
 */
async function copyPixCode() {
  const pixText = document.getElementById('pix-code-text');
  const copyFeedback = document.getElementById('pix-copy-feedback');
  if (!pixText || !pixText.textContent) return;

  try {
    await navigator.clipboard.writeText(pixText.textContent);
    if (copyFeedback) {
      copyFeedback.style.display = 'block';
      setTimeout(() => {
        if (copyFeedback) copyFeedback.style.display = 'none';
      }, 4000);
    }
  } catch {
    // Fallback manual para navegadores antigos
    const textarea = document.createElement('textarea');
    textarea.value = pixText.textContent;
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
    if (copyFeedback) copyFeedback.style.display = 'block';
  }
}

/**
 * Abre o Modal de Definição ou Alteração de PIN
 */
function openPinModal(forceChange: boolean = false) {
  const modal = document.getElementById('modal-pin');
  const title = document.getElementById('pin-modal-title');
  const desc = document.getElementById('pin-modal-desc');
  const groupCurrent = document.getElementById('group-current-pin');
  const feedback = document.getElementById('pin-feedback');
  const inputCurrent = document.getElementById('input-current-pin') as HTMLInputElement | null;
  const inputNew = document.getElementById('input-new-pin') as HTMLInputElement | null;
  const inputConfirm = document.getElementById('input-confirm-pin') as HTMLInputElement | null;

  if (!modal) return;

  isForceChangeMode = forceChange;
  if (feedback) feedback.style.display = 'none';
  if (inputCurrent) inputCurrent.value = '';
  if (inputNew) inputNew.value = '';
  if (inputConfirm) inputConfirm.value = '';

  const session = getCurrentSession();
  const hasExistingPin = !!(session?.hasPin || currentDashboard?.customer?.portalPin);

  if (forceChange) {
    if (title) title.textContent = 'Definição Obrigatória de PIN';
    if (desc) desc.textContent = 'Por segurança, você deve cadastrar um novo PIN numérico de 4 dígitos para acessar sua conta.';
    if (groupCurrent) groupCurrent.style.display = 'none';
  } else if (!hasExistingPin) {
    if (title) title.textContent = 'Cadastrar PIN de Segurança';
    if (desc) desc.textContent = 'Crie um PIN numérico de 4 dígitos para proteger e facilitar seus próximos acessos.';
    if (groupCurrent) groupCurrent.style.display = 'none';
  } else {
    if (title) title.textContent = 'Alterar PIN de Segurança';
    if (desc) desc.textContent = 'Informe seu PIN atual e o novo PIN numérico de 4 dígitos.';
    if (groupCurrent) groupCurrent.style.display = 'block';
  }

  modal.style.display = 'flex';
  if (inputNew) inputNew.focus();
}

function closePinModal() {
  if (isForceChangeMode) {
    alert('É obrigatório cadastrar seu novo PIN para prosseguir.');
    return;
  }
  const modal = document.getElementById('modal-pin');
  if (modal) modal.style.display = 'none';
}

/**
 * Submissão do formulário de PIN
 */
async function handlePinSubmit(e: Event) {
  e.preventDefault();
  const inputCurrent = document.getElementById('input-current-pin') as HTMLInputElement | null;
  const inputNew = document.getElementById('input-new-pin') as HTMLInputElement | null;
  const inputConfirm = document.getElementById('input-confirm-pin') as HTMLInputElement | null;
  const feedback = document.getElementById('pin-feedback');
  const btnSubmit = document.getElementById('btn-submit-pin') as HTMLButtonElement | null;
  const groupCurrent = document.getElementById('group-current-pin');

  const newPin = inputNew ? inputNew.value.trim() : '';
  const confirmPin = inputConfirm ? inputConfirm.value.trim() : '';
  const currentPin = (groupCurrent && groupCurrent.style.display !== 'none' && inputCurrent)
    ? inputCurrent.value.trim()
    : undefined;

  const showFeedback = (msg: string, isError: boolean = true) => {
    if (feedback) {
      feedback.textContent = msg;
      feedback.className = `login-alert ${isError ? 'login-alert-danger' : 'login-alert-success'}`;
      feedback.style.display = 'block';
    }
  };

  if (!validatePin(newPin)) {
    showFeedback('O novo PIN deve conter exatamente 4 números (ex: 1234).', true);
    return;
  }

  if (newPin !== confirmPin) {
    showFeedback('A confirmação do PIN não confere com o novo PIN digitado.', true);
    return;
  }

  const session = getCurrentSession();
  const targetCustomerId = isForceChangeMode ? pendingCustomerIdForPin : session?.customerId;

  if (!targetCustomerId) {
    showFeedback('Identificação da conta ausente. Por favor, faça login novamente.', true);
    return;
  }

  if (btnSubmit) {
    btnSubmit.disabled = true;
    btnSubmit.innerHTML = '<div class="spinner"></div> Salvando PIN...';
  }

  try {
    const res = await setCustomerPin(targetCustomerId, newPin, currentPin);
    showFeedback(res.message || 'PIN configurado com sucesso!', false);

    setTimeout(async () => {
      const wasForce = isForceChangeMode;
      isForceChangeMode = false;
      pendingCustomerIdForPin = null;
      closePinModal();

      if (wasForce) {
        showLoginAlert('PIN configurado com sucesso! Acesse sua conta.', false);
      } else {
        alert('PIN de segurança atualizado com sucesso!');
      }
    }, 1200);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Falha ao salvar o novo PIN.';
    showFeedback(msg, true);
  } finally {
    if (btnSubmit) {
      btnSubmit.disabled = false;
      btnSubmit.innerHTML = '<span id="pin-submit-text">Salvar PIN</span>';
    }
  }
}

/**
 * Carrega o Dashboard do assinante via API
 */
async function loadDashboard(customerId: string) {
  const viewLogin = document.getElementById('view-login');
  const viewDashboard = document.getElementById('view-dashboard');
  const viewSignature = document.getElementById('view-signature');
  const portalHeader = document.getElementById('portal-header');
  const userDisplayName = document.getElementById('user-display-name');
  const userDisplayDoc = document.getElementById('user-display-doc');

  if (viewLogin) viewLogin.style.display = 'none';
  if (viewSignature) viewSignature.style.display = 'none';
  if (viewDashboard) viewDashboard.style.display = 'block';
  if (portalHeader) portalHeader.style.display = 'block';

  try {
    currentDashboard = await fetchDashboard(customerId);

    // Preenche dados do assinante
    if (userDisplayName) userDisplayName.textContent = currentDashboard.customer.name;
    if (userDisplayDoc) userDisplayDoc.textContent = formatDocument(currentDashboard.customer.cpf);

    // Card de Status da Conexão
    const statusBadge = document.getElementById('status-badge');
    const statusText = document.getElementById('status-badge-text');
    const contractMeta = document.getElementById('contract-meta');
    const planName = document.getElementById('plan-name');
    const planSpeeds = document.getElementById('plan-speeds');
    const unblockWrapper = document.getElementById('trust-unblock-wrapper');

    const isBlocked = !!(currentDashboard.connectionBlocked ?? currentDashboard.isConnectionBlocked);

    if (isBlocked) {
      if (statusBadge) statusBadge.className = 'status-badge status-blocked';
      if (statusText) statusText.textContent = 'Conexão Bloqueada';
      if (unblockWrapper) {
        unblockWrapper.style.display = currentDashboard.canRequestTrustUnblock ? 'block' : 'none';
      }
    } else {
      if (statusBadge) statusBadge.className = 'status-badge status-active';
      if (statusText) statusText.textContent = 'Conexão Ativa e Estável';
      if (unblockWrapper) unblockWrapper.style.display = 'none';
    }

    if (contractMeta) {
      if (currentDashboard.contract?.id) {
        const shortId = currentDashboard.contract.id.slice(0, 8).toUpperCase();
        const label = formatContractStatus(currentDashboard.contract.status);
        contractMeta.textContent = `Contrato #${shortId} • ${label}`;
        contractMeta.style.display = 'inline-flex';
      } else {
        contractMeta.style.display = 'none';
      }
    }

    if (currentDashboard.currentPlan) {
      if (planName) planName.textContent = currentDashboard.currentPlan.name;
      if (planSpeeds) {
        planSpeeds.textContent = `${currentDashboard.currentPlan.downloadSpeed} Mbps Download • ${currentDashboard.currentPlan.uploadSpeed} Mbps Upload (100% Fibra)`;
      }
    } else {
      if (planName) planName.textContent = 'Plano de Fibra Óptica';
      if (planSpeeds) planSpeeds.textContent = 'Consulte o suporte para detalhes';
    }

    renderUpgradePlans();
    renderInvoices();
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Falha ao carregar dados da conta';
    showLoginAlert(msg, true);
    handleLogout();
  }
}

/**
 * Exibe alerta na tela pública de assinatura eletrônica
 */
function showSignatureAlert(message: string, isError: boolean = true) {
  const el = document.getElementById('sig-alert');
  if (!el) return;
  el.textContent = message;
  el.className = `alert-message ${isError ? 'alert-error' : 'alert-success'}`;
  el.style.display = 'flex';
}

function hideSignatureAlert() {
  const el = document.getElementById('sig-alert');
  if (el) el.style.display = 'none';
}

function stopSignaturePolling() {
  if (signaturePollingTimer) {
    clearInterval(signaturePollingTimer);
    signaturePollingTimer = null;
  }
}

function startSignaturePolling(token: string) {
  stopSignaturePolling();
  signaturePollingTimer = setInterval(() => {
    void refreshSignatureStatus(token, false);
  }, 5000);
}

/**
 * Renderiza o estado atual da sessão de assinatura eletrônica forense
 */
function renderSignatureState(view: SignaturePublicView) {
  currentSignatureView = view;

  const contractNameEl = document.getElementById('sig-contract-name');
  const statusBadgeEl = document.getElementById('sig-status-badge');
  const customerNameEl = document.getElementById('sig-customer-name');
  const customerDocEl = document.getElementById('sig-customer-doc');
  const companyNameEl = document.getElementById('sig-company-name');
  const sha256El = document.getElementById('sig-sha256-hash');
  const expiresAtEl = document.getElementById('sig-expires-at');
  const renderedContentEl = document.getElementById('sig-rendered-content');
  const consentClauseEl = document.getElementById('sig-consent-clause');
  const consentBoxEl = document.getElementById('sig-consent-box');
  const chkConsent = document.getElementById('chk-signature-consent') as HTMLInputElement | null;

  const pixPanel = document.getElementById('sig-pix-panel');
  const signedPanel = document.getElementById('sig-signed-panel');
  const fallbackPanel = document.getElementById('sig-fallback-panel');

  if (contractNameEl) contractNameEl.textContent = view.contractName || 'Contrato de Prestação de Serviços SCM';
  if (customerNameEl) customerNameEl.textContent = view.customerName || '—';
  if (customerDocEl) customerDocEl.textContent = view.customerDocumentMasked || '—';
  if (companyNameEl) companyNameEl.textContent = view.companyName || config.defaultTitle;
  if (sha256El) {
    sha256El.textContent = view.documentSha256Hash || 'Integrado na geração do documento';
    sha256El.title = view.documentSha256Hash || '';
  }
  if (expiresAtEl && view.expiresAt) {
    expiresAtEl.textContent = `Válido até ${formatDate(view.expiresAt.split('T')[0])}`;
  }
  if (renderedContentEl) {
    renderedContentEl.textContent = view.renderedContent || '';
  }
  if (consentClauseEl && view.consentClause) {
    consentClauseEl.textContent = view.consentClause;
  }

  // Configura QR Code e Copia-e-Cola do PIX Simbólico
  const symbolicAmountEl = document.getElementById('sig-symbolic-amount');
  if (symbolicAmountEl) {
    symbolicAmountEl.textContent = `R$ ${formatCurrency(view.symbolicAmount || 1.0)}`;
  }

  const pixCodeEl = document.getElementById('sig-pix-code-text');
  const pixQrImg = document.getElementById('sig-pix-qr-img') as HTMLImageElement | null;
  const pixCode = view.pixCopyPaste || '';
  if (pixCodeEl) pixCodeEl.textContent = pixCode;
  if (pixQrImg && pixCode) {
    if (view.pixQrCodeBase64) {
      pixQrImg.src = view.pixQrCodeBase64.startsWith('data:')
        ? view.pixQrCodeBase64
        : `data:image/png;base64,${view.pixQrCodeBase64}`;
    } else {
      pixQrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(pixCode)}`;
    }
  }

  // Tratamento por status
  if (view.status === 'SIGNED') {
    stopSignaturePolling();
    if (statusBadgeEl) {
      statusBadgeEl.className = 'status-badge status-active';
      statusBadgeEl.textContent = '✓ Contrato Assinado';
    }
    if (consentBoxEl) consentBoxEl.style.display = 'none';
    if (pixPanel) pixPanel.style.display = 'none';
    if (fallbackPanel) fallbackPanel.style.display = 'none';
    if (signedPanel) signedPanel.style.display = 'block';

    const payerNameEl = document.getElementById('sig-payer-name');
    const payerBankEl = document.getElementById('sig-payer-bank');
    const creditAmountEl = document.getElementById('sig-credit-amount');
    const signedAtEl = document.getElementById('sig-signed-at');
    const btnDownloadPdf = document.getElementById('btn-sig-download-pdf') as HTMLAnchorElement | null;

    if (payerNameEl) payerNameEl.textContent = view.payerName || view.customerName;
    if (payerBankEl) payerBankEl.textContent = view.payerBankName || 'Confirmado via SPI/BACEN';
    if (creditAmountEl) {
      creditAmountEl.textContent = `R$ ${formatCurrency(view.onboardingCreditAmount ?? view.symbolicAmount ?? 1.0)} (Abatimento automático na 1ª fatura)`;
    }
    if (signedAtEl) {
      signedAtEl.textContent = view.signedAt ? new Date(view.signedAt).toLocaleString('pt-BR') : 'Confirmado agora';
    }
    if (btnDownloadPdf) {
      btnDownloadPdf.href = getSignedContractPdfUrl(view.token, view.signedPdfUrl);
    }
    return;
  }

  if (view.status === 'REJECTED_DIVERGENT_DOCUMENT' || view.status === 'AWAITING_FALLBACK_SIGNATURE') {
    stopSignaturePolling();
    if (statusBadgeEl) {
      statusBadgeEl.className =
        view.status === 'REJECTED_DIVERGENT_DOCUMENT'
          ? 'status-badge status-blocked'
          : 'status-badge status-unblocked';
      statusBadgeEl.textContent =
        view.status === 'REJECTED_DIVERGENT_DOCUMENT'
          ? 'Divergência de CPF (Anti-Fraude)'
          : 'Em Contingência MP 2.200-2';
    }
    if (pixPanel) pixPanel.style.display = 'none';
    if (signedPanel) signedPanel.style.display = 'none';
    if (fallbackPanel) fallbackPanel.style.display = 'block';

    const rejectionBanner = document.getElementById('sig-rejection-banner');
    const rejectionReasonEl = document.getElementById('sig-rejection-reason');
    const fallbackActiveBanner = document.getElementById('sig-fallback-active-banner');
    const fallbackActiveDesc = document.getElementById('sig-fallback-active-desc');

    if (view.rejectionReason && rejectionBanner && rejectionReasonEl) {
      rejectionReasonEl.textContent = view.rejectionReason;
      rejectionBanner.style.display = 'flex';
    } else if (rejectionBanner) {
      rejectionBanner.style.display = 'none';
    }

    if (view.status === 'AWAITING_FALLBACK_SIGNATURE' && fallbackActiveBanner && fallbackActiveDesc) {
      fallbackActiveDesc.textContent = `Modalidade selecionada: ${formatFallbackMethodLabel(view.fallbackMethod || undefined)}. Nossa equipe ou portal finalizará a validação.`;
      fallbackActiveBanner.style.display = 'flex';
    } else if (fallbackActiveBanner) {
      fallbackActiveBanner.style.display = 'none';
    }
    return;
  }

  if (view.status === 'EXPIRED') {
    stopSignaturePolling();
    if (statusBadgeEl) {
      statusBadgeEl.className = 'status-badge status-blocked';
      statusBadgeEl.textContent = 'Sessão Expirada';
    }
    if (pixPanel) pixPanel.style.display = 'none';
    if (signedPanel) signedPanel.style.display = 'none';
    if (fallbackPanel) fallbackPanel.style.display = 'none';
    showSignatureAlert('Este link de assinatura expirou. Solicite um novo link ao atendimento do provedor.', true);
    return;
  }

  // Default: AWAITING_PAYMENT
  if (statusBadgeEl) {
    statusBadgeEl.className = 'status-badge status-unblocked';
    statusBadgeEl.textContent = 'Aguardando PIX R$ 1,00';
  }
  if (consentBoxEl) consentBoxEl.style.display = 'block';
  if (signedPanel) signedPanel.style.display = 'none';
  if (fallbackPanel) fallbackPanel.style.display = 'none';
  if (pixPanel) pixPanel.style.display = 'block';

  const isConsented = !!(chkConsent && chkConsent.checked);
  const lockedHint = document.getElementById('sig-pix-locked-hint');
  const unlockedContent = document.getElementById('sig-pix-unlocked-content');
  if (lockedHint) lockedHint.style.display = isConsented ? 'none' : 'block';
  if (unlockedContent) unlockedContent.style.display = isConsented ? 'block' : 'none';
}

/**
 * Atualiza o status da assinatura via endpoint leve /status
 */
async function refreshSignatureStatus(token: string, manualFeedback: boolean = false) {
  if (!currentSignatureView) return;
  const btnCheck = document.getElementById('btn-sig-check-now') as HTMLButtonElement | null;
  if (manualFeedback && btnCheck) {
    btnCheck.disabled = true;
    btnCheck.textContent = '⏳ Consultando BACEN...';
  }

  try {
    const statusRes = await fetchPublicSignatureStatus(token);
    currentSignatureView = {
      ...currentSignatureView,
      status: statusRes.status,
      payerName: statusRes.payerName ?? currentSignatureView.payerName,
      payerBankName: statusRes.payerBankName ?? currentSignatureView.payerBankName,
      rejectionReason: statusRes.rejectionReason ?? currentSignatureView.rejectionReason,
      signedPdfUrl: statusRes.signedPdfUrl ?? currentSignatureView.signedPdfUrl,
      documentSha256Hash: statusRes.documentSha256Hash ?? currentSignatureView.documentSha256Hash,
      fallbackMethod: statusRes.fallbackMethod ?? currentSignatureView.fallbackMethod,
      onboardingCreditAmount: statusRes.onboardingCreditAmount ?? currentSignatureView.onboardingCreditAmount,
      forensicCertificatePdfUrl: statusRes.forensicCertificatePdfUrl ?? currentSignatureView.forensicCertificatePdfUrl,
      signedAt: statusRes.signedAt ?? currentSignatureView.signedAt,
    };
    renderSignatureState(currentSignatureView);
  } catch (err: unknown) {
    if (manualFeedback) {
      const msg = err instanceof Error ? err.message : 'Não foi possível verificar o status agora.';
      showSignatureAlert(msg, true);
    }
  } finally {
    if (manualFeedback && btnCheck) {
      btnCheck.disabled = false;
      btnCheck.textContent = '🔄 Já paguei / Verificar agora';
    }
  }
}

/**
 * Carrega a tela pública de assinatura eletrônica forense (/sign/{token})
 */
async function loadPublicSignatureView(token: string) {
  const viewLogin = document.getElementById('view-login');
  const viewDashboard = document.getElementById('view-dashboard');
  const viewSignature = document.getElementById('view-signature');
  const portalHeader = document.getElementById('portal-header');

  if (viewLogin) viewLogin.style.display = 'none';
  if (viewDashboard) viewDashboard.style.display = 'none';
  if (portalHeader) portalHeader.style.display = 'none';
  if (viewSignature) viewSignature.style.display = 'block';

  hideSignatureAlert();

  try {
    const view = await fetchPublicSignatureView(token);
    document.title = `Assinatura de Contrato • ${view.companyName || config.defaultTitle}`;
    renderSignatureState(view);

    if (view.status === 'AWAITING_PAYMENT') {
      startSignaturePolling(token);
    }
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Link de assinatura inválido ou expirado.';
    showSignatureAlert(msg, true);
  }
}

/**
 * Copia o código PIX da assinatura eletrônica
 */
async function handleSignatureCopyPix() {
  const pixText = document.getElementById('sig-pix-code-text');
  const copyFeedback = document.getElementById('sig-copy-feedback');
  if (!pixText || !pixText.textContent) return;

  try {
    await navigator.clipboard.writeText(pixText.textContent);
    if (copyFeedback) {
      copyFeedback.style.display = 'block';
      setTimeout(() => {
        if (copyFeedback) copyFeedback.style.display = 'none';
      }, 4000);
    }
  } catch {
    const textarea = document.createElement('textarea');
    textarea.value = pixText.textContent;
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
    if (copyFeedback) copyFeedback.style.display = 'block';
  }
}

/**
 * Envia solicitação de contingência MP 2.200-2 pelo próprio cliente
 */
async function handleSignatureFallbackSubmit(e: Event) {
  e.preventDefault();
  if (!currentSignatureView) return;

  const selectMethod = document.getElementById('select-sig-fallback-method') as HTMLSelectElement | null;
  const inputJustification = document.getElementById('input-sig-fallback-justification') as HTMLInputElement | null;
  const btnSubmit = document.getElementById('btn-sig-submit-fallback') as HTMLButtonElement | null;

  const method = (selectMethod?.value || 'GOV_BR') as FallbackMethodType;
  const justification = inputJustification?.value.trim() || undefined;

  if (btnSubmit) {
    btnSubmit.disabled = true;
    btnSubmit.textContent = 'Registrando contingência...';
  }
  hideSignatureAlert();

  try {
    const updated = await selectPublicSignatureFallback(currentSignatureView.token, method, justification);
    currentSignatureView = {
      ...currentSignatureView,
      status: updated.status,
      fallbackMethod: updated.fallbackMethod ?? method,
    };
    renderSignatureState(currentSignatureView);
    showSignatureAlert('Modalidade de contingência registrada com sucesso!', false);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Não foi possível registrar a contingência.';
    showSignatureAlert(msg, true);
  } finally {
    if (btnSubmit) {
      btnSubmit.disabled = false;
      btnSubmit.textContent = 'Confirmar Modalidade de Contingência';
    }
  }
}

/**
 * Executa Desbloqueio em Confiança
 */
async function handleTrustUnblock() {
  const session = getCurrentSession();
  if (!session || !currentDashboard || !currentDashboard.contract) return;

  const btn = document.getElementById('btn-trust-unblock') as HTMLButtonElement | null;
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '<div class="spinner" style="width: 18px; height: 18px;"></div> Liberando conexão...';
  }

  try {
    await requestTrustUnblock(currentDashboard.contract.id, session.customerId);

    // Atualiza o estado visual
    const statusBadge = document.getElementById('status-badge');
    const statusText = document.getElementById('status-badge-text');
    const unblockWrapper = document.getElementById('trust-unblock-wrapper');

    if (statusBadge) statusBadge.className = 'status-badge status-unblocked';
    if (statusText) statusText.textContent = 'Liberado em Confiança (48h)';
    if (unblockWrapper) unblockWrapper.style.display = 'none';

    alert('Sua conexão foi liberada em confiança com sucesso por 48 horas!');
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : 'Não foi possível solicitar o desbloqueio.';
    alert(msg);
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = '⚡ Desbloqueio em Confiança (48h)';
    }
  }
}

/**
 * Realiza logout
 */
function handleLogout() {
  logoutClient();
  currentDashboard = null;

  const viewLogin = document.getElementById('view-login');
  const viewDashboard = document.getElementById('view-dashboard');
  const portalHeader = document.getElementById('portal-header');
  const groupPin = document.getElementById('group-pin');
  const inputDoc = document.getElementById('input-document') as HTMLInputElement | null;
  const inputPin = document.getElementById('input-pin') as HTMLInputElement | null;

  if (viewLogin) viewLogin.style.display = 'flex';
  if (viewDashboard) viewDashboard.style.display = 'none';
  if (portalHeader) portalHeader.style.display = 'none';
  if (groupPin) groupPin.style.display = 'none';
  if (inputDoc) inputDoc.value = '';
  if (inputPin) inputPin.value = '';

  hideLoginAlert();
}

/**
 * Inicialização dos Event Listeners do DOM
 */
function setupEventListeners() {
  const formLogin = document.getElementById('form-login');
  const inputDoc = document.getElementById('input-document') as HTMLInputElement | null;
  const inputPin = document.getElementById('input-pin') as HTMLInputElement | null;
  const groupPin = document.getElementById('group-pin');
  const btnSubmit = document.getElementById('btn-login-submit') as HTMLButtonElement | null;
  const btnLogout = document.getElementById('btn-logout');
  const btnUnblock = document.getElementById('btn-trust-unblock');
  const btnClosePix = document.getElementById('btn-close-pix');
  const btnCopyPix = document.getElementById('btn-copy-pix');
  const tabPending = document.getElementById('tab-pending');
  const tabPaid = document.getElementById('tab-paid');

  // Máscara dinâmica de CPF/CNPJ
  if (inputDoc) {
    inputDoc.addEventListener('input', () => {
      inputDoc.value = formatDocument(inputDoc.value);
    });
  }

  // Formulário de Login
  if (formLogin) {
    formLogin.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (!inputDoc || !btnSubmit) return;

      const doc = inputDoc.value.trim();
      const pin = inputPin ? inputPin.value.trim() : '';

      if (!doc) {
        showLoginAlert('Por favor, informe seu CPF ou CNPJ.');
        return;
      }

      btnSubmit.disabled = true;
      btnSubmit.innerHTML = '<div class="spinner"></div> Identificando...';
      hideLoginAlert();

      try {
        const auth = await loginClient(doc, pin);

        if (auth.status === 'FORCE_CHANGE_PIN' && auth.customerId) {
          pendingCustomerIdForPin = auth.customerId;
          isForceChangeMode = true;
          openPinModal(true);
          showLoginAlert('Por segurança, cadastre um novo PIN de 4 dígitos.', false);
          return;
        }

        if (auth.status === 'PIN_REQUIRED') {
          if (groupPin) groupPin.style.display = 'block';
          if (inputPin) inputPin.focus();
          showLoginAlert('Informe seu PIN de 4 dígitos para prosseguir.', false);
          btnSubmit.disabled = false;
          btnSubmit.innerHTML = 'Acessar Minha Conta';
          return;
        }

        if (auth.status === 'AUTHENTICATED' && auth.customerId) {
          await loadDashboard(auth.customerId);
        }
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Falha na autenticação';
        showLoginAlert(msg, true);
      } finally {
        if (btnSubmit) {
          btnSubmit.disabled = false;
          btnSubmit.innerHTML = 'Acessar Minha Conta';
        }
      }
    });
  }

  // Abas de Faturas
  if (tabPending && tabPaid) {
    tabPending.addEventListener('click', () => {
      activeTab = 'pending';
      tabPending.classList.add('active');
      tabPaid.classList.remove('active');
      renderInvoices();
    });

    tabPaid.addEventListener('click', () => {
      activeTab = 'paid';
      tabPaid.classList.add('active');
      tabPending.classList.remove('active');
      renderInvoices();
    });
  }

  if (btnUnblock) btnUnblock.addEventListener('click', handleTrustUnblock);
  if (btnLogout) btnLogout.addEventListener('click', handleLogout);
  if (btnClosePix) btnClosePix.addEventListener('click', closePixModal);
  if (btnCopyPix) btnCopyPix.addEventListener('click', copyPixCode);

  // Gestão de PIN
  const btnOpenPin = document.getElementById('btn-open-pin-modal');
  const btnClosePin = document.getElementById('btn-close-pin-modal');
  const formPin = document.getElementById('form-pin');

  if (btnOpenPin) btnOpenPin.addEventListener('click', () => openPinModal(false));
  if (btnClosePin) btnClosePin.addEventListener('click', closePinModal);
  if (formPin) formPin.addEventListener('submit', handlePinSubmit);

  // Eventos da Página Pública de Assinatura Eletrônica Forense
  const chkSigConsent = document.getElementById('chk-signature-consent') as HTMLInputElement | null;
  const btnSigCopyPix = document.getElementById('btn-sig-copy-pix');
  const btnSigCheckNow = document.getElementById('btn-sig-check-now');
  const btnSigOpenFallback = document.getElementById('btn-sig-open-fallback');
  const formSigFallback = document.getElementById('form-sig-fallback');

  if (chkSigConsent) {
    chkSigConsent.addEventListener('change', () => {
      const lockedHint = document.getElementById('sig-pix-locked-hint');
      const unlockedContent = document.getElementById('sig-pix-unlocked-content');
      if (lockedHint) lockedHint.style.display = chkSigConsent.checked ? 'none' : 'block';
      if (unlockedContent) unlockedContent.style.display = chkSigConsent.checked ? 'block' : 'none';
    });
  }

  if (btnSigCopyPix) btnSigCopyPix.addEventListener('click', handleSignatureCopyPix);
  if (btnSigCheckNow) {
    btnSigCheckNow.addEventListener('click', () => {
      if (currentSignatureView) {
        void refreshSignatureStatus(currentSignatureView.token, true);
      }
    });
  }
  if (btnSigOpenFallback) {
    btnSigOpenFallback.addEventListener('click', () => {
      const fallbackPanel = document.getElementById('sig-fallback-panel');
      if (fallbackPanel) {
        fallbackPanel.style.display = 'block';
        fallbackPanel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    });
  }
  if (formSigFallback) {
    formSigFallback.addEventListener('submit', handleSignatureFallbackSubmit);
  }

  // Fecha modais ao clicar fora
  window.addEventListener('click', (e) => {
    const modalPix = document.getElementById('modal-pix');
    const modalPin = document.getElementById('modal-pin');
    if (modalPix && e.target === modalPix) closePixModal();
    if (modalPin && e.target === modalPin && !isForceChangeMode) closePinModal();
  });
}

/**
 * Ponto de Entrada Principal
 */
async function bootstrap() {
  await loadBranding();
  setupEventListeners();

  // Verifica se a URL é um link público de Assinatura Eletrônica Forense (/sign/{token} ou ?token={token})
  const signatureToken = extractSignatureToken(window.location.pathname, window.location.search);
  if (signatureToken) {
    await loadPublicSignatureView(signatureToken);
    return;
  }

  const session = getCurrentSession();
  if (session) {
    await loadDashboard(session.customerId);
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', bootstrap);
} else {
  bootstrap();
}

