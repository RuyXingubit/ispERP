import api from './api';

export type SecurityMode = 'MANAGED_RESCUE' | 'ZERO_KNOWLEDGE';
export type StorageType = 'S3_COMPATIBLE' | 'SFTP' | 'LOCAL_VOLUME' | 'ISPERP_CLOUD';
export type CompressionAlgorithm = 'ZSTD' | 'GZIP';
export type BackupStatus = 'RUNNING' | 'SUCCESS' | 'FAILED' | 'VERIFIED_OK';
export type BackupTriggerType = 'SCHEDULED' | 'MANUAL' | 'DRY_RUN_VERIFICATION';

export interface BackupOverview {
  securityMode: SecurityMode;
  hasActivePolicy: boolean;
  cronExpression: string;
  retentionDays: number;
  activeDestinationsCount: number;
  totalBackupsCount: number;
  lastBackupStatus?: string;
  lastBackupAt?: string;
  lastBackupSizeBytes?: number;
  lastBackupCompressionRatio?: number;
  lastBackupSha256?: string;
  lastBackupFileName?: string;
  isDryRunVerified: boolean;
  lastDryRunVerifiedAt?: string;
  rescueKitDownloaded: boolean;
}

export interface BackupPolicyRequest {
  securityMode: SecurityMode;
  customMasterKey?: string;
  cronExpression: string;
  retentionDays: number;
  compressionAlgorithm: CompressionAlgorithm;
  autoDryRunEnabled: boolean;
}

export interface BackupPolicyResponse {
  id: string;
  securityMode: SecurityMode;
  masterKeyHash: string;
  generatedPlainMasterKey?: string;
  cronExpression: string;
  retentionDays: number;
  compressionAlgorithm: CompressionAlgorithm;
  autoDryRunEnabled: boolean;
  isActive: boolean;
  rescueKitDownloadedAt?: string;
  createdAt: string;
}

export interface BackupDestinationRequest {
  name: string;
  storageType: StorageType;
  endpointUrl?: string;
  bucketName?: string;
  region?: string;
  accessKey?: string;
  secretKey?: string;
  pathPrefix?: string;
  isPrimary?: boolean;
}

export interface BackupDestinationResponse {
  id: string;
  name: string;
  storageType: StorageType;
  endpointUrl?: string;
  bucketName?: string;
  region: string;
  accessKey?: string;
  pathPrefix: string;
  isActive: boolean;
  isPrimary: boolean;
  lastTestedAt?: string;
  lastTestStatus?: string;
  lastTestError?: string;
  createdAt: string;
}

export interface StorageTestResult {
  success: boolean;
  message: string;
  detailedError?: string;
  latencyMs: number;
}

export interface BackupExecutionLog {
  id: string;
  policyId: string;
  destinationId?: string;
  destinationName?: string;
  triggerType: BackupTriggerType;
  status: BackupStatus;
  fileName: string;
  originalSizeBytes?: number;
  compressedSizeBytes?: number;
  compressionRatio?: number;
  sha256Hash?: string;
  durationSeconds?: number;
  errorMessage?: string;
  isDryRunVerified: boolean;
  dryRunVerifiedAt?: string;
  startedAt: string;
  completedAt?: string;
}

export const backupService = {
  getOverview: async (): Promise<BackupOverview> => {
    const res = await api.get<BackupOverview>('/financial/backup/overview');
    return res.data;
  },

  configurePolicy: async (data: BackupPolicyRequest): Promise<BackupPolicyResponse> => {
    const res = await api.post<BackupPolicyResponse>('/financial/backup/policies', data);
    return res.data;
  },

  listDestinations: async (): Promise<BackupDestinationResponse[]> => {
    const res = await api.get<BackupDestinationResponse[]>('/financial/backup/destinations');
    return res.data;
  },

  createDestination: async (data: BackupDestinationRequest): Promise<BackupDestinationResponse> => {
    const res = await api.post<BackupDestinationResponse>('/financial/backup/destinations', data);
    return res.data;
  },

  testDestination: async (id: string): Promise<StorageTestResult> => {
    const res = await api.post<StorageTestResult>(`/financial/backup/destinations/${id}/test`);
    return res.data;
  },

  deleteDestination: async (id: string): Promise<void> => {
    await api.delete(`/financial/backup/destinations/${id}`);
  },

  executeManualBackup: async (masterKey?: string): Promise<BackupExecutionLog> => {
    const res = await api.post<BackupExecutionLog>('/financial/backup/execute', masterKey ? { masterKey } : {});
    return res.data;
  },

  listHistory: async (): Promise<BackupExecutionLog[]> => {
    const res = await api.get<BackupExecutionLog[]>('/financial/backup/history');
    return res.data;
  },

  downloadEmergencyKit: async (): Promise<Blob> => {
    const res = await api.get('/financial/backup/emergency-kit', { responseType: 'blob' });
    return res.data;
  },
};
