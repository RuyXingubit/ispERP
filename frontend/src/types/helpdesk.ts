export type TicketPriority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING_CUSTOMER' | 'RESOLVED' | 'CANCELED';
export type TicketCategory = 'LENTIDAO' | 'SEM_CONEXAO' | 'FINANCEIRO' | 'MUDANCA_ENDERECO' | 'DUVIDAS';

export interface HelpdeskTicket {
  id: string;
  protocol: string;
  customerId: string;
  customerName?: string;
  contractId: string;
  category: string;
  priority: TicketPriority;
  status: TicketStatus;
  subject: string;
  description: string;
  slaExpiresAt?: string;
  resolvedAt?: string;
  createdAt?: string;
}

export interface TicketInteraction {
  id: string;
  ticketId: string;
  authorName: string;
  isInternal: boolean;
  message: string;
  createdAt: string;
}
