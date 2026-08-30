import api from './api';

export const storageConfigService = {
  getActiveConfig: async () => {
    const response = await api.get('/storage/config');
    return response.data;
  },

  saveConfig: async (configData) => {
    const response = await api.put('/storage/config', configData);
    return response.data;
  },

  testConnection: async (configData) => {
    const response = await api.post('/storage/config/test', configData);
    return response.data;
  }
};

export default storageConfigService;
