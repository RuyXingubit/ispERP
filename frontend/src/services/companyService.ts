import { getCompanies } from '../api/generated/endpoints/companies/companies';
import type {
  CompanyResponse,
  CompanyCreateRequest,
  CompanyUpdateRequest,
} from '../api/generated/models';

export type Company = CompanyResponse;
export type { CompanyCreateRequest, CompanyUpdateRequest };

const companiesApi = getCompanies();

export const companyService = {
  getAll: async (): Promise<CompanyResponse[]> => {
    return companiesApi.getAllCompanies();
  },

  getPrimary: async (): Promise<CompanyResponse> => {
    return companiesApi.getPrimaryCompany();
  },

  getById: async (id: string): Promise<CompanyResponse> => {
    return companiesApi.getCompanyById(id);
  },

  create: async (company: CompanyCreateRequest): Promise<CompanyResponse> => {
    return companiesApi.createCompany(company);
  },

  update: async (id: string, company: CompanyUpdateRequest): Promise<CompanyResponse> => {
    return companiesApi.updateCompany(id, company);
  },

  delete: async (id: string): Promise<void> => {
    return companiesApi.deleteCompany(id);
  },
};

export default companyService;
