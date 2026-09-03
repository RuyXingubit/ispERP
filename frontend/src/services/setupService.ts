import api from './api';

export const setupService = {
  getStatus: () => api.get('/setup/status'),
  getSetupStatus: async () => {
    try {
      return await api.get('/setup/status');
    } catch {
      return await api.get('/initial-setup/status');
    }
  },
  completeSetup: (data: Record<string, unknown>) => api.post('/setup/complete', data),
  performSetup: (data: Record<string, unknown>) => api.post('/setup/complete', data),
};

export default setupService;
