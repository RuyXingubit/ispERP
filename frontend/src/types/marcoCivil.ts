export interface MarcoCivilSearchRequest {
  ip: string;
  port?: number;
  timestamp: string; // ISO 8601
}

export interface MarcoCivilSearchResult {
  matched: boolean;
  queriedIp: string;
  queriedPort?: number;
  queriedTimestamp: string;

  // CGNAT
  usedCgnat: boolean;
  resolvedPrivateIp?: string;
  cgnatRuleSummary?: string;

  // RADIUS Session
  radacctId?: number;
  username?: string;
  callingStationId?: string; // MAC ONT
  nasIpAddress?: string;
  sessionStartTime?: string;
  sessionStopTime?: string;

  // Assinante Identificado
  contractId?: string;
  contractNumber?: string;
  customerId?: string;
  customerName?: string;
  customerCpfCnpj?: string;
  customerPhone?: string;
  customerEmail?: string;
  installationAddress?: string;
  planName?: string;
}

export interface MarcoCivilReportRequest {
  courtOrderNumber?: string;
  requesterAuthority?: string;
  queriedIp: string;
  queriedPort?: number;
  queriedTimestamp: string;
  matchedContractId?: string;
  notes?: string;
}

export interface MarcoCivilReportResponse {
  id: string;
  validationToken: string;
  sha256Hash: string;
  publicValidationUrl: string;
  qrCodePayload: string;
  courtOrderNumber?: string;
  requesterAuthority?: string;
  queriedIp: string;
  queriedPort?: number;
  queriedTimestamp: string;
  matchedContractId?: string;
  matchedCustomerName?: string;
  matchedCpfCnpj?: string;
  matchedCallingStationId?: string;
  matchedSessionStart?: string;
  matchedSessionStop?: string;
  reportPdfUrl?: string;
  notes?: string;
  createdAt: string;
}

export interface PublicValidationResponse {
  valid: boolean;
  validationToken: string;
  sha256Hash: string;
  courtOrderNumber?: string;
  requesterAuthority?: string;
  queriedIp: string;
  queriedPort?: number;
  queriedTimestamp: string;
  customerNameMasked?: string;
  customerCpfCnpjMasked?: string;
  callingStationId?: string;
  reportIssuedAt: string;
  statusMessage: string;
}
