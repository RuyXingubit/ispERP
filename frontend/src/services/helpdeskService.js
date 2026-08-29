import api from './api';

export const helpdeskService = {
  getAllTickets: () => api.get('/api/helpdesk/tickets'),
  getTicketById: (id) => api.get(`/api/helpdesk/tickets/${id}`),
  getTicketByProtocol: (protocol) => api.get(`/api/helpdesk/tickets/protocol/${protocol}`),
  getTicketsByCustomer: (customerId) => api.get(`/api/helpdesk/tickets/customer/${customerId}`),
  createTicket: (data) => api.post('/api/helpdesk/tickets', data),
  escalateToN2: (id, data) => api.post(`/api/helpdesk/tickets/${id}/escalate-n2`, data),
  resolveByN2: (id, data) => api.post(`/api/helpdesk/tickets/${id}/resolve-n2`, data),
  escalateToWorkOrder: (id, data) => api.post(`/api/helpdesk/tickets/${id}/escalate-work-order`, data),
  getInteractions: (id, includeInternal = true) =>
    api.get(`/api/helpdesk/tickets/${id}/interactions`, { params: { includeInternal } }),
  addInteraction: (id, data) => api.post(`/api/helpdesk/tickets/${id}/interactions`, data),
  closeTicket: (id, data) => api.post(`/api/helpdesk/tickets/${id}/close`, data),
};
