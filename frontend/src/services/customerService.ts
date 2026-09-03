import { getCustomers } from '../api/generated/endpoints/customers/customers';
import {
  CustomerResponse,
  CustomerCreateRequest,
  CustomerUpdateRequest,
} from '../api/generated/models';

const customersApi = getCustomers();

export const customerService = {
  // Chamadas oficiais geradas pelo Contrato OpenAPI (API-First)
  getAll: async (): Promise<CustomerResponse[]> => {
    return customersApi.getAllCustomers();
  },

  getAllCustomers: async (): Promise<{ data: CustomerResponse[] }> => {
    const data = await customersApi.getAllCustomers();
    return { data };
  },

  getActive: async (): Promise<CustomerResponse[]> => {
    return customersApi.getActiveCustomers();
  },

  getById: async (id: string): Promise<CustomerResponse> => {
    return customersApi.getCustomerById(id);
  },

  getByCpf: async (cpf: string): Promise<CustomerResponse> => {
    return customersApi.getCustomerByCpf(cpf);
  },

  create: async (customerData: CustomerCreateRequest | any): Promise<CustomerResponse> => {
    return customersApi.createCustomer(customerData);
  },

  update: async (id: string, customerData: CustomerUpdateRequest | any): Promise<CustomerResponse> => {
    return customersApi.updateCustomer(id, customerData);
  },

  delete: async (id: string): Promise<void> => {
    return customersApi.deleteCustomer(id);
  },

  search: async (query: string): Promise<CustomerResponse[]> => {
    return customersApi.searchCustomers({ q: query });
  },

  searchByName: async (name: string): Promise<CustomerResponse[]> => {
    return customersApi.searchCustomersByName({ name });
  },

  searchByCpf: async (cpf: string): Promise<CustomerResponse[]> => {
    return customersApi.searchCustomersByCpf({ cpf });
  },

  activate: async (id: string): Promise<void> => {
    return customersApi.activateCustomer(id);
  },

  deactivate: async (id: string): Promise<void> => {
    return customersApi.deactivateCustomer(id);
  },
};

export default customerService;
