import api from './api';
import { Invoice } from '../types/invoice';

export const invoiceService = {
  getAll: async (page = 0, size = 10): Promise<Invoice[]> => {
    const response = await api.get<Invoice[]>(`/invoices?page=${page}&size=${size}`);
    return response.data;
  },

  getAllInvoices: async (page = 0, size = 10): Promise<Invoice[]> => {
    const response = await api.get<Invoice[]>(`/invoices?page=${page}&size=${size}`);
    return response.data;
  },

  getByCustomerId: async (customerId: string): Promise<Invoice[]> => {
    const response = await api.get<Invoice[]>(`/invoices/customer/${customerId}`);
    return response.data;
  },

  getById: async (id: string): Promise<Invoice> => {
    const response = await api.get<Invoice>(`/invoices/${id}`);
    return response.data;
  },

  create: async (invoiceData: Partial<Invoice>): Promise<Invoice> => {
    const response = await api.post<Invoice>('/invoices', invoiceData);
    return response.data;
  },

  generateInvoiceManually: async (invoiceData: Partial<Invoice>): Promise<Invoice> => {
    const response = await api.post<Invoice>('/invoices', invoiceData);
    return response.data;
  },

  markAsPaid: async (id: string): Promise<Invoice> => {
    const response = await api.put<Invoice>(`/invoices/${id}/pay`);
    return response.data;
  },

  cancelInvoice: async (id: string): Promise<Invoice> => {
    const response = await api.put<Invoice>(`/invoices/${id}/cancel`);
    return response.data;
  },

  triggerRecurringBilling: async () => {
    const response = await api.post('/invoices/generate-monthly');
    return response.data;
  },

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
