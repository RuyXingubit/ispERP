import api from './api';

export const inventoryCustodyService = {
  // Depósitos
  getAllWarehouses: () => api.get('/api/warehouses'),
  createWarehouse: (data) => api.post('/api/warehouses', data),

  // Ativos Serializados
  getAllAssets: () => api.get('/api/inventory/custody/assets'),
  getAssetsByWarehouse: (warehouseId) => api.get(`/api/inventory/custody/assets/warehouse/${warehouseId}`),
  getAssetsByHolder: (holderUserId) => api.get(`/api/inventory/custody/assets/holder/${holderUserId}`),

  // Transferências Intermunicipais
  getAllTransfers: () => api.get('/api/inventory/custody/transfers'),
  createTransfer: (data) => api.post('/api/inventory/custody/transfers', data),
  dispatchTransfer: (transferId, data) => api.post(`/api/inventory/custody/transfers/${transferId}/dispatch`, data),
  confirmReceiptTransfer: (transferId, data) => api.post(`/api/inventory/custody/transfers/${transferId}/receive`, data),

  // Termos de Cautela / Nota Promissória para Ferramental
  getAllToolAgreements: () => api.get('/api/inventory/custody/tool-agreements'),
  checkoutToolAgreement: (data) => api.post('/api/inventory/custody/tool-agreements/checkout', data),
  returnToolAgreement: (agreementId, data) => api.post(`/api/inventory/custody/tool-agreements/${agreementId}/return`, data),

  // Logística Reversa de O.S.
  returnAssetFromWorkOrder: (assetId, data) => api.post(`/api/inventory/custody/assets/${assetId}/reverse-logistics`, data),
};
