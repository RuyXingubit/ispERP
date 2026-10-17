// SPDX-License-Identifier: MIT
import { apiFetch } from './api';

export interface InvoiceCompositionItem {
  id?: string;
  name: string;
  category: 'SCM' | 'EQUIPMENT_RENTAL' | 'SVA_DIGITAL_CONTENT' | 'SVA_STREAMING_APP' | 'SVA_OTHER';
  amount: number;
  fiscalCode?: string;
}

export interface InvoiceItem {
  id: string;
  contractId: string;
  customerId: string;
  amount: number;
  scmAmount?: number;
  svaAmount?: number;
  compositionItems?: InvoiceCompositionItem[];
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
  monthlyPrice?: number;
  description?: string;
}

export interface ContractItem {
  id: string;
  status: string;
  dueDay?: number;
  monthlyFee?: number;
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

export interface ActiveNetworkIncidentItem {
  id: string;
  oltId: string;
  ponPort: string;
  ctoId?: string;
  incidentType: string;
  status: string;
  affectedCount: number;
  description: string;
  detectedAt?: string;
}

export interface Tr069ConnectedHostItem {
  hostName: string;
  ipAddress: string;
  macAddress: string;
  band: string;
  rssiDbm?: number;
}

export interface Tr069CommandLogItem {
  id: string;
  contractId: string;
  deviceId?: string;
  commandType: string;
  triggeredByRole: string;
  triggeredByName?: string;
  status: string;
  summary: string;
  createdAt: string;
}

export interface Tr069CpeWifiStatus {
  id: string;
  contractId: string;
  deviceId: string;
  serialNumber: string;
  modelName: string;
  firmwareVersion: string;
  ssid24ghz: string;
  channel24ghz: string;
  ssid5ghz: string;
  channel5ghz: string;
  bandSteeringEnabled: boolean;
  securityMode: string;
  uptimeSeconds: number;
  acsSyncStatus: string;
  lastInformAt?: string;
  lastRebootAt?: string;
  connectedHosts?: Tr069ConnectedHostItem[];
  recentCommands?: Tr069CommandLogItem[];
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
  activeNetworkIncident?: ActiveNetworkIncidentItem | null;
  cpeWifiStatus?: Tr069CpeWifiStatus | null;
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

export async function fetchSubscriberWifi(customerId: string): Promise<Tr069CpeWifiStatus> {
  return await apiFetch<Tr069CpeWifiStatus>(`/portal/client/wifi?customerId=${customerId}`);
}

export async function updateSubscriberWifi(
  customerId: string,
  payload: {
    ssid24ghz: string;
    ssid5ghz: string;
    password?: string;
    channel24ghz?: string;
    channel5ghz?: string;
    bandSteeringEnabled?: boolean;
  }
): Promise<Tr069CpeWifiStatus> {
  return await apiFetch<Tr069CpeWifiStatus>(`/portal/client/wifi?customerId=${customerId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function rebootSubscriberCpe(customerId: string): Promise<Tr069CommandLogItem> {
  return await apiFetch<Tr069CommandLogItem>(`/portal/client/wifi/reboot?customerId=${customerId}`, {
    method: 'POST',
  });
}

export async function requestTrustUnblock(contractId: string, customerId: string): Promise<unknown> {
  return await apiFetch(`/portal/client/trust-unblock?customerId=${customerId}`, {
    method: 'POST',
    body: JSON.stringify({ contractId }),
  });
}

export async function requestPlanUpgrade(
  contractId: string,
  newPlanId: string,
  notes?: string
): Promise<{ message: string }> {
  return await apiFetch<{ message: string }>(`/contracts/${contractId}/change-plan`, {
    method: 'POST',
    body: JSON.stringify({
      newPlanId,
      notes: notes || 'Upgrade solicitado pelo assinante na Central do Assinante',
    }),
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

