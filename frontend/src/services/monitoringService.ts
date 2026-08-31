import api from './api';
import {
  NocMonitoringSummary,
  OltPonPort,
  OltPonPortRequest,
  FtthIncident,
  FtthIncidentDispatchRequest,
  FtthIncidentResolveRequest,
} from '../types';

export const monitoringService = {
  getSummary: async (): Promise<NocMonitoringSummary> => {
    const res = await api.get<NocMonitoringSummary>('/ftth/monitoring/summary');
    return res.data;
  },

  getAllPonPorts: async (): Promise<OltPonPort[]> => {
    const res = await api.get<OltPonPort[]>('/ftth/monitoring/pons');
    return res.data;
  },

  getPonPortsByDevice: async (deviceId: string): Promise<OltPonPort[]> => {
    const res = await api.get<OltPonPort[]>(`/ftth/monitoring/pons/device/${deviceId}`);
    return res.data;
  },

  createPonPort: async (data: OltPonPortRequest): Promise<OltPonPort> => {
    const res = await api.post<OltPonPort>('/ftth/monitoring/pons', data);
    return res.data;
  },

  getAllIncidents: async (): Promise<FtthIncident[]> => {
    const res = await api.get<FtthIncident[]>('/ftth/monitoring/incidents');
    return res.data;
  },

  getActiveIncidents: async (): Promise<FtthIncident[]> => {
    const res = await api.get<FtthIncident[]>('/ftth/monitoring/incidents/active');
    return res.data;
  },

  dispatchIncident: async (id: string, data: FtthIncidentDispatchRequest): Promise<FtthIncident> => {
    const res = await api.post<FtthIncident>(`/ftth/monitoring/incidents/${id}/dispatch`, data);
    return res.data;
  },

  resolveIncident: async (id: string, data: FtthIncidentResolveRequest): Promise<FtthIncident> => {
    const res = await api.post<FtthIncident>(`/ftth/monitoring/incidents/${id}/resolve`, data);
    return res.data;
  },

  forcePollCycle: async (): Promise<NocMonitoringSummary> => {
    const res = await api.post<NocMonitoringSummary>('/ftth/monitoring/poll-now');
    return res.data;
  },
};
