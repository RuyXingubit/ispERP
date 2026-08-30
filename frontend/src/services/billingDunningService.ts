import api from './api';

export const billingDunningService = {
  processDailyDunning: () => api.post('/billing/dunning/process'),
  executeCrossCredit: (futurePaidInvoiceId: string, overdueUnpaidInvoiceId: string) =>
    api.post('/billing/dunning/rebalance/cross-credit', null, {
      params: { futurePaidInvoiceId, overdueUnpaidInvoiceId },
    }),
  requestBotUnblock: (contractId: string) =>
    api.post('/billing/dunning/trust-unblock/bot', null, {
      params: { contractId },
    }),
  requestAttendantUnblock: (contractId: string, attendantUserId: string, reason: string) =>
    api.post('/billing/dunning/trust-unblock/attendant', null, {
      params: { contractId, attendantUserId, reason },
    }),
};

export default billingDunningService;
