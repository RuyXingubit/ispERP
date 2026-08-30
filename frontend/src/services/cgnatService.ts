import api from './api';
import {
  CgnatMapping,
  CgnatMappingRequest,
  CgnatScriptImportRequest,
  CgnatScriptImportResponse,
} from '../types/cgnat';

export const cgnatService = {
  getAllMappings: async (): Promise<CgnatMapping[]> => {
    const response = await api.get<CgnatMapping[]>('/api/cgnat/mappings');
    return response.data;
  },

  getMappingsByNas: async (nasId: string): Promise<CgnatMapping[]> => {
    const response = await api.get<CgnatMapping[]>(`/api/cgnat/mappings/nas/${nasId}`);
    return response.data;
  },

  createMapping: async (data: CgnatMappingRequest): Promise<CgnatMapping> => {
    const response = await api.post<CgnatMapping>('/api/cgnat/mappings', data);
    return response.data;
  },

  importScript: async (data: CgnatScriptImportRequest): Promise<CgnatScriptImportResponse> => {
    const response = await api.post<CgnatScriptImportResponse>('/api/cgnat/import-script', data);
    return response.data;
  },

  deleteMapping: async (id: string): Promise<void> => {
    await api.delete(`/api/cgnat/mappings/${id}`);
  },
};
