import axios from 'axios';
import {
  InstallationMaterialDemand,
  TechnicianDispatchCandidate,
  OltUnprovisionedOnu,
  RadiusSessionStatus,
  TechnicianExecutionCompleteRequest,
} from '../types/onboarding-dispatch';

const API_BASE = '/api';

export const onboardingDispatchService = {
  // Despacho de Instalações
  async listDemands(): Promise<InstallationMaterialDemand[]> {
    const response = await axios.get(`${API_BASE}/dispatch/installations/demands`);
    return response.data;
  },

  async getDemandByWorkOrder(workOrderId: string): Promise<InstallationMaterialDemand> {
    const response = await axios.get(`${API_BASE}/dispatch/installations/demands/${workOrderId}`);
    return response.data;
  },

  async listCandidates(workOrderId: string): Promise<TechnicianDispatchCandidate[]> {
    const response = await axios.get(`${API_BASE}/dispatch/installations/${workOrderId}/candidates`);
    return response.data;
  },

  async dispatchWorkOrder(workOrderId: string, technicianId: string): Promise<any> {
    const response = await axios.post(`${API_BASE}/dispatch/installations/${workOrderId}/dispatch`, null, {
      params: { technicianId },
    });
    return response.data;
  },

  // Execução de Campo pelo Técnico
  async listUnprovisionedOnus(workOrderId: string): Promise<OltUnprovisionedOnu[]> {
    const response = await axios.get(`${API_BASE}/technician/execution/${workOrderId}/unprovisioned-onus`);
    return response.data;
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
    const response = await axios.post(`${API_BASE}/technician/execution/${workOrderId}/provision`, null, {
      params: data,
    });
    return response.data;
  },

  async getRadiusStatus(workOrderId: string): Promise<RadiusSessionStatus> {
    const response = await axios.get(`${API_BASE}/technician/execution/${workOrderId}/radius-status`);
    return response.data;
  },

  async completeInstallation(
    workOrderId: string,
    payload: TechnicianExecutionCompleteRequest
  ): Promise<any> {
    const response = await axios.post(
      `${API_BASE}/technician/execution/${workOrderId}/complete`,
      payload
    );
    return response.data;
  },
};
