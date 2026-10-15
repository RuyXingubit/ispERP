// SPDX-License-Identifier: MIT

/**
 * Formata CPF ou CNPJ com máscara
 */
export function formatDocument(value: string): string {
  if (!value) return '';
  const clean = value.replace(/\D/g, '');
  if (clean.length <= 11) {
    // CPF: 000.000.000-00
    return clean
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d{1,2})$/, '$1-$2')
      .slice(0, 14);
  }
  // CNPJ: 00.000.000/0000-00
  return clean
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d{1,2})$/, '$1-$2')
    .slice(0, 18);
}

/**
 * Formata moeda para padrão BRL (R$ 79,90)
 */
export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'decimal',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

/**
 * Formata data ISO (YYYY-MM-DD) para padrão brasileiro (DD/MM/YYYY)
 */
export function formatDate(isoDate: string): string {
  if (!isoDate) return '';
  const parts = isoDate.split('-');
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  return isoDate;
}

/**
 * Valida se o PIN contém exatamente 4 dígitos numéricos
 */
export function validatePin(pin: string): boolean {
  if (!pin) return false;
  return /^\d{4}$/.test(pin.trim());
}

/**
 * Extrai o token de assinatura eletrônica forense a partir do pathname (/sign/{token})
 * ou query string (?token={token} ou ?sign={token}).
 */
export function extractSignatureToken(pathname: string, search: string): string | null {
  if (pathname) {
    const cleanPath = pathname.replace(/\/+$/, '');
    const match = cleanPath.match(/(?:^|\/)sign\/([A-Za-z0-9_-]{8,})$/);
    if (match && match[1]) {
      return match[1];
    }
  }
  if (search) {
    const params = new URLSearchParams(search.startsWith('?') ? search : `?${search}`);
    const tokenParam = params.get('token') || params.get('sign');
    if (tokenParam && /^[A-Za-z0-9_-]{8,}$/.test(tokenParam.trim())) {
      return tokenParam.trim();
    }
  }
  return null;
}

/**
 * Traduz o status do contrato para exibição amigável ao assinante
 */
export function formatContractStatus(status?: string): string {
  switch ((status || '').toUpperCase()) {
    case 'ACTIVE':
      return 'Ativo';
    case 'BLOCKED':
    case 'SUSPENDED':
      return 'Bloqueado / Suspenso';
    case 'PENDING':
    case 'DRAFT':
      return 'Aguardando Ativação';
    case 'CANCELED':
    case 'CANCELLED':
      return 'Cancelado';
    default:
      return status || 'Ativo';
  }
}

/**
 * Traduz o método de contingência MP 2.200-2 para exibição amigável
 */
export function formatFallbackMethodLabel(method?: string): string {
  switch ((method || '').toUpperCase()) {
    case 'GOV_BR':
      return 'Assinatura Digital Gov.br (ICP-Brasil / Prata ou Ouro)';
    case 'EMAIL_OTP':
      return 'Token OTP Auditado (E-mail / WhatsApp Oficial + Evidência de IP)';
    case 'PHYSICAL_NOTARY':
      return 'Assinatura Física / Reconhecimento em Cartório';
    default:
      return method || 'Contingência MP 2.200-2';
  }
}

/**
 * Calcula a prévia Pro-Rata Die (base comercial 30 dias) para Upgrade/Downgrade na Central do Assinante (Onda I).
 */
export function calculatePortalProRataPreview(
  currentMonthlyFee: number,
  newMonthlyFee: number,
  dueDay: number = 10,
  referenceDate: Date = new Date()
): { remainingDays: number; proRataDelta: number } {
  const safeDueDay = Math.min(28, Math.max(1, dueDay || 10));
  const today = new Date(
    referenceDate.getFullYear(),
    referenceDate.getMonth(),
    referenceDate.getDate()
  );
  let nextDue = new Date(today.getFullYear(), today.getMonth(), safeDueDay);
  if (nextDue <= today) {
    nextDue = new Date(today.getFullYear(), today.getMonth() + 1, safeDueDay);
  }
  const diffMs = nextDue.getTime() - today.getTime();
  const remainingDays = Math.min(
    30,
    Math.max(1, Math.round(diffMs / (1000 * 60 * 60 * 24)))
  );
  const proRataDelta = Number(
    (((newMonthlyFee - currentMonthlyFee) / 30) * remainingDays).toFixed(2)
  );
  return { remainingDays, proRataDelta };
}


