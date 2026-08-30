import api from './api';
import {
  IpamAsn,
  IpamAsnRequest,
  IpamVrf,
  IpamVrfRequest,
  IpamSubnet,
  IpamSubnetRequest,
  IpamIpAddress,
  IpamIpAddressRequest,
  IpamSplitRequest,
  IpamSplitResponse,
  SubnetCalculationResult,
} from '../types/ipam';

export const ipamService = {
  // ASNs
  getAsns: async (): Promise<IpamAsn[]> => {
    const res = await api.get<IpamAsn[]>('/ipam/asns');
    return res.data;
  },

  createAsn: async (data: IpamAsnRequest): Promise<IpamAsn> => {
    const res = await api.post<IpamAsn>('/ipam/asns', data);
    return res.data;
  },

  updateAsn: async (id: string, data: IpamAsnRequest): Promise<IpamAsn> => {
    const res = await api.put<IpamAsn>(`/ipam/asns/${id}`, data);
    return res.data;
  },

  deleteAsn: async (id: string): Promise<void> => {
    await api.delete(`/ipam/asns/${id}`);
  },

  // VRFs
  getVrfs: async (): Promise<IpamVrf[]> => {
    const res = await api.get<IpamVrf[]>('/ipam/vrfs');
    return res.data;
  },

  createVrf: async (data: IpamVrfRequest): Promise<IpamVrf> => {
    const res = await api.post<IpamVrf>('/ipam/vrfs', data);
    return res.data;
  },

  updateVrf: async (id: string, data: IpamVrfRequest): Promise<IpamVrf> => {
    const res = await api.put<IpamVrf>(`/ipam/vrfs/${id}`, data);
    return res.data;
  },

  deleteVrf: async (id: string): Promise<void> => {
    await api.delete(`/ipam/vrfs/${id}`);
  },

  // Subnets
  getSubnets: async (): Promise<IpamSubnet[]> => {
    const res = await api.get<IpamSubnet[]>('/ipam/subnets');
    return res.data;
  },

  getSubnetById: async (id: string): Promise<IpamSubnet> => {
    const res = await api.get<IpamSubnet>(`/ipam/subnets/${id}`);
    return res.data;
  },

  createSubnet: async (data: IpamSubnetRequest): Promise<IpamSubnet> => {
    const res = await api.post<IpamSubnet>('/ipam/subnets', data);
    return res.data;
  },

  updateSubnet: async (id: string, data: IpamSubnetRequest): Promise<IpamSubnet> => {
    const res = await api.put<IpamSubnet>(`/ipam/subnets/${id}`, data);
    return res.data;
  },

  deleteSubnet: async (id: string): Promise<void> => {
    await api.delete(`/ipam/subnets/${id}`);
  },

  splitSubnet: async (data: IpamSplitRequest): Promise<IpamSplitResponse> => {
    const res = await api.post<IpamSplitResponse>('/ipam/subnets/split', data);
    return res.data;
  },

  // IP Addresses
  getIpsBySubnet: async (subnetId: string): Promise<IpamIpAddress[]> => {
    const res = await api.get<IpamIpAddress[]>(`/ipam/subnets/${subnetId}/ips`);
    return res.data;
  },

  createIpAddress: async (data: IpamIpAddressRequest): Promise<IpamIpAddress> => {
    const res = await api.post<IpamIpAddress>('/ipam/ips', data);
    return res.data;
  },

  updateIpAddress: async (id: string, data: IpamIpAddressRequest): Promise<IpamIpAddress> => {
    const res = await api.put<IpamIpAddress>(`/ipam/ips/${id}`, data);
    return res.data;
  },

  deleteIpAddress: async (id: string): Promise<void> => {
    await api.delete(`/ipam/ips/${id}`);
  },

  getNextAvailableIp: async (subnetId: string): Promise<string> => {
    const res = await api.get<{ nextAvailableIp: string }>(`/ipam/subnets/${subnetId}/next-available`);
    return res.data.nextAvailableIp;
  },

  // Calculator
  calculateCidr: async (cidr: string): Promise<SubnetCalculationResult> => {
    const res = await api.get<SubnetCalculationResult>('/ipam/calculate', { params: { cidr } });
    return res.data;
  },
};

export default ipamService;
