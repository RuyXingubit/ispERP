// SPDX-License-Identifier: MIT
import { apiFetch } from './api';

export interface ClientAuthResponse {
  status: 'AUTHENTICATED' | 'PIN_REQUIRED' | 'FORCE_CHANGE_PIN';
  message: string;
  customerId?: string;
  customerName?: string;
  maskedDocument?: string;
  hasPin?: boolean;
  customer?: Record<string, unknown>;
}

export interface CustomerSession {
  customerId: string;
  customerName: string;
  maskedDocument: string;
  hasPin: boolean;
}

export async function loginClient(document: string, pin?: string): Promise<ClientAuthResponse> {
  const payload: { document: string; pin?: string } = { document };
  if (pin && pin.trim().length > 0) {
    payload.pin = pin.trim();
  }

  const response = await apiFetch<ClientAuthResponse>('/portal/client/auth', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

  if (response.status === 'AUTHENTICATED' && response.customerId) {
    sessionStorage.setItem('customer_id', response.customerId);
    sessionStorage.setItem('customer_name', response.customerName || 'Assinante');
    sessionStorage.setItem('customer_doc', response.maskedDocument || document);
    sessionStorage.setItem('customer_has_pin', response.hasPin ? 'true' : 'false');
  }

  return response;
}

export function getCurrentSession(): CustomerSession | null {
  const customerId = sessionStorage.getItem('customer_id');
  if (!customerId) return null;

  return {
    customerId,
    customerName: sessionStorage.getItem('customer_name') || 'Assinante',
    maskedDocument: sessionStorage.getItem('customer_doc') || '',
    hasPin: sessionStorage.getItem('customer_has_pin') === 'true',
  };
}

export function logoutClient(): void {
  sessionStorage.removeItem('customer_id');
  sessionStorage.removeItem('customer_name');
  sessionStorage.removeItem('customer_doc');
  sessionStorage.removeItem('customer_has_pin');
}
