import { getInvoices } from '../api/generated/endpoints/invoices/invoices';
import {
  InvoiceResponse,
  InvoiceStatus,
  TriggerRecurringBillingResponse,
} from '../api/generated/models';
import api from './api';

const invoicesApi = getInvoices();

export const invoiceService = {
  // Chamadas oficiais geradas diretamente pelo Contrato OpenAPI (API-First)
  getAll: async (status?: InvoiceStatus): Promise<InvoiceResponse[]> => {
    return invoicesApi.getAllInvoices(status ? { status } : undefined);
  },

  getAllInvoices: async (status?: InvoiceStatus): Promise<InvoiceResponse[]> => {
    return invoicesApi.getAllInvoices(status ? { status } : undefined);
  },

  getByCustomerId: async (customerId: string): Promise<InvoiceResponse[]> => {
    return invoicesApi.getInvoicesByCustomerId(customerId);
  },

  getByContractId: async (contractId: string): Promise<InvoiceResponse[]> => {
    return invoicesApi.getInvoicesByContractId(contractId);
  },

  getById: async (id: string): Promise<InvoiceResponse> => {
    return invoicesApi.getInvoiceById(id);
  },

  generateInvoiceManually: async (options: { contractId: string; dueDate?: string } | any): Promise<InvoiceResponse> => {
    const contractId = options.contractId || options.contract_id;
    const dueDate = options.dueDate || options.due_date;
    return invoicesApi.generateInvoiceManually(contractId, dueDate ? { dueDate } : undefined);
  },

  create: async (options: { contractId: string; dueDate?: string } | any): Promise<InvoiceResponse> => {
    const contractId = options.contractId || options.contract_id;
    const dueDate = options.dueDate || options.due_date;
    return invoicesApi.generateInvoiceManually(contractId, dueDate ? { dueDate } : undefined);
  },

  markAsPaid: async (id: string, paidAmount?: number, paymentMethod = 'PIX'): Promise<InvoiceResponse> => {
    return invoicesApi.payInvoice(id, { paidAmount, paymentMethod });
  },

  cancelInvoice: async (id: string): Promise<InvoiceResponse> => {
    return invoicesApi.cancelInvoice(id);
  },

  triggerRecurringBilling: async (): Promise<TriggerRecurringBillingResponse> => {
    return invoicesApi.triggerRecurringBilling();
  },

  // Helpers auxiliares para PIX e Boleto
  generatePix: async (id: string): Promise<{ qrcode: string; qrcodeText: string }> => {
    const response = await api.post(`/invoices/${id}/pix`);
    return response.data;
  },

  generateBoleto: async (id: string): Promise<{ barcode: string; pdfUrl: string }> => {
    const response = await api.post(`/invoices/${id}/boleto`);
    return response.data;
  },
};

export default invoiceService;
