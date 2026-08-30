import api from './api';
import { Contract } from '../types/contract';

export const contractService = {
  getAll: async (): Promise<Contract[]> => {
    const response = await api.get<Contract[]>('/contracts');
    return response.data;
  },

  getAllContracts: async (): Promise<Contract[]> => {
    const response = await api.get<Contract[]>('/contracts');
    return response.data;
  },

  getByCustomerId: async (customerId: string): Promise<Contract[]> => {
    const response = await api.get<Contract[]>(`/contracts/customer/${customerId}`);
    return response.data;
  },

  getContractsByCustomerId: async (customerId: string): Promise<Contract[]> => {
    const response = await api.get<Contract[]>(`/contracts/customer/${customerId}`);
    return response.data;
  },

  create: async (contractData: Partial<Contract>): Promise<Contract> => {
    const response = await api.post<Contract>('/contracts', contractData);
    return response.data;
  },

  update: async (id: string, contractData: Partial<Contract>): Promise<Contract> => {
    const response = await api.put<Contract>(`/contracts/${id}`, contractData);
    return response.data;
  },

  updateStatus: async (id: string, status: string): Promise<Contract> => {
    const response = await api.put<Contract>(`/contracts/${id}/status`, { status });
    return response.data;
  },

  cancel: async (id: string): Promise<Contract> => {
    const response = await api.put<Contract>(`/contracts/${id}/cancel`);
    return response.data;
  },
};

export default contractService;
