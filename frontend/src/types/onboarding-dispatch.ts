export type MaterialDemandStatus = 
  | 'PENDING_ALLOCATION'
  | 'ALLOCATED_VEHICLE'
  | 'ALLOCATED_CENTRAL'
  | 'CONSUMED'
  | 'CANCELED';

export interface InstallationMaterialDemand {
  id: string;
  workOrderId: string;
  contractId: string;
  contractNumber?: string;
  customerName?: string;
  customerPhone?: string;
  customerAddress?: string;
  customerLatitude?: number;
  customerLongitude?: number;
  ctoId?: string;
  ctoName?: string;
  ctoLatitude?: number;
  ctoLongitude?: number;
  ctoPortNumber?: number;
  estimatedDropMeters: number;
  onuModelRequired: string;
  fastConnectorsCount: number;
  ptoRosetteCount: number;
  status: MaterialDemandStatus;
  allocatedWarehouseId?: string;
  allocatedWarehouseName?: string;
  allocatedTechnicianName?: string;
  createdAt: string;
}

export interface TechnicianDispatchCandidate {
  technicianId: string;
  technicianName: string;
  warehouseId?: string;
  vehicleWarehouseName?: string;
  hasCompleteKit: boolean;
  hasOnu: boolean;
  hasDropCable: boolean;
  hasConnectors: boolean;
  dropCableBalanceMeters: number;
  currentLatitude?: number;
  currentLongitude?: number;
  distanceKmToCustomer?: number;
  lastServiceAddress?: string;
  recommendedScore: number;
}

export interface OltUnprovisionedOnu {
  networkDeviceId: string;
  oltName: string;
  slotNumber: number;
  portNumber: number;
  ponName: string;
  onuSerial: string;
  onuMac?: string;
  rxPowerDbm?: number;
  detectedAt: string;
}

export interface RadiusSessionStatus {
  workOrderId: string;
  username: string;
  online: boolean;
  framedIpAddress?: string;
  nasIpAddress?: string;
  acctStartTime?: string;
  message: string;
}

export interface TechnicianExecutionCompleteRequest {
  onuSerial: string;
  onuMac?: string;
  vlanId?: number;
  pppoeUsername?: string;
  pppoePassword?: string;
  wifiSsid?: string;
  wifiPassword?: string;
  fiberSignalDbm?: number;
  installationPhotoUrl?: string;
  digitalSignatureBase64?: string;
  customerSignatureName?: string;
  notes?: string;
  technicianLatitude?: number;
  technicianLongitude?: number;
  warehouseId?: string;
}
