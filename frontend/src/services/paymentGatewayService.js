import api from './api';

export const paymentGatewayService = {
  getAllConfigs: () => api.get('/payment-gateways'),
  getConfigById: (id) => api.get(`/payment-gateways/${id}`),
  saveConfig: (config) => api.post('/payment-gateways', config),
};

export default paymentGatewayService;
