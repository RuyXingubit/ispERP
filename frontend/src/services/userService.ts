import { getUsers } from '../api/generated/endpoints/users/users';
import {
  UserResponse,
  UserCreateRequest,
  UserUpdateRequest,
  UserRole,
} from '../api/generated/models';

const usersApi = getUsers();

export type User = UserResponse;
export type { UserResponse, UserCreateRequest, UserUpdateRequest, UserRole };

export const userService = {
  // Chamadas oficiais via cliente gerado pelo Orval
  getAll: async (): Promise<UserResponse[]> => {
    return usersApi.getAllUsers();
  },

  getAllUsers: async (): Promise<{ data: UserResponse[] }> => {
    const data = await usersApi.getAllUsers();
    return { data };
  },

  getById: async (id: string): Promise<UserResponse> => {
    return usersApi.getUserById(id);
  },

  create: async (user: UserCreateRequest | any): Promise<UserResponse> => {
    return usersApi.createUser(user);
  },

  update: async (id: string, user: UserUpdateRequest | any): Promise<UserResponse> => {
    return usersApi.updateUser(id, user);
  },

  delete: async (id: string): Promise<void> => {
    return usersApi.deleteUser(id);
  },
};

export default userService;
