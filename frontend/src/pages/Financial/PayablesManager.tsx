import React, { useState, useEffect } from 'react';
import { 
  FiFileText as Receipt, 
  FiPlus as Plus, 
  FiCalendar as Calendar, 
  FiCheckCircle as CheckCircle2, 
  FiClock as Clock, 
  FiDollarSign as DollarSign, 
  FiFileText as FileText, 
  FiSearch as Search, 
  FiChevronDown as ChevronDown, 
  FiChevronUp as ChevronUp, 
  FiX as X,
  FiCreditCard as CreditCard,
  FiHome as Building
} from 'react-icons/fi';
import { financialService } from '../../services/financialService';
import { 
  PayableInvoiceDto, 
  ExpenseInstallmentDto, 
  ChartOfAccountDto, 
  PayableInvoiceRequest,
  PayableStatus 
} from '../../types/financial';

export const PayablesManager: React.FC = () => {
  const [payables, setPayables] = useState<PayableInvoiceDto[]>([]);
  const [accounts, setAccounts] = useState<ChartOfAccountDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [expandedInvoiceId, setExpandedInvoiceId] = useState<string | null>(null);

  // Modal de nova conta a pagar
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [supplierName, setSupplierName] = useState('');
  const [supplierDoc, setSupplierDoc] = useState('');
  const [chartOfAccountId, setChartOfAccountId] = useState('');
  const [description, setDescription] = useState('');
  const [invoiceNumber, setInvoiceNumber] = useState('');
  const [totalAmount, setTotalAmount] = useState('');
  const [installmentsCount, setInstallmentsCount] = useState('1');
  const [firstDueDate, setFirstDueDate] = useState(() => {
    const d = new Date();
    d.setMonth(d.getMonth() + 1);
    return d.toISOString().split('T')[0];
  });
  const [notes, setNotes] = useState('');
  const [saving, setSaving] = useState(false);

  // Modal de quitação de parcela
  const [isPayModalOpen, setIsPayModalOpen] = useState(false);
  const [selectedInstallment, setSelectedInstallment] = useState<ExpenseInstallmentDto | null>(null);
  const [payAmount, setPayAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('TRANSFERENCIA_BANCARIA');
  const [receiptUrl, setReceiptUrl] = useState('');
  const [paying, setPaying] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [payablesData, accountsData] = await Promise.all([
        financialService.getPayables(),
        financialService.getAllAccountsFlat()
      ]);
      setPayables(payablesData);
      setAccounts(accountsData.filter(a => a.isAnalytical));
    } catch (err) {
      console.error('Erro ao carregar contas a pagar', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreatePayable = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      const req: PayableInvoiceRequest = {
        supplierName: supplierName.trim(),
        supplierDocument: supplierDoc.trim() || undefined,
        chartOfAccountId,
        description: description.trim(),
        invoiceNumber: invoiceNumber.trim() || undefined,
        totalAmount: parseFloat(totalAmount),
        installmentsCount: parseInt(installmentsCount, 10),
        firstDueDate,
        notes: notes.trim() || undefined
      };

      await financialService.createPayable(req);
      setIsModalOpen(false);
      resetForm();
      await loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao criar conta a pagar');
    } finally {
      setSaving(false);
    }
  };

  const resetForm = () => {
    setSupplierName('');
    setSupplierDoc('');
    setDescription('');
    setInvoiceNumber('');
    setTotalAmount('');
    setInstallmentsCount('1');
    setNotes('');
  };

  const openPayModal = (installment: ExpenseInstallmentDto) => {
    setSelectedInstallment(installment);
    setPayAmount(installment.amount.toString());
    setReceiptUrl('');
    setIsPayModalOpen(true);
  };

  const handlePayInstallment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedInstallment) return;

    try {
      setPaying(true);
      await financialService.payInstallment(selectedInstallment.id, {
        paidAmount: parseFloat(payAmount),
        paymentMethod,
        receiptUrl: receiptUrl.trim() || undefined
      });
      setIsPayModalOpen(false);
      await loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao quitar parcela');
    } finally {
      setPaying(false);
    }
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
  };

  const getStatusBadge = (status: PayableStatus) => {
    switch (status) {
      case 'PAID':
        return <span className="px-2 py-0.5 rounded-full text-xs bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">Pago</span>;
      case 'PARTIALLY_PAID':
        return <span className="px-2 py-0.5 rounded-full text-xs bg-amber-500/10 text-amber-400 border border-amber-500/20">Parcial</span>;
      case 'PENDING':
        return <span className="px-2 py-0.5 rounded-full text-xs bg-rose-500/10 text-rose-400 border border-rose-500/20">Pendente</span>;
      default:
        return <span className="px-2 py-0.5 rounded-full text-xs bg-slate-800 text-slate-400">Cancelado</span>;
    }
  };

  // Métricas
  const totalPendingAmount = payables
    .filter(p => p.status === 'PENDING' || p.status === 'PARTIALLY_PAID')
    .reduce((acc, p) => acc + (p.totalAmount || 0), 0);

  const filteredPayables = payables.filter(p => 
    p.supplierName.toLowerCase().includes(search.toLowerCase()) ||
    p.description.toLowerCase().includes(search.toLowerCase()) ||
    (p.invoiceNumber && p.invoiceNumber.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6 border-b border-slate-800 pb-5">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400">
            <Receipt size={24} />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight text-white">Contas a Pagar & Despesas</h1>
            <p className="text-xs text-slate-400">Gestão de fornecedores, CAPEX e parcelamento de dívidas</p>
          </div>
        </div>

        <button
          onClick={() => setIsModalOpen(true)}
          className="flex items-center gap-2 bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold px-4 py-2.5 rounded-xl shadow-lg shadow-rose-950/30 transition-all cursor-pointer"
        >
          <Plus size={16} />
          Nova Despesa / Compra
        </button>
      </div>

      {/* Cards de Métricas */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-4 backdrop-blur-sm">
          <span className="text-xs text-slate-400 block mb-1">Total a Pagar (Em Aberto)</span>
          <span className="text-xl font-bold text-rose-400">{formatCurrency(totalPendingAmount)}</span>
        </div>
        <div className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-4 backdrop-blur-sm">
          <span className="text-xs text-slate-400 block mb-1">Contas Cadastradas</span>
          <span className="text-xl font-bold text-white">{payables.length}</span>
        </div>
        <div className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-4 backdrop-blur-sm">
          <span className="text-xs text-slate-400 block mb-1">Contas Quitadas</span>
          <span className="text-xl font-bold text-emerald-400">
            {payables.filter(p => p.status === 'PAID').length}
          </span>
        </div>
      </div>

      {/* Busca */}
      <div className="flex items-center gap-2 w-full sm:w-80 relative mb-6">
        <Search size={16} className="absolute left-3 text-slate-500" />
        <input
          type="text"
          placeholder="Buscar por fornecedor ou descrição..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-9 pr-4 py-2 bg-slate-900/60 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-rose-500/50 transition-colors"
        />
      </div>

      {/* Lista de Contas a Pagar */}
      <div className="space-y-3">
        {loading ? (
          <div className="py-20 text-center text-xs text-slate-500">Carregando contas a pagar...</div>
        ) : filteredPayables.length === 0 ? (
          <div className="py-20 text-center text-xs text-slate-500">Nenhuma conta encontrada.</div>
        ) : (
          filteredPayables.map((item) => {
            const isExpanded = expandedInvoiceId === item.id;
            return (
              <div 
                key={item.id}
                className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-4 transition-all shadow-md backdrop-blur-sm"
              >
                <div 
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 cursor-pointer"
                  onClick={() => setExpandedInvoiceId(isExpanded ? null : item.id)}
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-xl bg-slate-800/60 text-slate-300">
                      <Building size={18} />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-sm text-white">{item.supplierName}</span>
                        {getStatusBadge(item.status)}
                      </div>
                      <p className="text-xs text-slate-400 mt-0.5">{item.description}</p>
                      <div className="flex items-center gap-3 text-[11px] text-slate-500 mt-1">
                        <span>Conta: {item.chartOfAccountCode} - {item.chartOfAccountName}</span>
                        {item.invoiceNumber && <span>NF: {item.invoiceNumber}</span>}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between sm:justify-end gap-4 border-t sm:border-t-0 border-slate-800 pt-2 sm:pt-0">
                    <div className="text-right">
                      <span className="text-sm font-bold text-white block">
                        {formatCurrency(item.totalAmount)}
                      </span>
                      <span className="text-[11px] text-slate-400">
                        {item.installments?.length || 1} parcela(s)
                      </span>
                    </div>

                    <button className="p-1 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition-colors">
                      {isExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                    </button>
                  </div>
                </div>

                {/* Detalhes das Parcelas Expansíveis */}
                {isExpanded && item.installments && item.installments.length > 0 && (
                  <div className="mt-4 pt-4 border-t border-slate-800/80">
                    <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">
                      Cronograma de Amortização das Parcelas
                    </h4>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2.5">
                      {item.installments.map((inst) => (
                        <div 
                          key={inst.id}
                          className="bg-slate-950/60 border border-slate-800/60 rounded-xl p-3 flex items-center justify-between gap-2"
                        >
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-mono text-xs font-semibold text-slate-300">
                                #{inst.installmentNumber}/{inst.totalInstallments}
                              </span>
                              {getStatusBadge(inst.status)}
                            </div>
                            <span className="text-xs font-bold text-white block mt-1">
                              {formatCurrency(inst.amount)}
                            </span>
                            <span className="text-[11px] text-slate-500">
                              Vencimento: {new Date(inst.dueDate).toLocaleDateString('pt-BR')}
                            </span>
                          </div>

                          {inst.status === 'PENDING' && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                openPayModal(inst);
                              }}
                              className="px-3 py-1.5 bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-400 border border-emerald-500/30 rounded-lg text-xs font-medium transition-colors cursor-pointer"
                            >
                              Dar Baixa
                            </button>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* Modal de Criação de Conta a Pagar */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-xs p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800 mb-5">
              <div className="flex items-center gap-2">
                <Receipt size={18} className="text-rose-400" />
                <h3 className="font-semibold text-sm text-white">Nova Conta a Pagar / CAPEX</h3>
              </div>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleCreatePayable} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Fornecedor</label>
                  <input
                    type="text"
                    required
                    placeholder="Ex: Prysmian Fibras"
                    value={supplierName}
                    onChange={(e) => setSupplierName(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-rose-500"
                  />
                </div>

                <div>
                  <label className="block text-xs text-slate-400 mb-1">CNPJ / CPF</label>
                  <input
                    type="text"
                    placeholder="Ex: 00.000.000/0001-00"
                    value={supplierDoc}
                    onChange={(e) => setSupplierDoc(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-rose-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">Conta Contábil (Plano de Contas)</label>
                <select
                  required
                  value={chartOfAccountId}
                  onChange={(e) => setChartOfAccountId(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-rose-500"
                >
                  <option value="">Selecione uma conta analítica...</option>
                  {accounts.map(acc => (
                    <option key={acc.id} value={acc.id}>
                      {acc.code} - {acc.name} ({acc.accountType})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">Descrição</label>
                <input
                  type="text"
                  required
                  placeholder="Ex: Compra de 10 bobinas de drop 1km e 500 conectores"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-rose-500"
                />
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Valor Total (R$)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    placeholder="0.00"
                    value={totalAmount}
                    onChange={(e) => setTotalAmount(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-rose-500"
                  />
                </div>

                <div>
                  <label className="block text-xs text-slate-400 mb-1">Nº Parcelas</label>
                  <input
                    type="number"
                    min="1"
                    max="36"
                    required
                    value={installmentsCount}
                    onChange={(e) => setInstallmentsCount(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-rose-500"
                  />
                </div>

                <div>
                  <label className="block text-xs text-slate-400 mb-1">1º Vencimento</label>
                  <input
                    type="date"
                    required
                    value={firstDueDate}
                    onChange={(e) => setFirstDueDate(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-rose-500"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-800 mt-6">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-rose-950/40 disabled:opacity-50"
                >
                  {saving ? 'Gravando...' : 'Lançar Despesa'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal de Baixa de Parcela */}
      {isPayModalOpen && selectedInstallment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-xs p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-sm p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800 mb-5">
              <div className="flex items-center gap-2">
                <CreditCard size={18} className="text-emerald-400" />
                <h3 className="font-semibold text-sm text-white">
                  Quitar Parcela #{selectedInstallment.installmentNumber}
                </h3>
              </div>
              <button onClick={() => setIsPayModalOpen(false)} className="text-slate-400 hover:text-white">
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handlePayInstallment} className="space-y-4">
              <div>
                <label className="block text-xs text-slate-400 mb-1">Valor Pago (R$)</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={payAmount}
                  onChange={(e) => setPayAmount(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">Método de Pagamento</label>
                <select
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-emerald-500"
                >
                  <option value="TRANSFERENCIA_BANCARIA">Transferência / TED</option>
                  <option value="PIX">Pix Corporativo</option>
                  <option value="BOLETO_BANCARIO">Boleto Pago no Banco</option>
                  <option value="CARTAO_CORPORATIVO">Cartão de Crédito Corporativo</option>
                </select>
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">URL ou Hash do Comprovante</label>
                <input
                  type="text"
                  placeholder="https://storage... ou Nº Autenticação"
                  value={receiptUrl}
                  onChange={(e) => setReceiptUrl(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-800 mt-6">
                <button
                  type="button"
                  onClick={() => setIsPayModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={paying}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-emerald-950/40 disabled:opacity-50"
                >
                  {paying ? 'Processando...' : 'Confirmar Quitação'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
