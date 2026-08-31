export type IncidentType =
  | 'FIBER_CUT_PROBABLE'
  | 'POWER_OUTAGE_PROBABLE'
  | 'MASSIVE_LOS_PON'
  | 'CTO_OFFLINE'
  | 'DEGRADED_SIGNAL';

export type IncidentSeverity = 'CRITICAL' | 'MAJOR' | 'WARNING' | 'INFO';

export type IncidentStatus = 'ACTIVE' | 'INVESTIGATING' | 'DISPATCHED' | 'RESOLVED';

export type OnuSignalStatus =
  | 'ONLINE_GOOD'
  | 'ONLINE_WARNING'
  | 'ONLINE_CRITICAL'
  | 'LOS'
  | 'DYING_GASP'
  | 'OFFLINE';

export interface OltPonPort {
  id: string;
  companyId?: string;
  networkDeviceId: string;
  oltName?: string;
  slotNumber: number;
  portNumber: number;
  ponName: string;
  adminStatus: string;
  operStatus: string;
  txPowerDbm: number;
  temperatureCelsius: number;
  totalOnus: number;
  onlineOnus: number;
  losOnus: number;
  dyingGaspOnus: number;
  offlineOnus: number;
  healthPercentage: number;
  connectedCableId?: string;
  connectedCableName?: string;
  lastPolledAt?: string;
  createdAt: string;
}

export interface OltPonPortRequest {
  companyId?: string;
  networkDeviceId: string;
  slotNumber?: number;
  portNumber?: number;
  ponName: string;
  connectedCableId?: string;
  txPowerDbm?: number;
}

export interface FtthIncident {
  id: string;
  companyId?: string;
  networkDeviceId?: string;
  oltName?: string;
  oltPonPortId?: string;
  ponName?: string;
  incidentType: IncidentType;
  incidentTypeDescription: string;
  severity: IncidentSeverity;
  status: IncidentStatus;
  title: string;
  description?: string;
  affectedCustomersCount: number;
  affectedCtosIds?: string;
  affectedCtoNames?: string[];
  affectedCableId?: string;
  affectedCableName?: string;
  estimatedCutLatitude?: number;
  estimatedCutLongitude?: number;
  estimatedCutDetails?: string;
  workOrderId?: string;
  workOrderProtocol?: string;
  detectedAt: string;
  dispatchedAt?: string;
  resolvedAt?: string;
  rootCauseNotes?: string;
}

export interface FtthIncidentDispatchRequest {
  technicianId?: string;
  notes?: string;
}

export interface FtthIncidentResolveRequest {
  rootCauseNotes: string;
}

export interface NocMonitoringSummary {
  totalOlts: number;
  totalPonPorts: number;
  activePonPorts: number;
  totalOnus: number;
  onlineOnus: number;
  losOnus: number;
  dyingGaspOnus: number;
  offlineOnus: number;
  globalHealthPercentage: number;
  activeIncidentsCount: number;
  criticalIncidentsCount: number;
  activeIncidents: FtthIncident[];
}
