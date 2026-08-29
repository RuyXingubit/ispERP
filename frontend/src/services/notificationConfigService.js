import api from './api';

const notificationConfigService = {
  getAll: async () => {
    const response = await api.get('/notifications/configs');
    return response.data;
  },

  create: async (data) => {
    const response = await api.post('/notifications/configs', data);
    return response.data;
  },

  update: async (id, data) => {
    const response = await api.put(`/notifications/configs/${id}`, data);
    return response.data;
  }
};

export default notificationConfigService;
