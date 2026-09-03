export type CashTransferStatus = 'PENDING_ACCEPTANCE' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';

export type BankDepositStatus = 'PENDING_AUDIT' | 'CONFIRMED_IN_BANK' | 'REJECTED';

export type MaterialType = 'ONT' | 'DROP_CABLE' | 'FAST_CONNECTOR' | 'FUSION_MACHINE' | 'OTDR' | 'TOOL' | 'OTHER';

export interface CashCustody {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  userRole: string;
  cpf?: string;
  currentBalance: number;
  updatedAt: string;
}

export interface CashTransferLog {
  id: string;
  senderUserId: string;
  senderUserName: string;
  receiverUserId: string;
  receiverUserName: string;
  amount: number;
  reason?: string;
  status: CashTransferStatus;
  requestedAt: string;
  respondedAt?: string;
  notes?: string;
}

export interface BankDepositConfirmation {
  id: string;
  depositorUserId: string;
  depositorUserName: string;
  depositorCpf?: string;
  amount: number;
  bankName: string;
  bankAgency?: string;
  bankAccount?: string;
  receiptFileUrl: string;
  depositDate: string;
  status: BankDepositStatus;
  auditedByUserId?: string;
  auditedByUserName?: string;
  auditedAt?: string;
  notes?: string;
  rejectionReason?: string;
}

export interface MaterialCustody {
  id: string;
  userId: string;
  userName: string;
  userCpf?: string;
  itemName: string;
  itemType: MaterialType;
  serialNumber?: string;
  macAddress?: string;
  quantity: number;
  unit: string;
  allocatedAt: string;
  notes?: string;
}

export interface MaterialTransferLog {
  id: string;
  senderUserId: string;
  senderUserName: string;
  receiverUserId: string;
  receiverUserName: string;
  materialCustodyId: string;
  itemName: string;
  serialNumber?: string;
  quantity: number;
  status: CashTransferStatus;
  requestedAt: string;
  respondedAt?: string;
  notes?: string;
}
