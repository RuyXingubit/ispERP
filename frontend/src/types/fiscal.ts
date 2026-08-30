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
  id?: string;
  cnpj: string;
  razaoSocial: string;
  nomeFantasia?: string;
  inscricaoEstadual: string;
  inscricaoMunicipal?: string;
  cnaePrincipal: string;
  regimeTributario: 'SIMPLES_NACIONAL' | 'LUCRO_PRESUMIDO' | 'LUCRO_REAL';
  aliquotaIcms: number;
  aliquotaFust: number;
  aliquotaFunttel: number;
  aliquotaPis?: number;
  aliquotaCofins?: number;
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  uf: string;
  cep: string;
  codigoIbge: string;
  telefone?: string;
  emailFiscal?: string;
  nfcomAmbiente: 'HOMOLOGACAO' | 'PRODUCAO';
  nfcomSerie: string;
  nfcomProximoNumero: number;
  hasCertificate: boolean;
  certificateExpiresAt?: string;
  fiscalConfirmed?: boolean;
  fiscalConfirmedAt?: string;
  accountingName?: string;
  accountingEmails?: string;
  accountingSendDay?: number;
  accountingAutoSend?: boolean;
  accountingLastSentAt?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}
