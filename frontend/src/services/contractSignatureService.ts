import api from './api';
import {
  ContractTemplate,
  ContractTemplateRequest,
  ContractTemplateVariableInfo,
  SignatureSession,
  SignaturePublicView,
  CreateSignatureSessionRequest,
} from '../types/contract-signature';

export const contractSignatureService = {
  // --- Templates Administrativos ---
  listTemplates: async (companyId?: string): Promise<ContractTemplate[]> => {
    const params = companyId ? { companyId } : {};
    const res = await api.get<ContractTemplate[]>('/contracts/templates', { params });
    return res.data;
  },

  getTemplateById: async (id: string): Promise<ContractTemplate> => {
    const res = await api.get<ContractTemplate>(`/contracts/templates/${id}`);
    return res.data;
  },

  createTemplate: async (data: ContractTemplateRequest): Promise<ContractTemplate> => {
    const res = await api.post<ContractTemplate>('/contracts/templates', data);
    return res.data;
  },

  updateTemplate: async (id: string, data: ContractTemplateRequest): Promise<ContractTemplate> => {
    const res = await api.put<ContractTemplate>(`/contracts/templates/${id}`, data);
    return res.data;
  },

  deleteTemplate: async (id: string): Promise<void> => {
    await api.delete(`/contracts/templates/${id}`);
  },

  cloneTemplate: async (id: string): Promise<ContractTemplate> => {
    const res = await api.post<ContractTemplate>(`/contracts/templates/${id}/clone`);
    return res.data;
  },

  getVariables: async (): Promise<ContractTemplateVariableInfo[]> => {
    const res = await api.get<ContractTemplateVariableInfo[]>('/contracts/templates/variables');
    return res.data;
  },

  previewTemplate: async (content: string): Promise<string> => {
    const res = await api.post<{ rendered: string }>('/contracts/templates/preview', { content });
    return res.data.rendered;
  },

  // --- Sessões de Assinatura ---
  createSignatureSession: async (data: CreateSignatureSessionRequest): Promise<SignatureSession> => {
    const res = await api.post<SignatureSession>('/contracts/signatures', data);
    return res.data;
  },

  getSignaturesByContract: async (contractId: string): Promise<SignatureSession[]> => {
    const res = await api.get<SignatureSession[]>(`/contracts/${contractId}/signatures`);
    return res.data;
  },

  // --- Endpoints Públicos do Assinante ---
  getPublicSignatureView: async (
    token: string,
    lat?: number,
    lon?: number
  ): Promise<SignaturePublicView> => {
    const params = lat && lon ? { lat, lon } : {};
    const res = await api.get<SignaturePublicView>(`/public/signatures/${token}`, { params });
    return res.data;
  },

  getSignatureStatus: async (token: string): Promise<SignatureSession> => {
    const res = await api.get<SignatureSession>(`/public/signatures/${token}/status`);
    return res.data;
  },

  simulatePixPayment: async (
    token: string,
    data: { payerName: string; payerCpfCnpj: string; bankName?: string }
  ): Promise<SignatureSession> => {
    const res = await api.post<SignatureSession>(`/public/signatures/${token}/simulate-pix`, data);
    return res.data;
  },

  selectFallbackMethod: async (
    token: string,
    fallbackMethod: string,
    justification?: string
  ): Promise<SignatureSession> => {
    const res = await api.post<SignatureSession>(`/public/signatures/${token}/fallback`, {
      fallbackMethod,
      justification
    });
    return res.data;
  },
};
