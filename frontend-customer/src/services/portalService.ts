// SPDX-License-Identifier: MIT
import { apiFetch } from './api';

export interface InvoiceItem {
  id: string;
  contractId: string;
  customerId: string;
  amount: number;
  discountAmount?: number;
  penaltyAmount?: number;
  interestAmount?: number;
  dueDate: string;
  status: 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED';
  pixCopiaECola?: string;
  pixQrCodeUrl?: string;
  barcode?: string;
  digitableLine?: string;
  pdfUrl?: string;
  paidAt?: string;
}

export interface PlanItem {
  id: string;
  name: string;
  downloadSpeed: number;
  uploadSpeed: number;
  price: number;
  description?: string;
}

export interface ContractItem {
  id: string;
  status: string;
}

export interface CustomerData {
  id: string;
  name: string;
  cpf: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  state?: string;
  portalPin?: string;
}

export interface ClientPortalDashboard {
  customer: CustomerData;
  contract?: ContractItem;
  currentPlan?: PlanItem;
  availableUpgradePlans?: PlanItem[];
  pendingInvoices: InvoiceItem[];
  paidInvoices: InvoiceItem[];
  overdueInvoices: InvoiceItem[];
  connectionBlocked?: boolean;
  isConnectionBlocked?: boolean;
  canRequestTrustUnblock: boolean;
  connectionStatusMessage?: string;
}

export interface SiteSettings {
  siteTitle?: string;
  primaryColor?: string;
  secondaryColor?: string;
  logoUrl?: string;
}

export interface CompanyData {
  name?: string;
  tradeName?: string;
  phone?: string;
}

export async function fetchDashboard(customerId: string): Promise<ClientPortalDashboard> {
  return await apiFetch<ClientPortalDashboard>(`/portal/client/dashboard?customerId=${customerId}`);
}

export async function requestTrustUnblock(contractId: string, customerId: string): Promise<unknown> {
  return await apiFetch(`/portal/client/trust-unblock?customerId=${customerId}`, {
    method: 'POST',
    body: JSON.stringify({ contractId }),
  });
}

export async function setCustomerPin(customerId: string, newPin: string, currentPin?: string): Promise<{ message: string }> {
  const payload: { customerId: string; newPin: string; currentPin?: string } = {
    customerId,
    newPin,
  };
  if (currentPin) {
    payload.currentPin = currentPin;
  }

  return await apiFetch<{ message: string }>('/portal/client/pin', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function fetchThemeSettings(): Promise<SiteSettings | null> {
  try {
    return await apiFetch<SiteSettings>('/site-settings');
  } catch {
    return null;
  }
}

export async function fetchCompanyData(): Promise<CompanyData | null> {
  try {
    return await apiFetch<CompanyData>('/companies/primary');
  } catch {
    return null;
  }
}
