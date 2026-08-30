import api from './api';

export const inventoryService = {
  getAllItems: () => api.get('/inventory'),
};

export default inventoryService;
