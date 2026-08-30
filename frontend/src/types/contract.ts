export interface Plan {
  id: string;
  name: string;
  downloadSpeed: number;
  uploadSpeed: number;
  price: number;
  description?: string;
  svaIncluded?: string;
  suspensionDays?: number;
  alwaysIssueNfcom?: boolean;
  active?: boolean;
  createdAt?: string;
}

export type ContractStatus = 'PENDING_INSTALLATION' | 'ACTIVE' | 'SUSPENDED' | 'CANCELED';

export interface Contract {
  id: string;
  customerId: string;
  customerName?: string;
  planId: string;
  planName?: string;
  saleId?: string;
  contractNumber: string;
  status: ContractStatus;
  monthlyFee: number;
  dueDay: number;
  installationAddress: string;
  city?: string;
  state?: string;
  zipCode?: string;
  createdAt?: string;
}
