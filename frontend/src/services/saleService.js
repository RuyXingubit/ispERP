import api from './api';

export const saleService = {
  getAllSales: () => api.get('/sales'),
  getSaleById: (id) => api.get(`/sales/${id}`),
  submitSale: (saleData) => api.post('/sales', saleData),
};

export default saleService;
