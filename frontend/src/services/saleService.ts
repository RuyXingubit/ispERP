import { getSales } from '../api/generated/endpoints/sales/sales';
import type {
  CreateSaleRequest,
  SaleResponse,
} from '../api/generated/models';

export type { CreateSaleRequest, SaleResponse };
export type SaleProspect = CreateSaleRequest;

const salesApi = getSales();

export const saleService = {
  getAllSales: async (): Promise<SaleResponse[]> => {
    return salesApi.getAllSales();
  },

  getSaleById: async (id: string): Promise<SaleResponse> => {
    return salesApi.getSaleById(id);
  },

  submitSale: async (saleData: CreateSaleRequest): Promise<SaleResponse> => {
    return salesApi.submitSale(saleData);
  },

  createSale: async (saleData: CreateSaleRequest): Promise<SaleResponse> => {
    return salesApi.submitSale(saleData);
  },
};

export default saleService;
