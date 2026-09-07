import { fetchJson } from './api';

export interface PlanItem {
  id: string;
  name: string;
  downloadSpeed: number;
  uploadSpeed: number;
  price: number;
  description?: string;
  svaIncluded?: string;
  suspensionDays?: number;
}

export async function getActivePlans(): Promise<PlanItem[]> {
  const result = await fetchJson<PlanItem[]>('/plans/active');
  return result ?? [];
}
