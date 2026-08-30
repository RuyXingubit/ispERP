import api from './api';
import { FiscalRegimeTransition, FiscalRegimeTransitionRequest } from '../types/regime';

export const fiscalRegimeService = {
  getTransitions: async (companyId?: string): Promise<FiscalRegimeTransition[]> => {
    const params = companyId ? { companyId } : {};
    const res = await api.get<FiscalRegimeTransition[]>('/fiscal/regimes/transitions', { params });
    return res.data;
  },

  scheduleOrApplyTransition: async (
    data: FiscalRegimeTransitionRequest
  ): Promise<FiscalRegimeTransition> => {
    const res = await api.post<FiscalRegimeTransition>('/fiscal/regimes/transitions', data);
    return res.data;
  },

  cancelTransition: async (id: string): Promise<FiscalRegimeTransition> => {
    const res = await api.delete<FiscalRegimeTransition>(`/fiscal/regimes/transitions/${id}`);
    return res.data;
  },

  triggerProcessPending: async (): Promise<number> => {
    const res = await api.post<number>('/fiscal/regimes/transitions/process-pending');
    return res.data;
  },
};

export default fiscalRegimeService;
