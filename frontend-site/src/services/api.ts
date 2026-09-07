import { config } from '../config';

export async function fetchJson<T>(endpoint: string): Promise<T | null> {
  const url = `${config.apiBaseUrl}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
    });

    if (!response.ok) {
      console.warn(`[API] Erro ao buscar ${endpoint}: HTTP ${response.status}`);
      return null;
    }

    return (await response.json()) as T;
  } catch (error) {
    console.warn(`[API] Falha de conexão com ${endpoint}:`, error);
    return null;
  }
}
