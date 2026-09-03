import api from './api';
import {
  CashCustody,
  CashTransferLog,
  BankDepositConfirmation,
  MaterialCustody,
  MaterialTransferLog
} from '../types/custody';

export const custodyService = {
  // Custódia de Dinheiro Vivo
  getAllCashCustodies: async (): Promise<CashCustody[]> => {
    const response = await api.get<CashCustody[]>('/financial/custody/cash/all');
    return response.data;
  },

  getCashCustodyByUserId: async (userId: string): Promise<CashCustody> => {
    const response = await api.get<CashCustody>(`/financial/custody/cash/user/${userId}`);
    return response.data;
  },

  recordCashSettlement: async (userId: string, data: { invoiceId: string; amount: number; receiptNumber?: string; notes?: string }): Promise<CashCustody> => {
    const response = await api.post<CashCustody>('/financial/custody/cash/settle', data, {
      headers: { 'X-User-Id': userId }
    });
    return response.data;
  },

  requestCashTransfer: async (senderUserId: string, data: { receiverUserId: string; amount: number; reason?: string; notes?: string }): Promise<CashTransferLog> => {
    const response = await api.post<CashTransferLog>('/financial/custody/cash/transfer/request', data, {
      headers: { 'X-User-Id': senderUserId }
    });
    return response.data;
  },

  respondCashTransfer: async (receiverUserId: string, transferId: string, accept: boolean): Promise<CashTransferLog> => {
    const response = await api.post<CashTransferLog>(`/financial/custody/cash/transfer/${transferId}/respond?accept=${accept}`, {}, {
      headers: { 'X-User-Id': receiverUserId }
    });
    return response.data;
  },

  getPendingCashTransfers: async (receiverUserId: string): Promise<CashTransferLog[]> => {
    const response = await api.get<CashTransferLog[]>('/financial/custody/cash/transfer/pending', {
      headers: { 'X-User-Id': receiverUserId }
    });
    return response.data;
  },

  submitBankDeposit: async (depositorUserId: string, data: { amount: number; bankName: string; bankAgency?: string; bankAccount?: string; receiptFileUrl: string; notes?: string }): Promise<BankDepositConfirmation> => {
    const response = await api.post<BankDepositConfirmation>('/financial/custody/cash/deposit', data, {
      headers: { 'X-User-Id': depositorUserId }
    });
    return response.data;
  },

  getPendingBankDeposits: async (): Promise<BankDepositConfirmation[]> => {
    const response = await api.get<BankDepositConfirmation[]>('/financial/custody/cash/deposit/pending');
    return response.data;
  },

  auditBankDeposit: async (auditorUserId: string, depositId: string, data: { approved: boolean; notes?: string; rejectionReason?: string }): Promise<BankDepositConfirmation> => {
    const response = await api.post<BankDepositConfirmation>(`/financial/custody/cash/deposit/${depositId}/audit`, data, {
      headers: { 'X-User-Id': auditorUserId }
    });
    return response.data;
  },

  // Custódia Material por CPF
  getMaterialsByUserId: async (userId: string): Promise<MaterialCustody[]> => {
    const response = await api.get<MaterialCustody[]>(`/financial/custody/materials/user/${userId}`);
    return response.data;
  },

  allocateMaterial: async (userId: string, data: Partial<MaterialCustody>): Promise<MaterialCustody> => {
    const response = await api.post<MaterialCustody>(`/financial/custody/materials/user/${userId}/allocate`, data);
    return response.data;
  },

  requestMaterialTransfer: async (senderUserId: string, data: { receiverUserId: string; materialCustodyId: string; quantity: number; notes?: string }): Promise<MaterialTransferLog> => {
    const response = await api.post<MaterialTransferLog>('/financial/custody/materials/transfer/request', data, {
      headers: { 'X-User-Id': senderUserId }
    });
    return response.data;
  },

  respondMaterialTransfer: async (receiverUserId: string, transferId: string, accept: boolean): Promise<MaterialTransferLog> => {
    const response = await api.post<MaterialTransferLog>(`/financial/custody/materials/transfer/${transferId}/respond?accept=${accept}`, {}, {
      headers: { 'X-User-Id': receiverUserId }
    });
    return response.data;
  },

  getPendingMaterialTransfers: async (receiverUserId: string): Promise<MaterialTransferLog[]> => {
    const response = await api.get<MaterialTransferLog[]>('/financial/custody/materials/transfer/pending', {
      headers: { 'X-User-Id': receiverUserId }
    });
    return response.data;
  }
};
