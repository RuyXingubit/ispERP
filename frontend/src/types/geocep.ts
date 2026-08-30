export interface GeoCepAddress {
  cep: string;
  street: string;
  neighborhood: string;
  city: string;
  state: string;
  latitude?: number;
  longitude?: number;
}

export interface GeoCepContributeRequest {
  cep: string;
  streetNumber: string;
  latitude: number;
  longitude: number;
  accuracyMeters?: number;
  deviceSource?: string;
}
