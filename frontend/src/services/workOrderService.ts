import api from './api';
import { WorkOrder, CompleteWorkOrderPayload } from '../types/workorder';

export const workOrderService = {
  getAll: async (): Promise<WorkOrder[]> => {
    const response = await api.get<WorkOrder[]>('/work-orders');
    return response.data;
  },

  getAllWorkOrders: async () => {
    return api.get<WorkOrder[]>('/work-orders');
  },

  getById: async (id: string): Promise<WorkOrder> => {
    const response = await api.get<WorkOrder>(`/work-orders/${id}`);
    return response.data;
  },

  create: async (data: Partial<WorkOrder>): Promise<WorkOrder> => {
    const response = await api.post<WorkOrder>('/work-orders', data);
    return response.data;
  },

  update: async (id: string, data: Partial<WorkOrder>): Promise<WorkOrder> => {
    const response = await api.put<WorkOrder>(`/work-orders/${id}`, data);
    return response.data;
  },

  assign: async (id: string, technicianId: string): Promise<WorkOrder> => {
    const response = await api.put<WorkOrder>(`/work-orders/${id}/assign`, { technicianId });
    return response.data;
  },

  scheduleWorkOrder: async (id: string, scheduleData: { scheduledDate: string; scheduledPeriod: string; technicianName: string }) => {
    const response = await api.put<WorkOrder>(`/work-orders/${id}/schedule`, scheduleData);
    return response.data;
  },

  complete: async (id: string, notes?: string): Promise<WorkOrder> => {
    const response = await api.put<WorkOrder>(`/work-orders/${id}/complete`, { notes });
    return response.data;
  },

  completeWorkOrder: async (id: string, payload: Partial<CompleteWorkOrderPayload> | Record<string, unknown>): Promise<WorkOrder> => {
    const response = await api.put<WorkOrder>(`/work-orders/${id}/complete`, payload);
    return response.data;
  },
};

export default workOrderService;
