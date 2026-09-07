export interface SiteConfig {
  apiBaseUrl: string;
  customerAreaUrl: string;
  defaultTitle: string;
  defaultDescription: string;
  defaultPrimaryColor: string;
  defaultSecondaryColor: string;
}

export const config: SiteConfig = {
  // Em produção pode ser '/api' (com proxy reverso) ou URL absoluta da VPS
  apiBaseUrl: import.meta.env.VITE_API_URL || '/api',
  
  // URL para onde o botão "Área do Cliente" direciona o assinante
  customerAreaUrl: import.meta.env.VITE_CUSTOMER_AREA_URL || 'http://localhost:3001',
  
  // Valores default de fallback caso a API esteja indisponível
  defaultTitle: 'Provedor de Internet Fibra Óptica',
  defaultDescription: 'Conexão ultraveloz e estável com 100% fibra óptica até a sua casa ou empresa.',
  defaultPrimaryColor: '#0284c7',
  defaultSecondaryColor: '#10b981',
};
