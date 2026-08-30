export type NfcomStatus = 'DRAFT' | 'AUTHORIZED' | 'REJECTED' | 'CANCELED';

export interface NfcomRecord {
  id: string;
  invoiceId: string;
  accessKey?: string;
  series: string;
  documentNumber: number;
  status: NfcomStatus;
  xmlAuthorized?: string;
  danfePdfUrl?: string;
  rejectionReason?: string;
  issuedAt?: string;
  createdAt?: string;
}

export interface FiscalCompany {
  id: string;
  companyName: string;
  tradingName: string;
  cnpj: string;
  stateRegistration: string;
  environment: 'HOMOLOGATION' | 'PRODUCTION';
  nfcomSeries: string;
  certificateExpiryDate?: string;
  createdAt?: string;
}
