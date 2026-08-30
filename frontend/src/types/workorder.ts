export type WorkOrderType = 'INSTALACAO' | 'MANUTENCAO' | 'RETIRADA';
export type WorkOrderStatus = 'PENDING_SCHEDULE' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELED';

export interface WorkOrder {
  id: string;
  contractId: string;
  contractNumber?: string;
  customerId: string;
  customerName?: string;
  customerPhone?: string;
  installationAddress?: string;
  type: WorkOrderType;
  status: WorkOrderStatus;
  scheduledDate?: string;
  scheduledPeriod?: string;
  technicianName?: string;
  onuMac?: string;
  onuSerial?: string;
  fiberSignalDbm?: number;
  notes?: string;
  technicianLatitude?: number;
  technicianLongitude?: number;
  digitalSignatureBase64?: string;
  customerSignatureName?: string;
  completedAt?: string;
  createdAt?: string;
}

export interface CompleteWorkOrderPayload {
  technicianName: string;
  onuMac: string;
  onuSerial: string;
  fiberSignalDbm: number;
  notes?: string;
  latitude?: number;
  longitude?: number;
  accuracyMeters?: number;
  streetNumberContributed?: string;
  digitalSignatureBase64?: string;
  customerSignatureName?: string;
}
