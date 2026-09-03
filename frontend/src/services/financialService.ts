import api from './api';
import { 
  ChartOfAccountDto, 
  PayableInvoiceDto, 
  PayableInvoiceRequest, 
  ExpenseInstallmentDto, 
  WorkOrderFeeDto 
} from '../types/financial';

export const financialService = {
  // 1. Plano de Contas Dinâmico
  getChartTree: async (): Promise<ChartOfAccountDto[]> => {
    const res = await api.get<ChartOfAccountDto[]>('/financial/chart-of-accounts/tree');
    return res.data;
  },

  getAllAccountsFlat: async (): Promise<ChartOfAccountDto[]> => {
    const res = await api.get<ChartOfAccountDto[]>('/financial/chart-of-accounts');
    return res.data;
  },

  createAccount: async (data: Partial<ChartOfAccountDto>): Promise<ChartOfAccountDto> => {
    const res = await api.post<ChartOfAccountDto>('/financial/chart-of-accounts', data);
    return res.data;
  },

  // 2. Contas a Pagar & Parcelamentos
  getPayables: async (): Promise<PayableInvoiceDto[]> => {
    const res = await api.get<PayableInvoiceDto[]>('/financial/payables');
    return res.data;
  },

  getPendingInstallments: async (): Promise<ExpenseInstallmentDto[]> => {
    const res = await api.get<ExpenseInstallmentDto[]>('/financial/payables/installments/pending');
    return res.data;
  },

  createPayable: async (request: PayableInvoiceRequest): Promise<PayableInvoiceDto> => {
    const res = await api.post<PayableInvoiceDto>('/financial/payables', request);
    return res.data;
  },

  payInstallment: async (
    installmentId: string, 
    params: { paidAmount?: number; paymentMethod?: string; receiptUrl?: string }
  ): Promise<ExpenseInstallmentDto> => {
    const res = await api.post<ExpenseInstallmentDto>(
      `/financial/payables/installments/${installmentId}/pay`, 
      null, 
      { params }
    );
    return res.data;
  },

  // 3. Tarifas de O.S. & Esteira de Isenção Anti-Fraude
  getPendingWaivers: async (): Promise<WorkOrderFeeDto[]> => {
    const res = await api.get<WorkOrderFeeDto[]>('/financial/work-orders/waiver/pending');
    return res.data;
  },

  auditWaiver: async (
    workOrderId: string, 
    managerUserId: string, 
    data: { approved: boolean; notes?: string }
  ): Promise<WorkOrderFeeDto> => {
    const res = await api.post<WorkOrderFeeDto>(
      `/financial/work-orders/${workOrderId}/waiver/audit`, 
      data, 
      { headers: { 'X-User-Id': managerUserId } }
    );
    return res.data;
  }
};
