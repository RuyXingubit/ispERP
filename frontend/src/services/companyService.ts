import api from './api';

export interface Company {
  id?: string;
  name: string;
  document?: string;
  cnpj?: string;
  email?: string;
  phone?: string;
  address?: string;
  website?: string;
  active?: boolean;
  createdAt?: string;
}

export const companyService = {
  getAll: async (): Promise<Company[]> => {
    const res = await api.get<Company[]>('/companies');
    return res.data;
  },
  getPrimary: async (): Promise<Company> => {
    const res = await api.get<Company>('/companies/primary');
    return res.data;
  },
  getById: async (id: string): Promise<Company> => {
    const res = await api.get<Company>(`/companies/${id}`);
    return res.data;
  },
  create: async (company: Partial<Company>): Promise<Company> => {
    const res = await api.post<Company>('/companies', company);
    return res.data;
  },
  update: async (id: string, company: Partial<Company>): Promise<Company> => {
    const res = await api.put<Company>(`/companies/${id}`, company);
    return res.data;
  },
  delete: async (id: string): Promise<void> => {
    await api.delete(`/companies/${id}`);
  },
};

export default companyService;
