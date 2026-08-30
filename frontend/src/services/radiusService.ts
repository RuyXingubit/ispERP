import api from './api';
import {
  Nas,
  NasRequest,
  RadiusSession,
  RadiusDisconnectRequest,
  RadiusDisconnectResponse,
} from '../types/radius';

export const radiusService = {
  // NAS / BNGs
  getAllNas: async (): Promise<Nas[]> => {
    const response = await api.get<Nas[]>('/api/radius/nas');
    return response.data;
  },

  getNasById: async (id: string): Promise<Nas> => {
    const response = await api.get<Nas>(`/api/radius/nas/${id}`);
    return response.data;
  },

  createNas: async (data: NasRequest): Promise<Nas> => {
    const response = await api.post<Nas>('/api/radius/nas', data);
    return response.data;
  },

  updateNas: async (id: string, data: NasRequest): Promise<Nas> => {
    const response = await api.put<Nas>(`/api/radius/nas/${id}`, data);
    return response.data;
  },

  deleteNas: async (id: string): Promise<void> => {
    await api.delete(`/api/radius/nas/${id}`);
  },

  // Sessions
  getActiveSessions: async (): Promise<RadiusSession[]> => {
    const response = await api.get<RadiusSession[]>('/api/radius/sessions/active');
    return response.data;
  },

  getSessionHistory: async (username: string): Promise<RadiusSession[]> => {
    const response = await api.get<RadiusSession[]>(`/api/radius/sessions/history/${username}`);
    return response.data;
  },

  disconnectUser: async (data: RadiusDisconnectRequest): Promise<RadiusDisconnectResponse> => {
    const response = await api.post<RadiusDisconnectResponse>('/api/radius/disconnect', data);
    return response.data;
  },
};
