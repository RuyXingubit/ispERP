import api from './api';

const clientPortalService = {
  getDashboard: async (customerId) => {
    const params = customerId ? { customerId } : {};
    const response = await api.get('/portal/client/dashboard', { params });
    return response.data;
  },

  updateProfile: async (data, customerId) => {
    const params = customerId ? { customerId } : {};
    const response = await api.put('/portal/client/profile', data, { params });
    return response.data;
  },

  changePassword: async (data, customerId) => {
    const params = customerId ? { customerId } : {};
    const response = await api.post('/portal/client/change-password', data, { params });
    return response.data;
  },

  upgradePlan: async (contractId, newPlanId, customerId) => {
    const params = customerId ? { customerId } : {};
    const response = await api.post('/portal/client/upgrade-plan', { contractId, newPlanId }, { params });
    return response.data;
  },

  requestTrustUnblock: async (contractId, customerId) => {
    const params = customerId ? { customerId } : {};
    const response = await api.post('/portal/client/trust-unblock', { contractId }, { params });
    return response.data;
  }
};

export default clientPortalService;
