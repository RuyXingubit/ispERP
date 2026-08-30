import api from './api';

export interface DashboardBiMetrics {
  totalCustomers: number;
  activeContracts: number;
  mrr: number;
  openWorkOrders: number;
  openTickets: number;
  overdueInvoicesCount: number;
}

export const dashboardBiService = {
  getMetrics: () => api.get<DashboardBiMetrics>('/dashboard/metrics'),
};

export default dashboardBiService;
