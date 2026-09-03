import api from './api';
import { getTechnicianExecution } from '../api/generated/endpoints/technician-execution/technician-execution';
import {
  InstallationMaterialDemand,
  TechnicianDispatchCandidate,
  OltUnprovisionedOnu,
  RadiusSessionStatus,
  TechnicianExecutionCompleteRequest,
} from '../types/onboarding-dispatch';

const technicianExecutionApi = getTechnicianExecution();

export const onboardingDispatchService = {
  // Despacho de Instalações
  async listDemands(): Promise<InstallationMaterialDemand[]> {
    const response = await api.get<InstallationMaterialDemand[]>('/dispatch/installations/demands');
    return response.data;
  },

  async getDemandByWorkOrder(workOrderId: string): Promise<InstallationMaterialDemand> {
    const response = await api.get<InstallationMaterialDemand>(`/dispatch/installations/demands/${workOrderId}`);
    return response.data;
  },

  async listCandidates(workOrderId: string): Promise<TechnicianDispatchCandidate[]> {
    const response = await api.get<TechnicianDispatchCandidate[]>(`/dispatch/installations/${workOrderId}/candidates`);
    return response.data;
  },

  async dispatchWorkOrder(workOrderId: string, technicianId: string): Promise<any> {
    const response = await api.post(`/dispatch/installations/${workOrderId}/dispatch`, null, {
      params: { technicianId },
    });
    return response.data;
  },

  // Execução de Campo pelo Técnico (Plugado nos Contratos OpenAPI)
  async listUnprovisionedOnus(workOrderId: string): Promise<OltUnprovisionedOnu[]> {
    const data = await technicianExecutionApi.listUnprovisionedOnus(workOrderId);
    return data as unknown as OltUnprovisionedOnu[];
  },

  async provisionOnu(
    workOrderId: string,
    data: {
      onuSerial: string;
      vlanId?: number;
      pppoeUsername?: string;
      pppoePassword?: string;
    }
  ): Promise<any> {
    return technicianExecutionApi.provisionOnu(workOrderId, {
      onuSerial: data.onuSerial,
      vlanId: data.vlanId,
      pppoeUsername: data.pppoeUsername,
      pppoePassword: data.pppoePassword,
    });
  },

  async getRadiusStatus(workOrderId: string): Promise<RadiusSessionStatus> {
    const data = await technicianExecutionApi.getRadiusStatus(workOrderId);
    return data as unknown as RadiusSessionStatus;
  },

  async completeInstallation(
    workOrderId: string,
    payload: TechnicianExecutionCompleteRequest
  ): Promise<any> {
    const response = await api.post(
      `/technician/execution/${workOrderId}/complete`,
      payload
    );
    return response.data;
  },
};
