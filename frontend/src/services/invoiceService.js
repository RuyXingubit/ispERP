import api from './api';

export const invoiceService = {
  getAllInvoices: (status) => api.get(status ? `/invoices?status=${status}` : '/invoices'),
  getInvoiceById: (id) => api.get(`/invoices/${id}`),
  getInvoicesByCustomerId: (customerId) => api.get(`/invoices/customer/${customerId}`),
  getInvoicesByContractId: (contractId) => api.get(`/invoices/contract/${contractId}`),
  generateInvoiceManually: (contractId, dueDate) =>
    api.post(dueDate ? `/invoices/generate/contract/${contractId}?dueDate=${dueDate}` : `/invoices/generate/contract/${contractId}`),
  markAsPaid: (id, paidAmount, paymentMethod = 'PIX') =>
    api.post(`/invoices/${id}/pay?paidAmount=${paidAmount || ''}&paymentMethod=${paymentMethod}`),
  cancelInvoice: (id) => api.post(`/invoices/${id}/cancel`),
  triggerRecurringBilling: () => api.post('/invoices/trigger-recurring-billing'),
};

export default invoiceService;
