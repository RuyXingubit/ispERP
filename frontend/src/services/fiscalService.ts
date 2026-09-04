import { getFiscal } from '../api/generated/endpoints/fiscal/fiscal';
import type {
  FiscalCompanySaveRequest,
  FiscalGatewayConfigResponse,
  NfcomRecordResponse,
  SendAccountingReportResponse,
} from '../api/generated/models';
import { FiscalCompany, NfcomRecord } from '../types/fiscal';

export interface CertificateUploadResponse {
  success: boolean;
  errorMessage?: string;
  expiresAt?: string;
}

const fiscalApi = getFiscal();

const mapToNfcomRecord = (rec: NfcomRecordResponse): NfcomRecord => {
  return {
    id: rec.id,
    invoiceId: rec.invoiceId || '',
    accessKey: rec.chaveAcesso || undefined,
    series: rec.serie,
    documentNumber: rec.numero,
    status: (rec.status as any) || 'DRAFT',
    xmlAuthorized: rec.xmlAutorizado || undefined,
    danfePdfUrl: rec.danfePdfUrl || undefined,
    rejectionReason: rec.motivoCancelamento || undefined,
    issuedAt: rec.dataAutorizacao || undefined,
    createdAt: rec.createdAt || undefined,
    ...(rec as any),
  };
};

const fiscalService = {
  getActiveCompany: async (): Promise<FiscalCompany> => {
    const comp = await fiscalApi.getActiveFiscalCompany();
    return comp as unknown as FiscalCompany;
  },

  saveCompany: async (companyData: Partial<FiscalCompany>): Promise<FiscalCompany> => {
    const saved = await fiscalApi.saveFiscalCompany(companyData as unknown as FiscalCompanySaveRequest);
    return saved as unknown as FiscalCompany;
  },

  uploadCertificate: async (
    companyId: string,
    file: File,
    password: string
  ): Promise<CertificateUploadResponse> => {
    const result = await fiscalApi.uploadFiscalCertificate(companyId, { file, password });
    return {
      success: result.success,
      errorMessage: result.errorMessage || undefined,
      expiresAt: result.validUntil || undefined,
    };
  },

  getActiveConfig: async (): Promise<FiscalGatewayConfigResponse> => {
    return fiscalApi.getActiveFiscalConfig();
  },

  emitNfcom: async (invoiceId: string): Promise<NfcomRecord> => {
    const record = await fiscalApi.emitNfcom(invoiceId);
    return mapToNfcomRecord(record);
  },

  getRecords: async (page = 0, size = 10): Promise<NfcomRecord[]> => {
    const response = await fiscalApi.listNfcomRecords({ page, size, sort: 'createdAt,desc' });
    const content = (response && response.content) ? response.content : (Array.isArray(response) ? response : []);
    return content.map(mapToNfcomRecord);
  },

  cancelNfcom: async (recordId: string, reason: string): Promise<NfcomRecord> => {
    const result = await fiscalApi.cancelNfcom(recordId, { reason });
    return {
      id: recordId,
      invoiceId: '',
      accessKey: result.chaveAcesso || undefined,
      series: '1',
      documentNumber: 0,
      status: 'CANCELED',
      rejectionReason: result.errorMessage || undefined,
      issuedAt: result.dataCancelamento || undefined,
    };
  },

  getConvenio115ExportUrl: (year: number, month: number): string => {
    return `/api/fiscal/convenio115/export?year=${year}&month=${month}`;
  },

  sendAccountingReport: async (year: number, month: number): Promise<SendAccountingReportResponse> => {
    return fiscalApi.sendAccountingReport({ year, month });
  },
};

export default fiscalService;
