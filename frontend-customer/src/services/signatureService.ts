// SPDX-License-Identifier: MIT
import { config } from '../config';
import { apiFetch } from './api';

export type SignatureSessionStatus =
  | 'AWAITING_PAYMENT'
  | 'SIGNED'
  | 'REJECTED_DIVERGENT_DOCUMENT'
  | 'AWAITING_FALLBACK_SIGNATURE'
  | 'EXPIRED';

export type FallbackMethodType = 'GOV_BR' | 'EMAIL_OTP' | 'PHYSICAL_NOTARY';

export interface SignaturePublicView {
  token: string;
  contractName: string;
  customerName: string;
  customerDocumentMasked: string;
  companyName: string;
  renderedContent: string;
  consentClause: string;
  status: SignatureSessionStatus;
  symbolicAmount: number;
  pixCopyPaste?: string | null;
  pixQrCodeBase64?: string | null;
  payerName?: string | null;
  payerBankName?: string | null;
  rejectionReason?: string | null;
  signedPdfUrl?: string | null;
  documentSha256Hash?: string | null;
  fallbackMethod?: FallbackMethodType | null;
  onboardingCreditAmount?: number | null;
  forensicCertificatePdfUrl?: string | null;
  expiresAt?: string | null;
  signedAt?: string | null;
}

export interface SignatureStatusResponse {
  id: string;
  contractId: string;
  customerId: string;
  token: string;
  status: SignatureSessionStatus;
  payerName?: string | null;
  payerBankName?: string | null;
  rejectionReason?: string | null;
  signedPdfUrl?: string | null;
  documentSha256Hash?: string | null;
  fallbackMethod?: FallbackMethodType | null;
  fallbackJustification?: string | null;
  onboardingCreditAmount?: number | null;
  forensicCertificatePdfUrl?: string | null;
  expiresAt?: string | null;
  signedAt?: string | null;
}

/**
 * Consulta os dados públicos da sessão de assinatura eletrônica forense
 */
export async function fetchPublicSignatureView(token: string): Promise<SignaturePublicView> {
  return apiFetch<SignaturePublicView>(`/public/signatures/${encodeURIComponent(token)}`, {
    method: 'GET',
  });
}

/**
 * Consulta apenas o status em tempo real da sessão de assinatura (Polling a cada 5s)
 */
export async function fetchPublicSignatureStatus(token: string): Promise<SignatureStatusResponse> {
  return apiFetch<SignatureStatusResponse>(`/public/signatures/${encodeURIComponent(token)}/status`, {
    method: 'GET',
  });
}

/**
 * Registra a opção por contingência jurídica (MP 2.200-2) quando o assinante não possui PIX no próprio CPF
 */
export async function selectPublicSignatureFallback(
  token: string,
  fallbackMethod: FallbackMethodType,
  justification?: string
): Promise<SignatureStatusResponse> {
  return apiFetch<SignatureStatusResponse>(`/public/signatures/${encodeURIComponent(token)}/fallback`, {
    method: 'POST',
    body: JSON.stringify({
      fallbackMethod,
      justification: justification?.trim() || undefined,
    }),
  });
}

/**
 * Retorna a URL direta para download do PDF Assinado + Certificado Forense
 */
export function getSignedContractPdfUrl(token: string, signedPdfUrl?: string | null): string {
  if (signedPdfUrl && signedPdfUrl.startsWith('http')) {
    return signedPdfUrl;
  }
  return `${config.apiBaseUrl}/public/signatures/${encodeURIComponent(token)}/pdf`;
}
