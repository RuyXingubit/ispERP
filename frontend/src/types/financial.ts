export type AccountType = 'REVENUE' | 'TAX' | 'DIRECT_COST' | 'OPEX' | 'CAPEX';

export type DreCategory = 
  | 'GROSS_REVENUE'
  | 'TAX_DEDUCTION'
  | 'DIRECT_COST_INTERCONNECTION'
  | 'OPEX_HR'
  | 'OPEX_FLEET'
  | 'OPEX_POLES'
  | 'OPEX_MARKETING'
  | 'OPEX_ADMIN'
  | 'CAPEX_NETWORK'
  | 'CAPEX_EQUIPMENT'
  | 'CAPEX_FLEET';

export interface ChartOfAccountDto {
  id: string;
  parentId?: string | null;
  parentCode?: string | null;
  code: string;
  name: string;
  accountType: AccountType;
  dreCategory: DreCategory;
  isSynthetic: boolean;
  isAnalytical: boolean;
  active: boolean;
  children?: ChartOfAccountDto[];
}

export type PayableStatus = 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED';

export interface ExpenseInstallmentDto {
  id: string;
  installmentNumber: number;
  totalInstallments: number;
  dueDate: string;
  amount: number;
  interestAmount: number;
  status: PayableStatus;
  paidAt?: string | null;
  paidAmount?: number | null;
  paymentMethod?: string | null;
  receiptUrl?: string | null;
}

export interface PayableInvoiceDto {
  id: string;
  supplierName: string;
  supplierDocument?: string | null;
  chartOfAccountId: string;
  chartOfAccountCode?: string | null;
  chartOfAccountName?: string | null;
  description: string;
  invoiceNumber?: string | null;
  totalAmount: number;
  issueDate: string;
  status: PayableStatus;
  notes?: string | null;
  installments: ExpenseInstallmentDto[];
}

export interface PayableInvoiceRequest {
  supplierName: string;
  supplierDocument?: string;
  chartOfAccountId: string;
  description: string;
  invoiceNumber?: string;
  totalAmount: number;
  issueDate?: string;
  installmentsCount: number;
  firstDueDate: string;
  notes?: string;
}

export type FeeStatus = 
  | 'NOT_APPLICABLE'
  | 'BILLABLE'
  | 'PENDING_WAIVER_APPROVAL'
  | 'WAIVED_APPROVED'
  | 'WAIVED_REJECTED';

export interface WorkOrderFeeDto {
  workOrderId: string;
  protocol: string;
  customerId?: string | null;
  customerName?: string | null;
  serviceType?: string | null;
  standardFeeAmount: number;
  feeStatus: FeeStatus;
  waiverReason?: string | null;
  waiverRequestedByUserId?: string | null;
  waiverRequestedByName?: string | null;
  waiverAuditedByUserId?: string | null;
  waiverAuditedByName?: string | null;
  waiverAuditedAt?: string | null;
}
