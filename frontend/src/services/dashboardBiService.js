import api from './api';

const dashboardBiService = {
  getMetrics: async () => {
    const response = await api.get('/api/bi/dashboard');
    return response.data;
  }
};

export default dashboardBiService;
