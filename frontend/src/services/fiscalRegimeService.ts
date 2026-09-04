import { getFiscalRegimeTransitions } from '../api/generated/endpoints/fiscal-regime-transitions/fiscal-regime-transitions';
import type {
  FiscalRegimeTransitionRequest as GeneratedTransitionRequest,
} from '../api/generated/models';
import { FiscalRegimeTransition, FiscalRegimeTransitionRequest } from '../types/regime';

const regimeApi = getFiscalRegimeTransitions();

export const fiscalRegimeService = {
  getTransitions: async (companyId?: string): Promise<FiscalRegimeTransition[]> => {
    const res = await regimeApi.getTransitionHistory(companyId ? { companyId } : undefined);
    return res as unknown as FiscalRegimeTransition[];
  },

  scheduleOrApplyTransition: async (
    data: FiscalRegimeTransitionRequest
  ): Promise<FiscalRegimeTransition> => {
    const res = await regimeApi.scheduleOrApplyTransition(data as unknown as GeneratedTransitionRequest);
    return res as unknown as FiscalRegimeTransition;
  },

  cancelTransition: async (id: string): Promise<FiscalRegimeTransition> => {
    const res = await regimeApi.cancelTransition(id);
    return res as unknown as FiscalRegimeTransition;
  },

  triggerProcessPending: async (): Promise<number> => {
    return regimeApi.triggerProcessPending();
  },
};

export default fiscalRegimeService;
