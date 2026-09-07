// SPDX-License-Identifier: MIT
import { config } from '../config';

export class ApiError extends Error {
  constructor(public status: number, message: string, public data?: unknown) {
    super(message);
    this.name = 'ApiError';
  }
}

export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${config.apiBaseUrl}${endpoint}`;
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json');
  }

  // Se houver sessão de cliente salva, envia o X-Customer-Id automaticamente
  const savedCustomerId = sessionStorage.getItem('customer_id');
  if (savedCustomerId && !headers.has('X-Customer-Id')) {
    headers.set('X-Customer-Id', savedCustomerId);
  }

  try {
    const response = await fetch(url, { ...options, headers });

    if (!response.ok) {
      let errorMessage = `Erro HTTP ${response.status}`;
      let errorData: unknown = null;

      try {
        errorData = await response.json();
        if (typeof errorData === 'object' && errorData !== null) {
          const errObj = errorData as Record<string, unknown>;
          errorMessage = (errObj.message as string) || (errObj.error as string) || errorMessage;
        }
      } catch {
        // Resposta não é JSON
      }

      throw new ApiError(response.status, errorMessage, errorData);
    }

    // Se for 204 No Content
    if (response.status === 204) {
      return {} as T;
    }

    return await response.json();
  } catch (err: unknown) {
    if (err instanceof ApiError) {
      throw err;
    }
    const message = err instanceof Error ? err.message : 'Falha na comunicação com o servidor';
    throw new ApiError(0, message);
  }
}
