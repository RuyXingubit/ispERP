export type FiberColorStandard = 'ABNT_NBR_14106' | 'TIA_EIA_598';
export type FtthCableType = 'ALIMENTADOR' | 'DISTRIBUICAO' | 'DROP';
export type FtthPortStatus = 'LIVRE' | 'OCUPADA' | 'RESERVADA' | 'DEFEITO';
export type FtthClosureType = 'DOMO' | 'RETANGULAR' | 'SUBTERRANEA';

export type FtthSplitterType =
  | 'BALANCED_1_2'
  | 'BALANCED_1_4'
  | 'BALANCED_1_8'
  | 'BALANCED_1_16'
  | 'BALANCED_1_32'
  | 'BALANCED_1_64'
  | 'UNBALANCED_95_05'
  | 'UNBALANCED_90_10'
  | 'UNBALANCED_85_15'
  | 'UNBALANCED_80_20'
  | 'UNBALANCED_75_25'
  | 'UNBALANCED_70_30'
  | 'UNBALANCED_60_40'
  | 'UNBALANCED_50_50';

export interface FiberColorInfo {
  fiberNumber: number;
  tubeNumber: number;
  fiberInTubeNumber: number;
  fiberColorName: string;
  fiberColorHex: string;
  tubeColorName: string;
  tubeColorHex: string;
  standard: FiberColorStandard;
}

export interface FtthPop {
  id: string;
  companyId?: string;
  name: string;
  latitude?: number;
  longitude?: number;
  address?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface FtthPopRequest {
  companyId?: string;
  name: string;
  latitude?: number;
  longitude?: number;
  address?: string;
  description?: string;
}

export interface FtthPole {
  id: string;
  companyId?: string;
  code: string;
  latitude: number;
  longitude: number;
  poleType: string;
  reservationMeters: number;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface FtthPoleRequest {
  companyId?: string;
  code: string;
  latitude: number;
  longitude: number;
  poleType?: string;
  reservationMeters?: number;
  description?: string;
}

export interface FtthCable {
  id: string;
  companyId?: string;
  name: string;
  cableType: FtthCableType;
  fiberCount: number;
  tubeCount: number;
  colorStandard: FiberColorStandard;
  lengthMeters: number;
  pathCoordinates?: string;
  sourcePopId?: string;
  sourcePoleId?: string;
  targetPoleId?: string;
  attenuationDbPerKm: number;
  fibers?: FiberColorInfo[];
  createdAt: string;
  updatedAt: string;
}

export interface FtthCableRequest {
  companyId?: string;
  name: string;
  cableType?: FtthCableType;
  fiberCount?: number;
  tubeCount?: number;
  colorStandard?: FiberColorStandard;
  lengthMeters?: number;
  pathCoordinates?: string;
  sourcePopId?: string;
  sourcePoleId?: string;
  targetPoleId?: string;
  attenuationDbPerKm?: number;
}

export interface FtthClosure {
  id: string;
  companyId?: string;
  name: string;
  poleId?: string;
  poleCode?: string;
  latitude?: number;
  longitude?: number;
  closureType: FtthClosureType;
  trayCount: number;
  capacityFusions: number;
  usedFusionsCount: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface FtthClosureRequest {
  companyId?: string;
  name: string;
  poleId?: string;
  latitude?: number;
  longitude?: number;
  closureType?: FtthClosureType;
  trayCount?: number;
  capacityFusions?: number;
  status?: string;
}

export interface FtthSplitter {
  id: string;
  companyId?: string;
  closureId?: string;
  name: string;
  splitterType: FtthSplitterType;
  inputCableId?: string;
  inputFiberNumber?: number;
  attenuationDb: number;
  outputPorts: number;
  createdAt: string;
}

export interface FtthSplitterRequest {
  companyId?: string;
  closureId?: string;
  name: string;
  splitterType: FtthSplitterType;
  inputCableId?: string;
  inputFiberNumber?: number;
  attenuationDb?: number;
}

export interface FtthCtoPort {
  id: string;
  ctoId: string;
  portNumber: number;
  status: FtthPortStatus;
  onuProvisioningId?: string;
  onuSerial?: string;
  onuMac?: string;
  customerId?: string;
  customerName?: string;
  pppoeUser?: string;
  notes?: string;
  createdAt: string;
}

export interface FtthCto {
  id: string;
  companyId?: string;
  name: string;
  poleId?: string;
  poleCode?: string;
  closureId?: string;
  latitude: number;
  longitude: number;
  totalPorts: number;
  freePortsCount: number;
  occupiedPortsCount: number;
  occupancyPercentage: number;
  splitterType: string;
  status: string;
  description?: string;
  ports?: FtthCtoPort[];
  createdAt: string;
  updatedAt: string;
}

export interface FtthCtoRequest {
  companyId?: string;
  name: string;
  poleId?: string;
  closureId?: string;
  latitude: number;
  longitude: number;
  totalPorts?: number;
  splitterType?: string;
  status?: string;
  description?: string;
}

export interface FtthFusion {
  id: string;
  closureId: string;
  trayNumber: number;
  sourceCableId: string;
  sourceCableName?: string;
  sourceFiberNumber: number;
  sourceFiberColor?: FiberColorInfo;
  targetCableId?: string;
  targetCableName?: string;
  targetFiberNumber?: number;
  targetFiberColor?: FiberColorInfo;
  targetSplitterId?: string;
  targetSplitterName?: string;
  targetCtoId?: string;
  targetCtoName?: string;
  lossDb: number;
  description?: string;
  createdAt: string;
}

export interface FtthFusionRequest {
  closureId: string;
  trayNumber?: number;
  sourceCableId: string;
  sourceFiberNumber: number;
  targetCableId?: string;
  targetFiberNumber?: number;
  targetSplitterId?: string;
  targetCtoId?: string;
  lossDb?: number;
  description?: string;
}

export interface FtthClosureDiagramResponse {
  closure: FtthClosure;
  cables: FtthCable[];
  splitters: FtthSplitter[];
  fusions: FtthFusion[];
  connectedCtos: FtthCto[];
}

export interface FeasibleCtoItem {
  cto: FtthCto;
  distanceMeters: number;
  freePorts: number;
  hasCapacity: boolean;
}

export interface FtthFeasibilityRequest {
  latitude: number;
  longitude: number;
  maxDistanceMeters?: number;
}

export interface FtthFeasibilityResponse {
  viable: boolean;
  viableCtosCount: number;
  nearbyCtos: FeasibleCtoItem[];
}

export interface LightPathNode {
  elementType: string;
  name: string;
  details: string;
  addedAttenuationDb: number;
  cumulativeAttenuationDb: number;
}

export interface LightPathTraceResult {
  reachedSource: boolean;
  sourcePopName: string;
  totalAttenuationDb: number;
  estimatedRxPowerDbm: number;
  nodes: LightPathNode[];
}
