export type IpamIpVersion = 'IPV4' | 'IPV6';

export type IpamSubnetStatus = 'ACTIVE' | 'RESERVED' | 'DEPRECATED';

export type IpamSubnetCategory =
  | 'CUSTOMER_ACCESS'
  | 'CGNAT'
  | 'MANAGEMENT'
  | 'INFRASTRUCTURE'
  | 'PTP'
  | 'LOOPBACK';

export type IpamAddressStatus =
  | 'AVAILABLE'
  | 'ALLOCATED'
  | 'RESERVED'
  | 'DHCP_POOL';

export type IpamAssignedToType =
  | 'CONTRACT'
  | 'NETWORK_DEVICE'
  | 'INFRASTRUCTURE'
  | 'CGNAT_POOL'
  | 'ROUTED_SUBNET';

export type IpamRir =
  | 'REGISTRO_BR'
  | 'LACNIC'
  | 'ARIN'
  | 'RIPE'
  | 'APNIC'
  | 'AFRINIC';

export interface IpamAsn {
  id: string;
  companyId?: string | null;
  asn: number;
  name: string;
  rir: IpamRir;
  description?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface IpamAsnRequest {
  companyId?: string | null;
  asn: number;
  name: string;
  rir: IpamRir;
  description?: string | null;
}

export interface IpamVrf {
  id: string;
  companyId?: string | null;
  name: string;
  rd?: string | null;
  description?: string | null;
  isDefault: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface IpamVrfRequest {
  companyId?: string | null;
  name: string;
  rd?: string | null;
  description?: string | null;
  isDefault: boolean;
}

export interface IpamSubnet {
  id: string;
  parentId?: string | null;
  vrfId?: string | null;
  vrfName?: string | null;
  asnId?: string | null;
  asnNumber?: number | null;
  companyId?: string | null;
  cidr: string;
  ipVersion: IpamIpVersion;
  networkAddress: string;
  broadcastAddress?: string | null;
  prefixLength: number;
  totalHosts: number;
  allocatedHosts?: number;
  utilizationPercentage?: number;
  isPool: boolean;
  poolName?: string | null;
  status: IpamSubnetStatus;
  category: IpamSubnetCategory;
  description?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface IpamSubnetRequest {
  parentId?: string | null;
  vrfId?: string | null;
  asnId?: string | null;
  companyId?: string | null;
  cidr: string;
  isPool?: boolean;
  poolName?: string | null;
  status?: IpamSubnetStatus;
  category?: IpamSubnetCategory;
  description?: string | null;
}

export interface IpamIpAddress {
  id: string;
  subnetId: string;
  subnetCidr: string;
  ipAddress: string;
  status: IpamAddressStatus;
  assignedToType?: IpamAssignedToType | null;
  assignedToId?: string | null;
  assignedToLabel?: string | null;
  dnsName?: string | null;
  description?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface IpamIpAddressRequest {
  subnetId: string;
  ipAddress: string;
  status?: IpamAddressStatus;
  assignedToType?: IpamAssignedToType | null;
  assignedToId?: string | null;
  dnsName?: string | null;
  description?: string | null;
}

export interface SubnetCalculationResult {
  cidr: string;
  ipVersion: IpamIpVersion;
  networkAddress: string;
  broadcastAddress?: string | null;
  netmask: string;
  wildcardMask?: string | null;
  firstUsableIp: string;
  lastUsableIp: string;
  prefixLength: number;
  totalHosts: number;
  usableHosts: number;
}

export interface IpamSplitRequest {
  subnetId: string;
  targetPrefixLength: number;
  createSubnets?: boolean;
}

export interface IpamSplitResponse {
  parentSubnetId: string;
  parentCidr: string;
  targetPrefixLength: number;
  totalSubnetsGenerated: number;
  generatedSubnets: SubnetCalculationResult[];
  persistedSubnets: IpamSubnet[];
}
