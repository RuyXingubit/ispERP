import api from './api';

export const setupService = {
  getStatus: () => api.get('/setup/status'),
  getSetupStatus: () => api.get('/setup/status'),
  completeSetup: (data: Record<string, unknown>) => api.post('/setup/complete', data),
  performSetup: (data: Record<string, unknown>) => api.post('/setup/complete', data),
};

export default setupService;
