import { getNetworkDevices } from '../api/generated/endpoints/network-devices/network-devices';
import { getOnus } from '../api/generated/endpoints/onus/onus';
import type {
  NetworkDeviceResponse,
  NetworkDeviceCreateRequest,
  OnuResponse,
  OnuStatusResponse,
} from '../api/generated/models';

export type NetworkDevice = NetworkDeviceResponse;
export type { NetworkDeviceCreateRequest };
export type OnuProvisioning = OnuResponse;
export type { OnuStatusResponse };

const networkDevicesApi = getNetworkDevices();
const onusApi = getOnus();

export const networkService = {
  // Dispositivos de Rede (OLTs)
  getAllDevices: async (): Promise<{ data: NetworkDeviceResponse[] }> => {
    const data = await networkDevicesApi.getAllDevices();
    return { data };
  },

  getDeviceById: async (id: string): Promise<{ data: NetworkDeviceResponse }> => {
    const data = await networkDevicesApi.getDeviceById(id);
    return { data };
  },

  saveDevice: async (device: any): Promise<{ data: NetworkDeviceResponse }> => {
    const data = await networkDevicesApi.saveDevice(device);
    return { data };
  },

  // Provisionamentos de ONU
  getAllOnus: async (): Promise<{ data: OnuResponse[] }> => {
    const data = await onusApi.getAllOnus();
    return { data };
  },

  getOnuById: async (id: string): Promise<{ data: OnuResponse }> => {
    const data = await onusApi.getOnuById(id);
    return { data };
  },

  getOnuByContractId: async (contractId: string): Promise<{ data: OnuResponse }> => {
    const data = await onusApi.getOnuByContractId(contractId);
    return { data };
  },

  blockOnu: async (contractId: string, reason = 'Inadimplência'): Promise<{ data: OnuResponse }> => {
    const data = await onusApi.blockOnu(contractId, { reason });
    return { data };
  },

  unblockOnu: async (contractId: string): Promise<{ data: OnuResponse }> => {
    const data = await onusApi.unblockOnu(contractId);
    return { data };
  },

  diagnoseOnu: async (id: string): Promise<{ data: OnuStatusResponse }> => {
    const data = await onusApi.diagnoseOnu(id);
    return { data };
  },
};

export default networkService;
