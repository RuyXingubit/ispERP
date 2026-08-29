import api from './api';

export const geoCepService = {
  lookupCep: (cep) => api.get(`/api/geocep/cep/${cep}`),
  createOptimizedRoute: (data) => api.post('/api/geocep/routes/optimize', data),
  getRoutesByDate: (date) => api.get('/api/geocep/routes', { params: { date } }),
  getStopsByRouteId: (routeId) => api.get(`/api/geocep/routes/${routeId}/stops`),
  contributeCoordinate: (data) => api.post('/api/geocep/contribute', data),
};
