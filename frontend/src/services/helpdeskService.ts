import { getHelpdesk } from '../api/generated/endpoints/helpdesk/helpdesk';
import type {
  HelpdeskTicketResponse,
  TicketInteractionResponse,
  TicketCreateRequest,
  TicketInteractionCreateRequest,
  EscalateN2Request,
  ResolveN2Request,
  EscalateWorkOrderRequest,
  CloseTicketRequest,
  TicketCategory,
  TicketPriority,
  TicketStatus,
  TicketChannel,
  InteractionSenderType,
} from '../api/generated/models';

export type HelpdeskTicket = HelpdeskTicketResponse;
export type TicketInteraction = TicketInteractionResponse;
export type {
  HelpdeskTicketResponse,
  TicketInteractionResponse,
  TicketCreateRequest,
  TicketInteractionCreateRequest,
  EscalateN2Request,
  ResolveN2Request,
  EscalateWorkOrderRequest,
  CloseTicketRequest,
  TicketCategory,
  TicketPriority,
  TicketStatus,
  TicketChannel,
  InteractionSenderType,
};

const helpdeskApi = getHelpdesk();

export const helpdeskService = {
  getAllTickets: async (): Promise<HelpdeskTicketResponse[]> => {
    return helpdeskApi.getAllTickets();
  },

  getTickets: async (params?: { customerId?: string }): Promise<HelpdeskTicketResponse[]> => {
    if (params?.customerId) {
      return helpdeskApi.getTicketsByCustomer(params.customerId);
    }
    return helpdeskApi.getAllTickets();
  },

  getTicketsByCustomer: async (customerId: string): Promise<HelpdeskTicketResponse[]> => {
    return helpdeskApi.getTicketsByCustomer(customerId);
  },

  getTicketById: async (id: string): Promise<HelpdeskTicketResponse> => {
    return helpdeskApi.getTicketById(id);
  },

  getTicketByProtocol: async (protocol: string): Promise<HelpdeskTicketResponse> => {
    return helpdeskApi.getTicketByProtocol(protocol);
  },

  createTicket: async (ticket: TicketCreateRequest): Promise<HelpdeskTicketResponse> => {
    return helpdeskApi.createTicket(ticket);
  },

  getInteractions: async (ticketId: string, includeInternal = true): Promise<TicketInteractionResponse[]> => {
    return helpdeskApi.getInteractions(ticketId, { includeInternal });
  },

  addInteraction: async (
    ticketId: string,
    interaction: TicketInteractionCreateRequest
  ): Promise<TicketInteractionResponse> => {
    return helpdeskApi.addInteraction(ticketId, interaction);
  },

  escalateToN2: async (ticketId: string, reason?: string, attendantName?: string): Promise<HelpdeskTicketResponse> => {
    return helpdeskApi.escalateToN2(ticketId, { reason, attendantName });
  },

  resolveByN2: async (ticketId: string, resolutionNotes?: string, n2Name?: string): Promise<HelpdeskTicketResponse> => {
    return helpdeskApi.resolveByN2(ticketId, { resolutionNotes, n2Name });
  },

  escalateToWorkOrder: async (ticketId: string, payload: EscalateWorkOrderRequest): Promise<any> => {
    return helpdeskApi.escalateToWorkOrder(ticketId, payload);
  },

  closeTicket: async (
    ticketId: string,
    closureNotes?: string,
    satisfactionRating?: number
  ): Promise<HelpdeskTicketResponse> => {
    return helpdeskApi.closeTicket(ticketId, { closureNotes, satisfactionRating });
  },
};

export default helpdeskService;
