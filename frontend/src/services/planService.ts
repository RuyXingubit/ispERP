import api from './api';

export interface Plan {
  id?: string;
  name: string;
  description?: string;
  downloadSpeed: number;
  uploadSpeed: number;
  price: number;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export const planService = {
  getAll: async (): Promise<Plan[]> => {
    const response = await api.get<Plan[]>('/plans');
    return response.data;
  },

  getAllPlans: async (): Promise<Plan[]> => {
    const response = await api.get<Plan[]>('/plans');
    return response.data;
  },

  getActivePlans: async (): Promise<Plan[]> => {
    const response = await api.get<Plan[]>('/plans');
    return response.data;
  },

  getById: async (id: string): Promise<Plan> => {
    const response = await api.get<Plan>(`/plans/${id}`);
    return response.data;
  },

  create: async (planData: Partial<Plan>): Promise<Plan> => {
    const response = await api.post<Plan>('/plans', planData);
    return response.data;
  },

  createPlan: async (planData: Partial<Plan>): Promise<Plan> => {
    const response = await api.post<Plan>('/plans', planData);
    return response.data;
  },

  update: async (id: string, planData: Partial<Plan>): Promise<Plan> => {
    const response = await api.put<Plan>(`/plans/${id}`, planData);
    return response.data;
  },

  updatePlan: async (id: string, planData: Partial<Plan>): Promise<Plan> => {
    const response = await api.put<Plan>(`/plans/${id}`, planData);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/plans/${id}`);
  },

  deletePlan: async (id: string): Promise<void> => {
    await api.delete(`/plans/${id}`);
  },
};

export default planService;
