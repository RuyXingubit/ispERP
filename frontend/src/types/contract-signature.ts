export type DocumentType = 
  | 'SERVICE_AGREEMENT'
  | 'LOYALTY_TERM'
  | 'EQUIPMENT_COMODATO'
  | 'CUSTOM_TERM';

export type SignatureStatus = 
  | 'PENDING'
  | 'SIGNED'
  | 'REJECTED_DIVERGENT_DOCUMENT'
  | 'EXPIRED'
  | 'CANCELED';

export type FallbackMethod = 
  | 'PIX'
  | 'EMAIL_OTP'
  | 'GOV_BR'
  | 'PHYSICAL_NOTARY';

export interface ContractTemplate {
  id: string;
  companyId?: string;
  name: string;
  documentType: DocumentType;
  version: number;
  isActive: boolean;
  contentMarkdown: string;
  consentClause: string;
  createdAt: string;
  updatedAt: string;
}

export interface ContractTemplateRequest {
  companyId?: string;
  name: string;
  documentType: DocumentType;
  version?: number;
  isActive?: boolean;
  contentMarkdown: string;
  consentClause: string;
}

export interface ContractTemplateVariableInfo {
  tag: string;
  label: string;
  category: 'CUSTOMER' | 'COMPANY' | 'CONTRACT' | 'PLAN' | 'SIGNATURE';
  example: string;
  description: string;
}

export interface SignatureSession {
  id: string;
  contractId: string;
  templateId?: string;
  token: string;
  signatureUrl: string;
  status: SignatureStatus;
  symbolicAmount: number;
  pixTxid?: string;
  pixCopyPaste?: string;
  pixQrCodeBase64?: string;
  pixEndToEndId?: string;
  documentSha256Hash?: string;
  payerName?: string;
  payerCpfCnpj?: string;
  payerBankName?: string;
  rejectionReason?: string;
  signedPdfUrl?: string;
  fallbackMethod?: FallbackMethod;
  onboardingCreditAmount?: number;
  forensicCertificatePdfUrl?: string;
  expiresAt: string;
  signedAt?: string;
  createdAt: string;
}

export interface SignaturePublicView {
  token: string;
  contractName: string;
  customerName: string;
  customerDocumentMasked: string;
  companyName: string;
  renderedContent: string;
  consentClause: string;
  status: SignatureStatus;
  symbolicAmount: number;
  pixCopyPaste?: string;
  pixQrCodeBase64?: string;
  payerName?: string;
  payerBankName?: string;
  rejectionReason?: string;
  signedPdfUrl?: string;
  documentSha256Hash?: string;
  fallbackMethod?: FallbackMethod;
  onboardingCreditAmount?: number;
  forensicCertificatePdfUrl?: string;
  expiresAt: string;
  signedAt?: string;
}

export interface CreateSignatureSessionRequest {
  contractId: string;
  templateId?: string;
  symbolicAmount?: number;
}
