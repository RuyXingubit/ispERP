import api from './api';
import { StorageConfig, StorageConfigRequest, StorageConnectionTestResponse } from '../types/storage';

export const storageConfigService = {
  getConfig: async (): Promise<StorageConfig> => {
    const response = await api.get<StorageConfig>('/storage/config');
    return response.data;
  },

  getActiveConfig: async (): Promise<StorageConfig> => {
    const response = await api.get<StorageConfig>('/storage/config');
    return response.data;
  },

  saveConfig: async (config: StorageConfigRequest | Record<string, any>): Promise<StorageConfig> => {
    const response = await api.put<StorageConfig>('/storage/config', config);
    return response.data;
  },

  testConnection: async (config: StorageConfigRequest | Record<string, any>): Promise<StorageConnectionTestResponse> => {
    const response = await api.post<StorageConnectionTestResponse>('/storage/config/test', config);
    return response.data;
  },
};

export default storageConfigService;
