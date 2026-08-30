import api from './api';

export interface SaleProspect {
  id?: string;
  name: string;
  cpf: string;
  phone: string;
  email?: string;
  cep: string;
  address: string;
  planId: string;
  dueDate: number;
}

export const saleService = {
  createSale: (saleData: SaleProspect) => api.post('/sales', saleData),
  submitSale: (saleData: SaleProspect) => api.post('/sales', saleData),
};

export default saleService;
