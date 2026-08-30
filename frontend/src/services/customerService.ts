import api from './api';
import { Customer } from '../types/customer';

export const customerService = {
  getAll: async (): Promise<Customer[]> => {
    const response = await api.get<Customer[]>('/customers');
    return response.data;
  },

  getAllCustomers: async () => {
    return api.get<Customer[]>('/customers');
  },

  getById: async (id: string): Promise<Customer> => {
    const response = await api.get<Customer>(`/customers/${id}`);
    return response.data;
  },

  create: async (customerData: Partial<Customer>): Promise<Customer> => {
    const response = await api.post<Customer>('/customers', customerData);
    return response.data;
  },

  update: async (id: string, customerData: Partial<Customer>): Promise<Customer> => {
    const response = await api.put<Customer>(`/customers/${id}`, customerData);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/customers/${id}`);
  },

  search: async (query: string): Promise<Customer[]> => {
    const response = await api.get<Customer[]>(`/customers/search?q=${encodeURIComponent(query)}`);
    return response.data;
  },
};

export default customerService;
