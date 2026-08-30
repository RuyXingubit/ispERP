import api from './api';
import {
  RadiusPolicyConfig,
  RadiusPolicyConfigRequest,
  RadiusLifecycleSummary,
  RadiusLifecycleLog,
  RadiusManualActionRequest,
  RadiusManualActionResponse,
} from '../types';

export const radiusLifecycleService = {
  getSummary: async (): Promise<RadiusLifecycleSummary> => {
    const res = await api.get<RadiusLifecycleSummary>('/radius/lifecycle/summary');
    return res.data;
  },

  getPolicyConfig: async (): Promise<RadiusPolicyConfig> => {
    const res = await api.get<RadiusPolicyConfig>('/radius/lifecycle/policy');
    return res.data;
  },

  updatePolicyConfig: async (data: RadiusPolicyConfigRequest): Promise<RadiusPolicyConfig> => {
    const res = await api.put<RadiusPolicyConfig>('/radius/lifecycle/policy', data);
    return res.data;
  },

  getLogs: async (page = 0, size = 20): Promise<{ content: RadiusLifecycleLog[]; totalElements: number }> => {
    const res = await api.get<{ content: RadiusLifecycleLog[]; totalElements: number }>(
      `/radius/lifecycle/logs?page=${page}&size=${size}`
    );
    return res.data;
  },

  executeManualAction: async (data: RadiusManualActionRequest): Promise<RadiusManualActionResponse> => {
    const res = await api.post<RadiusManualActionResponse>('/radius/lifecycle/action', data);
    return res.data;
  },

  runAutoBlockNow: async (): Promise<void> => {
    await api.post('/radius/lifecycle/run-autoblock');
  },
};
