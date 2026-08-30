import { NasVendorType } from './radius';

export interface CgnatMapping {
  id: string;
  nasId?: string;
  nasName?: string;
  vendorType: NasVendorType;
  publicIp: string;
  portStart: number;
  portEnd: number;
  privateIpStart: string;
  privateIpEnd: string;
  protocol: 'TCP' | 'UDP' | 'BOTH';
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CgnatMappingRequest {
  nasId?: string;
  vendorType?: NasVendorType;
  publicIp: string;
  portStart: number;
  portEnd: number;
  privateIpStart: string;
  privateIpEnd: string;
  protocol?: 'TCP' | 'UDP' | 'BOTH';
  notes?: string;
}

export interface CgnatScriptImportRequest {
  nasId?: string;
  vendorType?: NasVendorType;
  scriptContent: string;
  replaceExisting?: boolean;
}

export interface CgnatScriptImportResponse {
  totalParsed: number;
  totalSaved: number;
  importedMappings: CgnatMapping[];
  warnings: string[];
}
