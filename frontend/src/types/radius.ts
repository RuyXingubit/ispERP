export type NasVendorType =
  | 'MIKROTIK'
  | 'HUAWEI'
  | 'JUNIPER'
  | 'ACCEL_PPP'
  | 'CISCO'
  | 'A10'
  | 'HILLSTONE'
  | 'GENERIC';

export interface Nas {
  id: string;
  companyId?: string;
  nasname: string;
  shortname?: string;
  type: string;
  ports?: number;
  secret: string;
  server?: string;
  community?: string;
  description?: string;
  vendorType: NasVendorType;
  createdAt: string;
  updatedAt: string;
}

export interface NasRequest {
  companyId?: string;
  nasname: string;
  shortname?: string;
  type?: string;
  ports?: number;
  secret: string;
  server?: string;
  community?: string;
  description?: string;
  vendorType: NasVendorType;
}

export interface RadiusSession {
  radacctId: number;
  acctSessionId: string;
  username: string;
  nasIpAddress: string;
  nasShortname?: string;
  acctStartTime?: string;
  acctUpdateTime?: string;
  acctStopTime?: string;
  acctSessionTime?: number;
  acctInputOctets: number;
  acctOutputOctets: number;
  callingStationId?: string;
  framedIpAddress?: string;
  framedIpv6Prefix?: string;
  delegatedIpv6Prefix?: string;
  isOnline: boolean;
  customerName?: string;
  customerCpfCnpj?: string;
}

export interface RadiusDisconnectRequest {
  username: string;
  nasIpAddress?: string;
  acctSessionId?: string;
  framedIpAddress?: string;
}

export interface RadiusDisconnectResponse {
  username: string;
  success: boolean;
  message: string;
}
