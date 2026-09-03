import { getWorkOrders } from '../api/generated/endpoints/work-orders/work-orders';
import {
  WorkOrderResponse,
  ScheduleWorkOrderRequest,
  CompleteWorkOrderRequest,
  WorkOrderStatus,
  WorkOrderType,
} from '../api/generated/models';
import api from './api';

const workOrdersApi = getWorkOrders();

export const workOrderService = {
  // Chamadas oficiais geradas diretamente pelo Contrato OpenAPI (API-First)
  getAll: async (status?: WorkOrderStatus): Promise<WorkOrderResponse[]> => {
    return workOrdersApi.getAllWorkOrders(status ? { status } : undefined);
  },

  getAllWorkOrders: async (status?: WorkOrderStatus): Promise<{ data: WorkOrderResponse[] }> => {
    const data = await workOrdersApi.getAllWorkOrders(status ? { status } : undefined);
    return { data };
  },

  getById: async (id: string): Promise<WorkOrderResponse> => {
    return workOrdersApi.getWorkOrderById(id);
  },

  scheduleWorkOrder: async (
    id: string,
    scheduleData: ScheduleWorkOrderRequest
  ): Promise<WorkOrderResponse> => {
    return workOrdersApi.scheduleWorkOrder(id, scheduleData);
  },

  completeWorkOrder: async (
    id: string,
    completeData: CompleteWorkOrderRequest
  ): Promise<WorkOrderResponse> => {
    return workOrdersApi.completeWorkOrder(id, completeData);
  },

  complete: async (id: string, notes?: string): Promise<WorkOrderResponse> => {
    return workOrdersApi.completeWorkOrder(id, {
      onuMac: '00:00:00:00:00:00',
      onuSerial: 'MANUAL',
      fiberSignalDbm: -19.5,
      notes,
    });
  },

  // Métodos legados mantidos para retrocompatibilidade
  create: async (data: any): Promise<WorkOrderResponse> => {
    const response = await api.post<WorkOrderResponse>('/work-orders', data);
    return response.data;
  },

  update: async (id: string, data: any): Promise<WorkOrderResponse> => {
    const response = await api.put<WorkOrderResponse>(`/work-orders/${id}`, data);
    return response.data;
  },

  assign: async (id: string, technicianId: string): Promise<WorkOrderResponse> => {
    const response = await api.put<WorkOrderResponse>(`/work-orders/${id}/assign`, { technicianId });
    return response.data;
  },
};

export default workOrderService;
export type {
  WorkOrderResponse,
  ScheduleWorkOrderRequest,
  CompleteWorkOrderRequest,
  WorkOrderStatus,
  WorkOrderType,
};
