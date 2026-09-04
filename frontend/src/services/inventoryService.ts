import { getInventory } from '../api/generated/endpoints/inventory/inventory';
import type { InventoryItemResponse } from '../api/generated/models';

export type InventoryItem = InventoryItemResponse;
export type { InventoryItemResponse };

const inventoryApi = getInventory();

export const inventoryService = {
  // Chamada direta tipada
  getAll: async (): Promise<InventoryItemResponse[]> => {
    return inventoryApi.getAllInventoryItems();
  },

  // Compatibilidade com componentes que consomem res.data
  getAllItems: async (): Promise<{ data: InventoryItemResponse[] }> => {
    const data = await inventoryApi.getAllInventoryItems();
    return { data };
  },
};

export default inventoryService;
