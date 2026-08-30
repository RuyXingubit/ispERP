import api from './api';

export interface User {
  id?: string;
  username: string;
  name?: string;
  email?: string;
  cpf?: string;
  role: string;
  active?: boolean;
}

export const userService = {
  getAll: async (): Promise<User[]> => {
    const res = await api.get<User[]>('/users');
    return res.data;
  },
  getAllUsers: async () => {
    return api.get<User[]>('/users');
  },
  create: (user: Partial<User>) => api.post<User>('/users', user),
};

export default userService;
