import api from './api';
import {
  FtthPop,
  FtthPopRequest,
  FtthPole,
  FtthPoleRequest,
  FtthCable,
  FtthCableRequest,
  FtthClosure,
  FtthClosureRequest,
  FtthClosureDiagramResponse,
  FtthSplitter,
  FtthSplitterRequest,
  FtthCto,
  FtthCtoRequest,
  FtthFusion,
  FtthFusionRequest,
  FtthFeasibilityRequest,
  FtthFeasibilityResponse,
  LightPathTraceResult,
} from '../types';

export const ftthService = {
  // POPs
  getAllPops: async (): Promise<FtthPop[]> => {
    const res = await api.get<FtthPop[]>('/ftth/pops');
    return res.data;
  },
  createPop: async (data: FtthPopRequest): Promise<FtthPop> => {
    const res = await api.post<FtthPop>('/ftth/pops', data);
    return res.data;
  },

  // Postes
  getAllPoles: async (): Promise<FtthPole[]> => {
    const res = await api.get<FtthPole[]>('/ftth/poles');
    return res.data;
  },
  createPole: async (data: FtthPoleRequest): Promise<FtthPole> => {
    const res = await api.post<FtthPole>('/ftth/poles', data);
    return res.data;
  },

  // Cabos
  getAllCables: async (): Promise<FtthCable[]> => {
    const res = await api.get<FtthCable[]>('/ftth/cables');
    return res.data;
  },
  getCableById: async (id: string): Promise<FtthCable> => {
    const res = await api.get<FtthCable>(`/ftth/cables/${id}`);
    return res.data;
  },
  createCable: async (data: FtthCableRequest): Promise<FtthCable> => {
    const res = await api.post<FtthCable>('/ftth/cables', data);
    return res.data;
  },

  // Caixas de Emenda (CEO)
  getAllClosures: async (): Promise<FtthClosure[]> => {
    const res = await api.get<FtthClosure[]>('/ftth/closures');
    return res.data;
  },
  getClosureById: async (id: string): Promise<FtthClosure> => {
    const res = await api.get<FtthClosure>(`/ftth/closures/${id}`);
    return res.data;
  },
  createClosure: async (data: FtthClosureRequest): Promise<FtthClosure> => {
    const res = await api.post<FtthClosure>('/ftth/closures', data);
    return res.data;
  },
  getClosureDiagram: async (id: string): Promise<FtthClosureDiagramResponse> => {
    const res = await api.get<FtthClosureDiagramResponse>(`/ftth/closures/${id}/diagram`);
    return res.data;
  },

  // Splitters
  createSplitter: async (closureId: string, data: FtthSplitterRequest): Promise<FtthSplitter> => {
    const res = await api.post<FtthSplitter>(`/ftth/closures/${closureId}/splitters`, data);
    return res.data;
  },

  // Fusões
  createFusion: async (closureId: string, data: FtthFusionRequest): Promise<FtthFusion> => {
    const res = await api.post<FtthFusion>(`/ftth/closures/${closureId}/fusions`, data);
    return res.data;
  },
  deleteFusion: async (id: string): Promise<void> => {
    await api.delete(`/ftth/fusions/${id}`);
  },

  // Caixas de Atendimento (CTO)
  getAllCtos: async (): Promise<FtthCto[]> => {
    const res = await api.get<FtthCto[]>('/ftth/ctos');
    return res.data;
  },
  getCtoById: async (id: string): Promise<FtthCto> => {
    const res = await api.get<FtthCto>(`/ftth/ctos/${id}`);
    return res.data;
  },
  createCto: async (data: FtthCtoRequest): Promise<FtthCto> => {
    const res = await api.post<FtthCto>('/ftth/ctos', data);
    return res.data;
  },

  // Viabilidade & LightPath
  checkFeasibility: async (data: FtthFeasibilityRequest): Promise<FtthFeasibilityResponse> => {
    const res = await api.post<FtthFeasibilityResponse>('/ftth/feasibility', data);
    return res.data;
  },
  traceLightPath: async (ctoPortId: string): Promise<LightPathTraceResult> => {
    const res = await api.get<LightPathTraceResult>(`/ftth/lightpath/${ctoPortId}`);
    return res.data;
  },
};
