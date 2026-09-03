import api from './api';

export interface SiteSettingsDto {
  id?: string;
  siteTitle: string;
  siteDescription?: string;
  primaryColor?: string;
  secondaryColor?: string;
  logoUrl?: string;
  contactEmail?: string;
  contactPhone?: string;
  supportEmail?: string;
  footerText?: string;
  createdAt?: string;
  updatedAt?: string;
}

export const siteSettingsService = {
  get: async (): Promise<SiteSettingsDto> => {
    const res = await api.get<SiteSettingsDto>('/site-settings');
    return res.data;
  },
  update: async (data: Partial<SiteSettingsDto>): Promise<SiteSettingsDto> => {
    const res = await api.put<SiteSettingsDto>('/site-settings', data);
    return res.data;
  },
};

export default siteSettingsService;
