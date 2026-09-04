import { getNas } from '../api/generated/endpoints/nas/nas';
import { getRadiusSessions } from '../api/generated/endpoints/radius-sessions/radius-sessions';
import type {
  NasRequest,
  RadiusDisconnectRequest,
  RadiusDisconnectResponse,
} from '../api/generated/models';
import type { Nas, RadiusSession } from '../types/radius';

const nasApi = getNas();
const sessionsApi = getRadiusSessions();

export const radiusService = {
  // NAS / BNGs
  getAllNas: async (): Promise<Nas[]> => {
    return (await nasApi.getAllNas()) as unknown as Nas[];
  },

  getNasById: async (id: string): Promise<Nas> => {
    return (await nasApi.getNasById(id)) as unknown as Nas;
  },

  createNas: async (data: NasRequest): Promise<Nas> => {
    return (await nasApi.createNas(data)) as unknown as Nas;
  },

  updateNas: async (id: string, data: NasRequest): Promise<Nas> => {
    return (await nasApi.updateNas(id, data)) as unknown as Nas;
  },

  deleteNas: async (id: string): Promise<void> => {
    await nasApi.deleteNas(id);
  },

  // Sessions
  getActiveSessions: async (): Promise<RadiusSession[]> => {
    return (await sessionsApi.getActiveSessions()) as unknown as RadiusSession[];
  },

  getSessionHistory: async (username: string): Promise<RadiusSession[]> => {
    return (await sessionsApi.getSessionHistory(username)) as unknown as RadiusSession[];
  },

  disconnectUser: async (data: RadiusDisconnectRequest): Promise<RadiusDisconnectResponse> => {
    return await sessionsApi.disconnectUser(data);
  },
};

export default radiusService;
