import api from './api';

export const setupService = {
  // Verificar status do setup
  getSetupStatus: async () => {
    try {
      const response = await api.get('/initial-setup/status');
      return response.data;
    } catch (error) {
      console.error('Erro ao verificar status do setup:', error);
      throw error;
    }
  },

  // Realizar setup inicial
  performSetup: async (setupData) => {
    try {
      const response = await api.post('/initial-setup', setupData);
      return response.data;
    } catch (error) {
      console.error('Erro ao realizar setup:', error);
      throw error;
    }
  }
};