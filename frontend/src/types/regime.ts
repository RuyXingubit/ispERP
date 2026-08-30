export type FiscalRegimeTransitionStatus = 'SCHEDULED' | 'APPLIED' | 'CANCELLED';

export interface FiscalRegimeTransition {
  id?: string;
  companyId: string;
  previousRegime: string;
  newRegime: 'SIMPLES_NACIONAL' | 'LUCRO_PRESUMIDO' | 'LUCRO_REAL';
  effectiveDate: string;
  aliquotaIcms: number;
  aliquotaPis: number;
  aliquotaCofins: number;
  aliquotaFust: number;
  aliquotaFunttel: number;
  status: FiscalRegimeTransitionStatus;
  notes?: string;
  appliedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface FiscalRegimeTransitionRequest {
  companyId?: string;
  newRegime: 'SIMPLES_NACIONAL' | 'LUCRO_PRESUMIDO' | 'LUCRO_REAL';
  effectiveDate: string;
  aliquotaIcms?: number;
  aliquotaPis?: number;
  aliquotaCofins?: number;
  aliquotaFust?: number;
  aliquotaFunttel?: number;
  notes?: string;
}
