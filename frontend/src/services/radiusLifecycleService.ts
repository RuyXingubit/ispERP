import { getRadiusLifecycle } from '../api/generated/endpoints/radius-lifecycle/radius-lifecycle';
import type {
  RadiusPolicyConfig,
  RadiusPolicyConfigRequest,
  RadiusLifecycleSummary,
  RadiusLifecycleLog,
  RadiusManualActionRequest,
  RadiusManualActionResponse,
} from '../types';

const lifecycleApi = getRadiusLifecycle();

export const radiusLifecycleService = {
  getSummary: async (): Promise<RadiusLifecycleSummary> => {
    return (await lifecycleApi.getRadiusLifecycleSummary()) as unknown as RadiusLifecycleSummary;
  },

  getPolicyConfig: async (): Promise<RadiusPolicyConfig> => {
    return (await lifecycleApi.getRadiusPolicy()) as unknown as RadiusPolicyConfig;
  },

  updatePolicyConfig: async (data: RadiusPolicyConfigRequest): Promise<RadiusPolicyConfig> => {
    return (await lifecycleApi.updateRadiusPolicy(data as any)) as unknown as RadiusPolicyConfig;
  },

  getLogs: async (page = 0, size = 20): Promise<{ content: RadiusLifecycleLog[]; totalElements: number }> => {
    const res = await lifecycleApi.getRadiusLifecycleLogs({ page, size });
    return {
      content: (res.content || []) as unknown as RadiusLifecycleLog[],
      totalElements: res.totalElements || 0,
    };
  },

  executeManualAction: async (data: RadiusManualActionRequest): Promise<RadiusManualActionResponse> => {
    return (await lifecycleApi.executeRadiusManualAction(data as any)) as unknown as RadiusManualActionResponse;
  },

  runAutoBlockNow: async (): Promise<void> => {
    await lifecycleApi.runRadiusAutoBlock();
  },
};

export default radiusLifecycleService;
