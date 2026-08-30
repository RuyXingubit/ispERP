export interface Customer {
  id: string;
  name: string;
  cpfCnpj: string;
  email: string;
  phone: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  latitude?: number;
  longitude?: number;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CustomerCreateRequest {
  name: string;
  cpfCnpj: string;
  email: string;
  phone: string;
  address: string;
  city?: string;
  state?: string;
  zipCode?: string;
  latitude?: number;
  longitude?: number;
}
