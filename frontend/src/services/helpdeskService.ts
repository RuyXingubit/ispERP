import api from './api';
import { HelpdeskTicket, TicketInteraction, TicketPriority, TicketCategory } from '../types/helpdesk';

export const helpdeskService = {
  getTickets: async (params?: {
    status?: string;
    priority?: TicketPriority;
    category?: TicketCategory;
    customerId?: string;
  }): Promise<HelpdeskTicket[]> => {
    const response = await api.get<HelpdeskTicket[]>('/helpdesk/tickets', { params });
    return response.data;
  },

  getAllTickets: async (params?: any): Promise<HelpdeskTicket[]> => {
    const response = await api.get<HelpdeskTicket[]>('/helpdesk/tickets', { params });
    return response.data;
  },

  getTicketsByCustomer: async (customerId: string): Promise<HelpdeskTicket[]> => {
    const response = await api.get<HelpdeskTicket[]>('/helpdesk/tickets', { params: { customerId } });
    return response.data;
  },

  getTicketById: async (id: string): Promise<HelpdeskTicket> => {
    const response = await api.get<HelpdeskTicket>(`/helpdesk/tickets/${id}`);
    return response.data;
  },

  getInteractions: async (ticketId: string): Promise<TicketInteraction[]> => {
    const response = await api.get<TicketInteraction[]>(`/helpdesk/tickets/${ticketId}/interactions`);
    return response.data;
  },

  createTicket: async (ticket: Partial<HelpdeskTicket> & Record<string, any>): Promise<HelpdeskTicket> => {
    const response = await api.post<HelpdeskTicket>('/helpdesk/tickets', ticket);
    return response.data;
  },

  addInteraction: async (
    ticketId: string,
    interaction: Partial<TicketInteraction> & Record<string, any>
  ): Promise<TicketInteraction> => {
    const response = await api.post<TicketInteraction>(
      `/helpdesk/tickets/${ticketId}/interactions`,
      interaction
    );
    return response.data;
  },

  resolveTicket: async (ticketId: string, resolutionNotes: string): Promise<HelpdeskTicket> => {
    const response = await api.post<HelpdeskTicket>(`/helpdesk/tickets/${ticketId}/resolve`, {
      resolutionNotes,
    });
    return response.data;
  },

  escalateToN2: async (ticketId: string, reason: string): Promise<HelpdeskTicket> => {
    const response = await api.post<HelpdeskTicket>(`/helpdesk/tickets/${ticketId}/escalate-n2`, { reason });
    return response.data;
  },

  resolveByN2: async (ticketId: string, notes: string): Promise<HelpdeskTicket> => {
    const response = await api.post<HelpdeskTicket>(`/helpdesk/tickets/${ticketId}/resolve-n2`, { notes });
    return response.data;
  },

  escalateToWorkOrder: async (ticketId: string, payload: Record<string, any>): Promise<any> => {
    const response = await api.post(`/helpdesk/tickets/${ticketId}/escalate-wo`, payload);
    return response.data;
  },

  closeTicket: async (ticketId: string): Promise<HelpdeskTicket> => {
    const response = await api.post<HelpdeskTicket>(`/helpdesk/tickets/${ticketId}/close`);
    return response.data;
  },
};

export default helpdeskService;
