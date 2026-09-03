import api from './api';
import { FiscalCompany, NfcomRecord } from '../types/fiscal';

export interface CertificateUploadResponse {
  success: boolean;
  errorMessage?: string;
  expiresAt?: string;
}

const fiscalService = {
  getActiveCompany: async (): Promise<FiscalCompany> => {
    const response = await api.get<FiscalCompany>('/fiscal/company');
    return response.data;
  },

  saveCompany: async (companyData: Partial<FiscalCompany>): Promise<FiscalCompany> => {
    const response = await api.post<FiscalCompany>('/fiscal/company', companyData);
    return response.data;
  },

  uploadCertificate: async (
    companyId: string,
    file: File,
    password: string
  ): Promise<CertificateUploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('password', password);

    const response = await api.post<CertificateUploadResponse>(
      `/fiscal/company/${companyId}/certificate`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },

  getActiveConfig: async () => {
    const response = await api.get('/fiscal/configs');
    return response.data;
  },

  emitNfcom: async (invoiceId: string): Promise<NfcomRecord> => {
    const response = await api.post<NfcomRecord>(`/fiscal/invoices/${invoiceId}/emit`);
    return response.data;
  },

  getRecords: async (page = 0, size = 10): Promise<NfcomRecord[]> => {
    const response = await api.get<any>(
      `/fiscal/records?page=${page}&size=${size}&sort=createdAt,desc`
    );
    if (Array.isArray(response.data)) {
      return response.data;
    }
    return response.data?.content || [];
  },

  cancelNfcom: async (recordId: string, reason: string): Promise<NfcomRecord> => {
    const response = await api.post<NfcomRecord>(`/fiscal/records/${recordId}/cancel`, { reason });
    return response.data;
  },

  getConvenio115ExportUrl: (year: number, month: number): string => {
    return `/api/fiscal/convenio115/export?year=${year}&month=${month}`;
  },

  sendAccountingReport: async (year: number, month: number): Promise<{ success: boolean; message?: string }> => {
    const response = await api.post(`/fiscal/convenio115/send-accounting?year=${year}&month=${month}`);
    return response.data;
  },
};

export default fiscalService;
