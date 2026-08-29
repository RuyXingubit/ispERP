import api from './api';

export const networkService = {
  // Dispositivos de Rede (OLTs)
  getAllDevices: () => api.get('/network-devices'),
  getDeviceById: (id) => api.get(`/network-devices/${id}`),
  saveDevice: (device) => api.post('/network-devices', device),

  // Provisionamentos de ONU
  getAllOnus: () => api.get('/onus'),
  getOnuById: (id) => api.get(`/onus/${id}`),
  getOnuByContractId: (contractId) => api.get(`/onus/contract/${contractId}`),
  blockOnu: (contractId, reason = 'Inadimplência') =>
    api.post(`/onus/contract/${contractId}/block?reason=${encodeURIComponent(reason)}`),
  unblockOnu: (contractId) => api.post(`/onus/contract/${contractId}/unblock`),
  diagnoseOnu: (id) => api.get(`/onus/${id}/diagnose`),
};

export default networkService;
