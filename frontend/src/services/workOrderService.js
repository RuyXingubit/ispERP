import api from './api';

export const workOrderService = {
  getAllWorkOrders: (status) => api.get(status ? `/work-orders?status=${status}` : '/work-orders'),
  getWorkOrderById: (id) => api.get(`/work-orders/${id}`),
  getWorkOrderByContractId: (contractId) => api.get(`/work-orders/contract/${contractId}`),
  scheduleWorkOrder: (id, data) => api.post(`/work-orders/${id}/schedule`, data),
  completeWorkOrder: (id, data) => api.post(`/work-orders/${id}/complete`, data),
};

export default workOrderService;
