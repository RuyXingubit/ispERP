import api from './api';
import { GeoAddressResponse, CoverageCheckResult, GeoCepContributeRequest } from '../types/geocep';

export const geoCepService = {
  lookupCep: async (cep: string): Promise<GeoAddressResponse> => {
    const cleanCep = cep.replace(/\D/g, '');
    const response = await api.get<GeoAddressResponse>(`/geo/cep/${cleanCep}`);
    return response.data;
  },

  checkCoverage: async (lat: number, lng: number): Promise<CoverageCheckResult> => {
    const response = await api.get<CoverageCheckResult>(`/geo/coverage?lat=${lat}&lng=${lng}`);
    return response.data;
  },

  contributeCoordinate: async (data: GeoCepContributeRequest | Record<string, any>) => {
    const response = await api.post('/geo/cep/contribute', data);
    return response.data;
  },

  getRoutesByDate: async (date: string) => {
    const response = await api.get(`/geo/routes?date=${date}`);
    return response.data;
  },

  getStopsByRouteId: async (routeId: string) => {
    const response = await api.get(`/geo/routes/${routeId}/stops`);
    return response.data;
  },

  createOptimizedRoute: async (payload: Record<string, any>) => {
    const response = await api.post('/geo/routes/optimize', payload);
    return response.data;
  },
};

export default geoCepService;
