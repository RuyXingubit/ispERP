import api from './api';

export const planService = {
  getAllPlans: () => api.get('/plans'),
  getActivePlans: () => api.get('/plans/active'),
  getPlanById: (id) => api.get(`/plans/${id}`),
  createPlan: (plan) => api.post('/plans', plan),
  updatePlan: (id, plan) => api.put(`/plans/${id}`, plan),
  deletePlan: (id) => api.delete(`/plans/${id}`),
};

export default planService;
