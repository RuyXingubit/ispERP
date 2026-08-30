import api from './api';

export interface PaymentGatewayConfig {
  id?: string;
  type: string;
  sandbox?: boolean;
  apiKey?: string;
  secretKey?: string;
  active?: boolean;
}

export const paymentGatewayService = {
  getConfigs: () => api.get<PaymentGatewayConfig[]>('/financial/gateways'),
  getAllConfigs: () => api.get<PaymentGatewayConfig[]>('/financial/gateways'),
  saveConfig: (config: Partial<PaymentGatewayConfig>) => api.post<PaymentGatewayConfig>('/financial/gateways', config),
  testConnection: (type: string) => api.post(`/financial/gateways/test?type=${type}`),
};

export default paymentGatewayService;
