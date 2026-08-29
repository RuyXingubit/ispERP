import api from './api';

export const billingDunningService = {
  processDailyDunning: () => api.post('/api/billing/dunning/process'),
  executeCrossCredit: (futurePaidInvoiceId, overdueUnpaidInvoiceId) =>
    api.post('/api/billing/dunning/rebalance/cross-credit', null, {
      params: { futurePaidInvoiceId, overdueUnpaidInvoiceId },
    }),
  requestBotUnblock: (contractId) =>
    api.post('/api/billing/dunning/trust-unblock/bot', null, {
      params: { contractId },
    }),
  requestAttendantUnblock: (contractId, attendantUserId, reason) =>
    api.post('/api/billing/dunning/trust-unblock/attendant', null, {
      params: { contractId, attendantUserId, reason },
    }),
};
