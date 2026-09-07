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
