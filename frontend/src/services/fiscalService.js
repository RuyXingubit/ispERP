import api from './api';

const fiscalService = {
  getActiveCompany: async () => {
    const response = await api.get('/fiscal/company');
    return response.data;
  },

  saveCompany: async (companyData) => {
    const response = await api.post('/fiscal/company', companyData);
    return response.data;
  },

  uploadCertificate: async (companyId, file, password) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('password', password);

    const response = await api.post(`/fiscal/company/${companyId}/certificate`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  getActiveConfig: async () => {
    const response = await api.get('/fiscal/configs');
    return response.data;
  },

  emitNfcom: async (invoiceId) => {
    const response = await api.post(`/fiscal/invoices/${invoiceId}/emit`);
    return response.data;
  },

  getRecords: async (page = 0, size = 10) => {
    const response = await api.get(`/fiscal/records?page=${page}&size=${size}&sort=createdAt,desc`);
    return response.data;
  },

  cancelNfcom: async (recordId, reason) => {
    const response = await api.post(`/fiscal/records/${recordId}/cancel`, { reason });
    return response.data;
  },

  getConvenio115ExportUrl: (year, month) => {
    return `/api/fiscal/convenio115/export?year=${year}&month=${month}`;
  },

  sendAccountingReport: async (year, month) => {
    const response = await api.post(`/fiscal/convenio115/send-accounting?year=${year}&month=${month}`);
    return response.data;
  },
};

export default fiscalService;
