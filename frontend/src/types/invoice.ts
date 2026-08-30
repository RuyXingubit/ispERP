export type InvoiceStatus = 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELED';

export interface Invoice {
  id: string;
  contractId: string;
  contractNumber?: string;
  customerName?: string;
  amount: number;
  dueDate: string;
  status: InvoiceStatus;
  pixQrCodeBase64?: string;
  pixCopiaECola?: string;
  paidAt?: string;
  createdAt?: string;
}

export interface RebalanceSimulationRequest {
  contractId: string;
  paidAmount: number;
  paymentDate: string;
}

export interface RebalanceSimulationResult {
  overdueInvoicesCovered: string[];
  futureInvoiceAdjusted: string;
  creditApplied: number;
  nextInvoiceRemainingAmount: number;
  message: string;
}
