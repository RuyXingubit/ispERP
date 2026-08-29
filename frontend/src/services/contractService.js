import api from './api';

export const contractService = {
  getAllContracts: () => api.get('/contracts'),
  getContractById: (id) => api.get(`/contracts/${id}`),
  getContractsByCustomerId: (customerId) => api.get(`/contracts/customer/${customerId}`),
  getContractsByStatus: (status) => api.get(`/contracts/status/${status}`),
  updateStatus: (id, status) => api.patch(`/contracts/${id}/status?status=${status}`),
};

export default contractService;
