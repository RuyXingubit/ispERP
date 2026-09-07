import { fetchJson } from './api';

export interface SiteSettings {
  id?: string;
  siteTitle?: string;
  siteDescription?: string;
  primaryColor?: string;
  secondaryColor?: string;
  logoUrl?: string;
}

export interface Company {
  id?: string;
  name: string;
  document?: string;
  address?: string;
  phone?: string;
  email?: string;
  website?: string;
}

export async function getSiteSettings(): Promise<SiteSettings | null> {
  return fetchJson<SiteSettings>('/site-settings');
}

export async function getPrimaryCompany(): Promise<Company | null> {
  return fetchJson<Company>('/companies/primary');
}
