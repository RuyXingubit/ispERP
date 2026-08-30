import api from './api';

export interface NotificationConfig {
  id?: string;
  channel: 'WHATSAPP' | 'SMS' | 'EMAIL';
  provider: string;
  apiUrl?: string;
  apiKey?: string;
  senderNumber?: string;
  active?: boolean;
}

export const notificationConfigService = {
  getAll: async () => {
    const res = await api.get<NotificationConfig[]>('/notifications/configs');
    return res.data;
  },
  getById: (id: string) => api.get<NotificationConfig>(`/notifications/configs/${id}`),
  save: (config: Partial<NotificationConfig>) => api.post<NotificationConfig>('/notifications/configs', config),
  create: (config: Partial<NotificationConfig>) => api.post<NotificationConfig>('/notifications/configs', config),
  update: (id: string, config: Partial<NotificationConfig>) => api.put<NotificationConfig>(`/notifications/configs/${id}`, config),
  test: (id: string, destination: string) =>
    api.post(`/notifications/configs/${id}/test`, { destination }),
};

export default notificationConfigService;
