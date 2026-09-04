import { getWarehouses } from '../api/generated/endpoints/warehouses/warehouses';
import { getAssetCustody } from '../api/generated/endpoints/asset-custody/asset-custody';
import type {
  WarehouseResponse,
  WarehouseCreateRequest,
  SerializedAssetResponse,
  StockTransferResponse,
  CreateTransferRequest,
  DispatchTransferRequest,
  ConfirmReceiptTransferRequest,
  ToolCustodyAgreementResponse,
  CheckoutToolRequest,
  ReturnToolAgreementRequest,
  ReverseLogisticsRequest,
  AssetCategory,
  AssetStatus,
  CarrierType,
  TransferStatus,
  AgreementStatus,
} from '../api/generated/models';

export type Warehouse = WarehouseResponse;
export type SerializedAsset = SerializedAssetResponse;
export type StockTransfer = StockTransferResponse;
export type ToolCustodyAgreement = ToolCustodyAgreementResponse;

export type {
  WarehouseResponse,
  WarehouseCreateRequest,
  SerializedAssetResponse,
  StockTransferResponse,
  CreateTransferRequest,
  DispatchTransferRequest,
  ConfirmReceiptTransferRequest,
  ToolCustodyAgreementResponse,
  CheckoutToolRequest,
  ReturnToolAgreementRequest,
  ReverseLogisticsRequest,
  AssetCategory,
  AssetStatus,
  CarrierType,
  TransferStatus,
  AgreementStatus,
};

const warehousesApi = getWarehouses();
const custodyApi = getAssetCustody();

export const inventoryCustodyService = {
  // Depósitos
  getAllWarehouses: async (): Promise<{ data: WarehouseResponse[] }> => {
    const data = await warehousesApi.getAllWarehouses();
    return { data };
  },

  getActiveWarehouses: async (): Promise<WarehouseResponse[]> => {
    return warehousesApi.getActiveWarehouses();
  },

  getWarehouseById: async (id: string): Promise<WarehouseResponse> => {
    return warehousesApi.getWarehouseById(id);
  },

  createWarehouse: async (data: WarehouseCreateRequest): Promise<WarehouseResponse> => {
    return warehousesApi.createWarehouse(data);
  },

  // Ativos Serializados
  getAllAssets: async (): Promise<{ data: SerializedAssetResponse[] }> => {
    const data = await custodyApi.getAllAssets();
    return { data };
  },

  getAssetsByWarehouse: async (warehouseId: string): Promise<{ data: SerializedAssetResponse[] }> => {
    const data = await custodyApi.getAssetsByWarehouse(warehouseId);
    return { data };
  },

  getAssetsByHolder: async (holderUserId: string): Promise<{ data: SerializedAssetResponse[] }> => {
    const data = await custodyApi.getAssetsByHolder(holderUserId);
    return { data };
  },

  // Transferências Intermunicipais
  getAllTransfers: async (): Promise<{ data: StockTransferResponse[] }> => {
    const data = await custodyApi.getAllTransfers();
    return { data };
  },

  createTransfer: async (data: CreateTransferRequest): Promise<StockTransferResponse> => {
    return custodyApi.createTransfer(data);
  },

  dispatchTransfer: async (
    transferId: string,
    data?: DispatchTransferRequest
  ): Promise<StockTransferResponse> => {
    return custodyApi.dispatchTransfer(transferId, data);
  },

  confirmReceiptTransfer: async (
    transferId: string,
    data?: ConfirmReceiptTransferRequest
  ): Promise<StockTransferResponse> => {
    return custodyApi.confirmReceiptTransfer(transferId, data);
  },

  // Termos de Cautela / Nota Promissória para Ferramental
  getAllToolAgreements: async (): Promise<{ data: ToolCustodyAgreementResponse[] }> => {
    const data = await custodyApi.getAllToolAgreements();
    return { data };
  },

  checkoutToolAgreement: async (data: CheckoutToolRequest): Promise<ToolCustodyAgreementResponse> => {
    return custodyApi.checkoutToolAgreement(data);
  },

  returnToolAgreement: async (
    agreementId: string,
    data?: ReturnToolAgreementRequest
  ): Promise<ToolCustodyAgreementResponse> => {
    return custodyApi.returnToolAgreement(agreementId, data);
  },

  // Logística Reversa de O.S.
  returnAssetFromWorkOrder: async (
    assetId: string,
    data: ReverseLogisticsRequest
  ): Promise<SerializedAssetResponse> => {
    return custodyApi.returnAssetFromWorkOrder(assetId, data);
  },
};

export default inventoryCustodyService;
