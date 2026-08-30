import api from './api';

export interface NetworkDevice {
  id?: string;
  name: string;
  ipAddress: string;
  vendor: string;
  model: string;
  snmpCommunity?: string;
  active?: boolean;
}

export interface OnuProvisioning {
  id?: string;
  contractId: string;
  serialNumber: string;
  oltId?: string;
  ponPort?: string;
  status?: string;
  signalRx?: number;
  signalTx?: number;
}

export const networkService = {
  // Dispositivos de Rede (OLTs)
  getAllDevices: () => api.get<NetworkDevice[]>('/network-devices'),
  getDeviceById: (id: string) => api.get<NetworkDevice>(`/network-devices/${id}`),
  saveDevice: (device: Partial<NetworkDevice>) => api.post<NetworkDevice>('/network-devices', device),

  // Provisionamentos de ONU
  getAllOnus: () => api.get<OnuProvisioning[]>('/onus'),
  getOnuById: (id: string) => api.get<OnuProvisioning>(`/onus/${id}`),
  getOnuByContractId: (contractId: string) => api.get<OnuProvisioning>(`/onus/contract/${contractId}`),
  blockOnu: (contractId: string, reason = 'Inadimplência') =>
    api.post(`/onus/contract/${contractId}/block?reason=${encodeURIComponent(reason)}`),
  unblockOnu: (contractId: string) => api.post(`/onus/contract/${contractId}/unblock`),
  diagnoseOnu: (id: string) => api.get(`/onus/${id}/diagnose`),
};

export default networkService;
