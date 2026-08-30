export interface GeoCepAddress {
  cep: string;
  street: string;
  neighborhood: string;
  city: string;
  state: string;
  latitude?: number;
  longitude?: number;
}

export type GeoAddressResponse = GeoCepAddress;

export interface CoverageCheckResult {
  hasCoverage: boolean;
  closestPopDistanceMeters?: number;
  availableTechnologies?: string[];
  message?: string;
}

export interface GeoCepContributeRequest {
  cep: string;
  streetNumber: string;
  latitude: number;
  longitude: number;
  accuracyMeters?: number;
  deviceSource?: string;
}
