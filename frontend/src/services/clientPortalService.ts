import api from './api';

export const clientPortalService = {
  authenticate: async (document: string, pin?: string) => {
    const response = await api.post('/portal/client/auth', { document, pin });
    return response.data;
  },

  setPin: async (customerId: string, newPin: string, currentPin?: string) => {
    const response = await api.post('/portal/client/pin', { customerId, newPin, currentPin });
    return response.data;
  },

  getDashboard: async (customerId?: string) => {
    const params = customerId ? { customerId } : {};
    const response = await api.get('/portal/client/dashboard', { params });
    return response.data;
  },

  updateProfile: async (data: Record<string, unknown>, customerId?: string) => {
    const params = customerId ? { customerId } : {};
    const response = await api.put('/portal/client/profile', data, { params });
    return response.data;
  },

  changePassword: async (
    data: { oldPassword?: string; currentPassword?: string; newPassword?: string },
    customerId?: string
  ) => {
    const params = customerId ? { customerId } : {};
    const response = await api.post('/portal/client/change-password', data, { params });
    return response.data;
  },

  upgradePlan: async (contractId: string, newPlanId: string, customerId?: string) => {
    const params = customerId ? { customerId } : {};
    const response = await api.post('/portal/client/upgrade-plan', { contractId, newPlanId }, { params });
    return response.data;
  },

  requestTrustUnblock: async (contractId: string, customerId?: string) => {
    const params = customerId ? { customerId } : {};
    const response = await api.post('/portal/client/trust-unblock', { contractId }, { params });
    return response.data;
  },
};

export default clientPortalService;
