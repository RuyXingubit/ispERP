import api from './api';

export const authService = {
  // Fazer login
  login: async (credentials) => {
    try {
      const response = await api.post('/auth/login', {
        username: credentials.email, // O backend espera username, mas enviamos email
        password: credentials.password
      });
      
      if (response.data.success && response.data.token) {
        // Salvar token no localStorage
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', JSON.stringify({
          username: response.data.username,
          role: response.data.role
        }));
        
        // Configurar token no axios para próximas requisições
        api.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;
      }
      
      return response.data;
    } catch (error) {
      console.error('Erro ao fazer login:', error);
      throw error;
    }
  },

  // Fazer logout
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    delete api.defaults.headers.common['Authorization'];
  },

  // Verificar se está autenticado
  isAuthenticated: () => {
    const token = localStorage.getItem('token');
    return !!token;
  },

  // Obter token
  getToken: () => {
    return localStorage.getItem('token');
  },

  // Obter dados do usuário
  getUser: () => {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },

  // Configurar token no axios (para quando a página recarregar)
  setupAxiosToken: () => {
    const token = authService.getToken();
    if (token) {
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    }
  }
};

// Configurar token automaticamente quando o serviço for importado
authService.setupAxiosToken();