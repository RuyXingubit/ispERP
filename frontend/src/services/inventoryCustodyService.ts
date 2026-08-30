import api from './api';

export const inventoryCustodyService = {
  // Depósitos
  getAllWarehouses: () => api.get('/warehouses'),
  createWarehouse: (data: Record<string, unknown>) => api.post('/warehouses', data),

  // Ativos Serializados
  getAllAssets: () => api.get('/inventory/custody/assets'),
  getAssetsByWarehouse: (warehouseId: string) => api.get(`/inventory/custody/assets/warehouse/${warehouseId}`),
  getAssetsByHolder: (holderUserId: string) => api.get(`/inventory/custody/assets/holder/${holderUserId}`),

  // Transferências Intermunicipais
  getAllTransfers: () => api.get('/inventory/custody/transfers'),
  createTransfer: (data: Record<string, unknown>) => api.post('/inventory/custody/transfers', data),
  dispatchTransfer: (transferId: string, data: Record<string, unknown>) =>
    api.post(`/inventory/custody/transfers/${transferId}/dispatch`, data),
  confirmReceiptTransfer: (transferId: string, data: Record<string, unknown>) =>
    api.post(`/inventory/custody/transfers/${transferId}/receive`, data),

  // Termos de Cautela / Nota Promissória para Ferramental
  getAllToolAgreements: () => api.get('/inventory/custody/tool-agreements'),
  checkoutToolAgreement: (data: Record<string, unknown>) =>
    api.post('/inventory/custody/tool-agreements/checkout', data),
  returnToolAgreement: (agreementId: string, data: Record<string, unknown>) =>
    api.post(`/inventory/custody/tool-agreements/${agreementId}/return`, data),

  // Logística Reversa de O.S.
  returnAssetFromWorkOrder: (assetId: string, data: Record<string, unknown>) =>
    api.post(`/inventory/custody/assets/${assetId}/reverse-logistics`, data),
};

export default inventoryCustodyService;
