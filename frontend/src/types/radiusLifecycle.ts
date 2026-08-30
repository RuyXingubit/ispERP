export type RadiusBlockMode = 'CAPTIVE_PORTAL' | 'BANDWIDTH_REDUCTION' | 'COMPLETE_DISCONNECT';

export type RadiusLifecycleActionType = 
  | 'AUTO_BLOCK' 
  | 'PAYMENT_UNBLOCK' 
  | 'TRUST_UNBLOCK' 
  | 'MANUAL_BLOCK' 
  | 'MANUAL_UNBLOCK' 
  | 'PROVISIONING_SYNC' 
  | 'POD_DISCONNECT';

export interface RadiusPolicyConfig {
  id: string;
  autoBlockEnabled: boolean;
  toleranceDays: number;
  blockMode: RadiusBlockMode;
  reducedDownloadKbps: number;
  reducedUploadKbps: number;
  unblockOnPayment: boolean;
  sendPodOnBlock: boolean;
  sendPodOnUnblock: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface RadiusPolicyConfigRequest {
  autoBlockEnabled: boolean;
  toleranceDays: number;
  blockMode: RadiusBlockMode;
  reducedDownloadKbps: number;
  reducedUploadKbps: number;
  unblockOnPayment: boolean;
  sendPodOnBlock: boolean;
  sendPodOnUnblock: boolean;
}

export interface RadiusLifecycleSummary {
  totalPppoeUsers: number;
  totalActiveUsers: number;
  totalBlockedUsers: number;
  totalTrustUnblocked: number;
  todayAutoBlocksCount: number;
  todayUnblocksCount: number;
  toleranceDays: number;
  autoBlockEnabled: boolean;
}

export interface RadiusLifecycleLog {
  id: string;
  contractId: string;
  customerId: string;
  customerName?: string;
  username: string;
  actionType: RadiusLifecycleActionType;
  reason?: string;
  nasIp?: string;
  success: boolean;
  details?: string;
  createdAt: string;
}

export interface RadiusManualActionRequest {
  contractId: string;
  action: 'BLOCK' | 'UNBLOCK';
  reason?: string;
  vendorType?: 'MIKROTIK' | 'HUAWEI' | 'JUNIPER' | 'CISCO' | 'ACCEL_PPP';
  sendPod?: boolean;
}

export interface RadiusManualActionResponse {
  contractId: string;
  username: string;
  actionApplied: string;
  success: boolean;
  message: string;
  podResult?: string;
}
