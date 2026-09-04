import { getContracts } from '../api/generated/endpoints/contracts/contracts';
import {
  ContractResponse,
  ContractCreateRequest,
  ContractUpdateRequest,
  ContractStatus,
} from '../api/generated/models';

const contractsApi = getContracts();

export const contractService = {
  // Chamadas oficiais geradas pelo Contrato OpenAPI (API-First)
  getAll: async (): Promise<ContractResponse[]> => {
    return contractsApi.getAllContracts();
  },

  getAllContracts: async (): Promise<ContractResponse[]> => {
    return contractsApi.getAllContracts();
  },

  getById: async (id: string): Promise<ContractResponse> => {
    return contractsApi.getContractById(id);
  },

  getByCustomerId: async (customerId: string): Promise<ContractResponse[]> => {
    return contractsApi.getContractsByCustomerId(customerId);
  },

  getContractsByCustomerId: async (customerId: string): Promise<ContractResponse[]> => {
    return contractsApi.getContractsByCustomerId(customerId);
  },

  getByStatus: async (status: ContractStatus): Promise<ContractResponse[]> => {
    return contractsApi.getContractsByStatus(status);
  },

  create: async (contractData: ContractCreateRequest | any): Promise<ContractResponse> => {
    return contractsApi.createContract(contractData);
  },

  update: async (id: string, contractData: ContractUpdateRequest | any): Promise<ContractResponse> => {
    return contractsApi.updateContract(id, contractData);
  },

  updateStatus: async (id: string, status: string): Promise<ContractResponse> => {
    const targetStatus = status as ContractStatus;
    return contractsApi.updateContractStatus(id, { status: targetStatus }, { status: targetStatus });
  },

  cancel: async (id: string): Promise<ContractResponse> => {
    const targetStatus: ContractStatus = 'CANCELLED';
    return contractsApi.updateContractStatus(id, { status: targetStatus }, { status: targetStatus });
  },
};

export default contractService;
