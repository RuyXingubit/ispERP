export type StorageType = 'S3' | 'LOCAL';

export type StorageProvider =
  | 'SEAWEEDFS_LOCAL'
  | 'AWS_S3'
  | 'CLOUDFLARE_R2'
  | 'CUSTOM_S3'
  | 'LOCAL_DISK';

export interface StorageConfigRequest {
  companyId?: string;
  storageType: StorageType;
  provider: StorageProvider;
  endpointUrl?: string;
  bucketName: string;
  region: string;
  accessKey?: string;
  secretKey?: string;
  pathStyleAccess: boolean;
  isActive: boolean;
}

export interface StorageConfigResponse {
  id?: string;
  companyId?: string;
  storageType: StorageType;
  provider: StorageProvider;
  endpointUrl?: string;
  bucketName: string;
  region: string;
  accessKey?: string;
  maskedSecretKey?: string;
  pathStyleAccess: boolean;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface StorageConnectionTestResponse {
  success: boolean;
  message: string;
  details?: string;
  latencyMs: number;
}
