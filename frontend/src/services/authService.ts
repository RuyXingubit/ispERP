import api from './api';
import { getAuth } from '../api/generated/endpoints/auth/auth';
import { LoginRequest, LoginResponse } from '../api/generated/models';

const authApi = getAuth();

export interface LoginCredentials {
  email?: string;
  username?: string;
  password?: string;
}

export interface AuthResponse {
  success: boolean;
  token?: string;
  username?: string;
  name?: string;
  id?: string;
  role?: string;
  message?: string;
}

export interface AuthUser {
  id?: string;
  username: string;
  name?: string;
  role: string;
  cpf?: string;
}

export const authService = {
  // Chamada oficial via cliente gerado pelo Orval
  login: async (credentials: LoginCredentials): Promise<AuthResponse> => {
    try {
      const username = credentials.email || credentials.username || '';
      const password = credentials.password || '';

      const data: LoginResponse = await authApi.login({
        username,
        password,
      });

      if (data.success && data.token) {
        localStorage.setItem('token', data.token);
        localStorage.setItem(
          'user',
          JSON.stringify({
            id: 'usr-1',
            username: data.username || username,
            name: data.username || username,
            role: data.role || 'USER',
          })
        );
        api.defaults.headers.common['Authorization'] = `Bearer ${data.token}`;
      }

      return data as AuthResponse;
    } catch (error) {
      console.error('Erro ao fazer login:', error);
      throw error;
    }
  },

  logout: (): void => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    delete api.defaults.headers.common['Authorization'];
  },

  isAuthenticated: (): boolean => {
    const token = localStorage.getItem('token');
    return !!token;
  },

  getToken: (): string | null => {
    return localStorage.getItem('token');
  },

  getUser: (): AuthUser | null => {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },

  setupAxiosToken: (): void => {
    const token = authService.getToken();
    if (token) {
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    }
  },
};

export default authService;
