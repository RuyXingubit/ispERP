import api from './api';

export interface DashboardBiMetrics {
  mrr: number;
  arr: number;
  arpu: number;
  overdueAmount: number;
  defaultRate: number;
  pixConversionRate: number;
  totalReceivedMonth: number;
  totalCustomers: number;
  activeContracts: number;
  suspendedContracts: number;
  pendingInstallationContracts: number;
  canceledContractsLast30Days: number;
  churnRate: number;
  totalOnus: number;
  provisionedOnus: number;
  criticalSignalOnus: number;
  totalNetworkDevices: number;
  recentOverdueInvoices: Array<{
    id: string;
    amount: number;
    dueDate: string;
    contractId: string;
  }>;
  criticalSignalAlerts: Array<{
    id: string;
    serialNumber: string;
    rxPower: number;
    customerName: string;
  }>;
}

export const dashboardBiService = {
  getMetrics: async (): Promise<DashboardBiMetrics> => {
    try {
      const res = await api.get<DashboardBiMetrics>('/bi/dashboard');
      return res.data;
    } catch {
      const res = await api.get<DashboardBiMetrics>('/dashboard/metrics');
      return res.data;
    }
  },
};

export default dashboardBiService;
