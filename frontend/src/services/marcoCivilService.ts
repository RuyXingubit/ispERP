import api from './api';
import {
  MarcoCivilSearchRequest,
  MarcoCivilSearchResult,
  MarcoCivilReportRequest,
  MarcoCivilReportResponse,
  PublicValidationResponse,
} from '../types/marcoCivil';

export const marcoCivilService = {
  searchSubscriber: async (data: MarcoCivilSearchRequest): Promise<MarcoCivilSearchResult> => {
    const response = await api.post<MarcoCivilSearchResult>('/api/marco-civil/search', data);
    return response.data;
  },

  generateOfficialReport: async (data: MarcoCivilReportRequest): Promise<MarcoCivilReportResponse> => {
    const response = await api.post<MarcoCivilReportResponse>('/api/marco-civil/reports', data);
    return response.data;
  },

  validatePublicToken: async (token: string): Promise<PublicValidationResponse> => {
    const response = await api.get<PublicValidationResponse>(`/api/public/marco-civil/validate/${token}`);
    return response.data;
  },
};
