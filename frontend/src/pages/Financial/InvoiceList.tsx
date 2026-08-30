import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Alert,
  Tabs,
  Tab,
  Tooltip,
  Snackbar,
  InputAdornment
} from '@mui/material';
import {
  QrCode2 as QrCodeIcon,
  CheckCircle as PaidIcon,
  HourglassEmpty as PendingIcon,
  ErrorOutline as OverdueIcon,
  Cancel as CancelIcon,
  ContentCopy as CopyIcon,
  Autorenew as SyncIcon,
  AttachMoney as MoneyIcon,
  Receipt as ReceiptIcon,
  SwapHoriz as RebalanceIcon,
  NotificationsActive as DunningIcon,
  LockOpen as UnblockIcon,
  Add as AddIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';
import invoiceService from '../../services/invoiceService';
import { billingDunningService } from '../../services/billingDunningService';
import { contractService } from '../../services/contractService';
import { customerService } from '../../services/customerService';
import { useAuth } from '../../contexts/AuthContext';
import { toast } from 'react-toastify';

const statusConfig = {
  PENDING: { label: 'Aguardando Pagamento', color: 'warning', icon: <PendingIcon fontSize="small" /> },
  PAID: { label: 'Pago / Liquidado', color: 'success', icon: <PaidIcon fontSize="small" /> },
  OVERDUE: { label: 'Vencido / Em Atraso', color: 'error', icon: <OverdueIcon fontSize="small" /> },
  CANCELED: { label: 'Cancelado', color: 'default', icon: <CancelIcon fontSize="small" /> },
};

const InvoiceList = () => {
  const { user } = useAuth();
  const [currentTab, setCurrentTab] = useState(0);
  const [invoices, setInvoices] = useState([]);
  const [contracts, setContracts] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filtros
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  // Modais
  const [pixModalOpen, setPixModalOpen] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [copied, setCopied] = useState(false);

  const [newInvoiceModalOpen, setNewInvoiceModalOpen] = useState(false);
  const [newInvoiceForm, setNewInvoiceForm] = useState({
    contractId: '',
    dueDate: new Date(Date.now() + 10 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  });

  const [unblockModalOpen, setUnblockModalOpen] = useState(false);
  const [unblockForm, setUnblockForm] = useState({
    contractId: '',
    reason: 'Solicitação do assinante via WhatsApp / Central de Atendimento'
  });

  // Rebalanceamento Cruzado State
  const [selectedRebalanceContract, setSelectedRebalanceContract] = useState('');
  const [rebalanceFutureInvoiceId, setRebalanceFutureInvoiceId] = useState('');
  const [rebalanceOverdueInvoiceId, setRebalanceOverdueInvoiceId] = useState('');
  const [rebalanceLoading, setRebalanceLoading] = useState(false);

  // Dunning State
  const [dunningProcessing, setDunningProcessing] = useState(false);
  const [dunningResult, setDunningResult] = useState(null);

  const loadData = async () => {
    try {
      setLoading(true);
      const [invRes, conRes, custRes]: any[] = await Promise.all([
        invoiceService.getAllInvoices(),
        contractService.getAllContracts().catch(() => []),
        customerService.getAll().catch(() => [])
      ]);

      setInvoices(Array.isArray(invRes) ? invRes : (invRes?.data || []));
      setContracts(Array.isArray(conRes) ? conRes : (conRes?.data || []));
      setCustomers(Array.isArray(custRes) ? custRes : (custRes?.data || []));
      setError(null);
    } catch (err) {
      console.error('Erro ao carregar faturas:', err);
      setError('Erro ao carregar dados financeiros.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const getCustomerName = (customerId) => {
    if (!customerId) return 'Cliente N/D';
    const c = customers.find((item) => item.id === customerId);
    return c ? c.name : 'Cliente #' + customerId.substring(0, 8);
  };

  const getContractNumber = (contractId) => {
    if (!contractId) return 'N/D';
    const c = contracts.find((item) => item.id === contractId);
    return c ? c.contractNumber : 'CTR-' + contractId.substring(0, 8);
  };

  const handleOpenPix = (invoice) => {
    setSelectedInvoice(invoice);
    setCopied(false);
    setPixModalOpen(true);
  };

  const handleCopyPix = () => {
    if (selectedInvoice?.pixCopiaECola) {
      navigator.clipboard.writeText(selectedInvoice.pixCopiaECola);
      setCopied(true);
      toast.success('Código Pix Copia e Cola copiado com sucesso!');
      setTimeout(() => setCopied(false), 3000);
    }
  };

  const handleMarkAsPaid = async (id) => {
    if (window.confirm('Deseja confirmar a baixa manual desta fatura no sistema?')) {
      try {
        await invoiceService.markAsPaid(id);
        toast.success('Fatura baixada com sucesso!');
        loadData();
      } catch (err) {
        toast.error('Erro ao dar baixa na fatura.');
      }
    }
  };

  const handleCancelInvoice = async (id) => {
    if (window.confirm('Tem certeza de que deseja cancelar esta fatura?')) {
      try {
        await invoiceService.cancelInvoice(id);
        toast.success('Fatura cancelada com sucesso.');
        loadData();
      } catch (err) {
        toast.error('Erro ao cancelar fatura.');
      }
    }
  };

  const handleCreateManualInvoice = async (e) => {
    e.preventDefault();
    if (!newInvoiceForm.contractId) {
      toast.warning('Selecione um contrato para emitir a fatura.');
      return;
    }
    try {
      await invoiceService.generateInvoiceManually({
        contractId: newInvoiceForm.contractId,
        dueDate: newInvoiceForm.dueDate,
      } as any);
      toast.success('Fatura gerada com sucesso para o contrato!');
      setNewInvoiceModalOpen(false);
      loadData();
    } catch (err: any) {
      toast.error('Erro ao gerar fatura: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleTriggerRecurring = async () => {
    try {
      setLoading(true);
      const res: any = await invoiceService.triggerRecurringBilling();
      toast.success(res?.data || 'Rotina de faturamento recorrente executada com sucesso!');
      loadData();
    } catch (err) {
      toast.error('Erro ao executar faturamento recorrente.');
    } finally {
      setLoading(false);
    }
  };

  const handleProcessDailyDunning = async () => {
    try {
      setDunningProcessing(true);
      const res: any = await billingDunningService.processDailyDunning();
      setDunningResult(res.data);
      toast.success(`Régua de cobrança executada! ${res.data?.suspendedCount || 0} contratos suspensos por inadimplência.`);
      loadData();
    } catch (err) {
      toast.error('Erro ao processar régua de cobrança.');
    } finally {
      setDunningProcessing(false);
    }
  };

  const handleExecuteCrossCredit = async (e) => {
    e.preventDefault();
    if (!rebalanceFutureInvoiceId || !rebalanceOverdueInvoiceId) {
      toast.warning('Selecione a fatura futura paga e a fatura atrasada.');
      return;
    }
    try {
      setRebalanceLoading(true);
      await billingDunningService.executeCrossCredit(rebalanceFutureInvoiceId, rebalanceOverdueInvoiceId);
      toast.success('Compensação cruzada realizada! Fatura atrasada quitada e avisos fixos gravados.');
      setRebalanceFutureInvoiceId('');
      setRebalanceOverdueInvoiceId('');
      loadData();
    } catch (err: any) {
      toast.error('Erro na compensação cruzada: ' + (err.response?.data?.message || err.message));
    } finally {
      setRebalanceLoading(false);
    }
  };

  const handleExecuteAttendantUnblock = async (e) => {
    e.preventDefault();
    if (!unblockForm.contractId) {
      toast.warning('Selecione o contrato para desbloqueio.');
      return;
    }
    try {
      const res: any = await billingDunningService.requestAttendantUnblock(
        unblockForm.contractId,
        user?.id || 'attendant',
        unblockForm.reason
      );
      if (res.data?.allowed) {
        toast.success(`Desbloqueio em Confiança concedido com sucesso! Motivo: ${res.data.reason}`);
        setUnblockModalOpen(false);
        loadData();
      } else {
        toast.error(`Desbloqueio recusado pela política: ${res.data?.reason}`);
      }
    } catch (err) {
      toast.error('Erro ao solicitar desbloqueio em confiança.');
    }
  };

  // Filtragem
  const filteredInvoices = invoices.filter((inv) => {
    const matchStatus = filterStatus === 'ALL' || inv.status === filterStatus;
    const matchSearch =
      (inv.externalTransactionId && inv.externalTransactionId.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (inv.id && inv.id.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (getCustomerName(inv.customerId).toLowerCase().includes(searchTerm.toLowerCase())) ||
      (getContractNumber(inv.contractId).toLowerCase().includes(searchTerm.toLowerCase()));
    return matchStatus && matchSearch;
  });

  const totalPending = invoices
    .filter((i) => i.status === 'PENDING')
    .reduce((acc, curr) => acc + Number(curr.amount || 0), 0);

  const totalPaid = invoices
    .filter((i) => i.status === 'PAID')
    .reduce((acc, curr) => acc + Number(curr.paidAmount || curr.amount || 0), 0);

  const totalOverdue = invoices
    .filter((i) => i.status === 'OVERDUE')
    .reduce((acc, curr) => acc + Number(curr.amount || 0), 0);

  // Invoices para o Simulador de Rebalanceamento
  const contractInvoices = selectedRebalanceContract
    ? invoices.filter((i) => i.contractId === selectedRebalanceContract)
    : [];
  const futurePaidOptions = contractInvoices.filter((i) => i.status === 'PAID');
  const overdueUnpaidOptions = contractInvoices.filter((i) => i.status === 'OVERDUE' || i.status === 'PENDING');

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Gestão Financeira & Cobrança
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Emissão de faturas, Pix via Xingubit Pay, rebalanceamento contábil pro-rata e régua de cobrança automática.
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadData}
            disabled={loading}
            sx={{ borderRadius: 2 }}
          >
            Atualizar
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            onClick={() => setNewInvoiceModalOpen(true)}
            sx={{ borderRadius: 2 }}
          >
            Nova Fatura
          </Button>
        </Box>
      </Box>

      {/* Cards de Métricas */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderRadius: 3, borderLeft: '6px solid #ed6c02', bgcolor: '#fff8e1' }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                A RECEBER (PENDENTE)
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="warning.main" sx={{ mt: 0.5 }}>
                R$ {totalPending.toFixed(2).replace('.', ',')}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {invoices.filter((i) => i.status === 'PENDING').length} faturas a vencer
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderRadius: 3, borderLeft: '6px solid #d32f2f', bgcolor: '#ffebee' }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                INADIMPLÊNCIA (VENCIDO)
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="error.main" sx={{ mt: 0.5 }}>
                R$ {totalOverdue.toFixed(2).replace('.', ',')}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {invoices.filter((i) => i.status === 'OVERDUE').length} faturas em atraso
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderRadius: 3, borderLeft: '6px solid #2e7d32', bgcolor: '#e8f5e9' }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                RECEBIDO (LIQUIDADO)
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="success.main" sx={{ mt: 0.5 }}>
                R$ {totalPaid.toFixed(2).replace('.', ',')}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {invoices.filter((i) => i.status === 'PAID').length} faturas quitadas
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderRadius: 3, borderLeft: '6px solid #1976d2', bgcolor: '#e3f2fd' }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                TOTAL DE EMISSÕES
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="primary.main" sx={{ mt: 0.5 }}>
                {invoices.length} cobranças
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Base consolidada de faturas
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Tabs */}
      <Paper elevation={1} sx={{ mb: 3, borderRadius: 2 }}>
        <Tabs
          value={currentTab}
          onChange={(e, val) => setCurrentTab(val)}
          indicatorColor="primary"
          textColor="primary"
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab label="Todas as Faturas & Emissões" icon={<MoneyIcon />} iconPosition="start" />
          <Tab label="Compensação Cruzada (Rebalanceamento)" icon={<RebalanceIcon />} iconPosition="start" />
          <Tab label="Régua de Cobrança & Dunning" icon={<DunningIcon />} iconPosition="start" />
        </Tabs>
      </Paper>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {/* ABA 0: Todas as Faturas */}
      {currentTab === 0 && (
        <Box>
          <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
            <TextField
              placeholder="Buscar por cliente, contrato, TxID ou código..."
              size="small"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              sx={{ minWidth: 320, flexGrow: 1 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
            />
            <FormControl sx={{ minWidth: 220 }} size="small">
              <InputLabel id="filter-status-label">Filtrar por Status</InputLabel>
              <Select
                labelId="filter-status-label"
                value={filterStatus}
                label="Filtrar por Status"
                onChange={(e) => setFilterStatus(e.target.value)}
              >
                <MenuItem value="ALL">Todas as Faturas</MenuItem>
                <MenuItem value="PENDING">Aguardando Pagamento</MenuItem>
                <MenuItem value="PAID">Pagas / Liquidadas</MenuItem>
                <MenuItem value="OVERDUE">Vencidas / Em Atraso</MenuItem>
                <MenuItem value="CANCELED">Canceladas</MenuItem>
              </Select>
            </FormControl>
            <Button
              variant="outlined"
              color="primary"
              startIcon={<SyncIcon />}
              onClick={handleTriggerRecurring}
              sx={{ borderRadius: 2 }}
            >
              Faturamento Recorrente
            </Button>
          </Box>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', my: 6 }}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 2 }}>
              <Table>
                <TableHead sx={{ backgroundColor: '#f8f9fa' }}>
                  <TableRow>
                    <TableCell><strong>Identificador / Cliente</strong></TableCell>
                    <TableCell><strong>Status</strong></TableCell>
                    <TableCell><strong>Vencimento</strong></TableCell>
                    <TableCell><strong>Valor</strong></TableCell>
                    <TableCell><strong>Pagamento</strong></TableCell>
                    <TableCell align="right"><strong>Ações</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredInvoices.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                        Nenhuma fatura encontrada com os filtros selecionados.
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredInvoices.map((inv) => {
                      const conf = statusConfig[inv.status] || { label: inv.status, color: 'default' };
                      return (
                        <TableRow key={inv.id} hover>
                          <TableCell>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <MoneyIcon color="primary" />
                              <Box>
                                <Typography variant="body2" fontWeight="bold">
                                  {getCustomerName(inv.customerId)}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                  Contrato: {getContractNumber(inv.contractId)} | TxID: {inv.externalTransactionId || inv.id?.substring(0, 8)}
                                </Typography>
                              </Box>
                            </Box>
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={conf.label}
                              color={conf.color}
                              size="small"
                              icon={conf.icon}
                            />
                            {inv.paidByCrossCreditId && (
                              <Chip label="Compensação Cruzada" size="small" color="primary" variant="outlined" sx={{ ml: 0.5 }} />
                            )}
                            {inv.protectedAgainstSuspension && (
                              <Chip label="Protegido contra Corte" size="small" color="info" sx={{ ml: 0.5 }} />
                            )}
                            {inv.rebalanceNotice && (
                              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5, fontStyle: 'italic' }}>
                                ℹ️ {inv.rebalanceNotice}
                              </Typography>
                            )}
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2" fontWeight="500">
                              {new Date(inv.dueDate).toLocaleDateString('pt-BR')}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2" fontWeight="600" color="primary">
                              R$ {Number(inv.amount).toFixed(2).replace('.', ',')}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            {inv.paidAt ? (
                              <Box>
                                <Typography variant="caption" color="success.main" display="block" fontWeight="bold">
                                  Pago em {new Date(inv.paidAt).toLocaleDateString('pt-BR')}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                  Via {inv.paymentMethod}
                                </Typography>
                              </Box>
                            ) : (
                              <Typography variant="caption" color="text.secondary">
                                Aguardando compensação
                              </Typography>
                            )}
                          </TableCell>
                          <TableCell align="right">
                            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, flexWrap: 'wrap' }}>
                              {inv.pixCopiaECola && (
                                <Button
                                  size="small"
                                  variant="outlined"
                                  color="primary"
                                  startIcon={<QrCodeIcon />}
                                  onClick={() => handleOpenPix(inv)}
                                >
                                  Pix QR
                                </Button>
                              )}
                              {inv.nfcomKey && (
                                <Button
                                  size="small"
                                  variant="outlined"
                                  color="info"
                                  startIcon={<ReceiptIcon />}
                                  href={inv.nfcomPdfUrl || `https://pay.xingubit.com.br/v1/nfcom/${inv.nfcomKey}/danfe-pdf`}
                                  target="_blank"
                                >
                                  NFCom
                                </Button>
                              )}
                              {inv.status !== 'PAID' && inv.status !== 'CANCELED' && (
                                <Button
                                  size="small"
                                  variant="contained"
                                  color="success"
                                  onClick={() => handleMarkAsPaid(inv.id)}
                                >
                                  Baixa
                                </Button>
                              )}
                              {inv.status !== 'PAID' && inv.status !== 'CANCELED' && (
                                <IconButton
                                  size="small"
                                  color="error"
                                  onClick={() => handleCancelInvoice(inv.id)}
                                  title="Cancelar Fatura"
                                >
                                  <CancelIcon fontSize="small" />
                                </IconButton>
                              )}
                            </Box>
                          </TableCell>
                        </TableRow>
                      );
                    })
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Box>
      )}

      {/* ABA 1: Compensação Cruzada & Rebalanceamento Contábil */}
      {currentTab === 1 && (
        <Box>
          <Card sx={{ borderRadius: 3, mb: 3 }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Simulador de Compensação Cruzada (Pagamento Fora de Ordem)
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Quando o cliente paga uma fatura futura por engano enquanto uma anterior está em aberto, este recurso transfere o crédito para quitar a fatura vencida e reabre a futura sem juros ou multas, registrando notas explicativas em ambas.
              </Typography>

              <form onSubmit={handleExecuteCrossCredit}>
                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <TextField
                      select
                      fullWidth
                      label="Selecione o Contrato do Assinante *"
                      value={selectedRebalanceContract}
                      onChange={(e) => {
                        setSelectedRebalanceContract(e.target.value);
                        setRebalanceFutureInvoiceId('');
                        setRebalanceOverdueInvoiceId('');
                      }}
                      helperText="Escolha o contrato para inspecionar as faturas pagas e pendentes"
                    >
                      {contracts.map((c) => (
                        <MenuItem key={c.id} value={c.id}>
                          {c.contractNumber} - {getCustomerName(c.customerId)} (Mensalidade: R$ {Number(c.monthlyFee || 0).toFixed(2)})
                        </MenuItem>
                      ))}
                    </TextField>
                  </Grid>

                  {selectedRebalanceContract && (
                    <>
                      <Grid item xs={12} sm={6}>
                        <TextField
                          select
                          fullWidth
                          label="Fatura Futura Paga (Fonte do Crédito) *"
                          value={rebalanceFutureInvoiceId}
                          onChange={(e) => setRebalanceFutureInvoiceId(e.target.value)}
                          helperText="Fatura quitada cujo crédito será transferido"
                        >
                          {futurePaidOptions.length === 0 ? (
                            <MenuItem disabled value="">Nenhuma fatura paga encontrada</MenuItem>
                          ) : (
                            futurePaidOptions.map((inv) => (
                              <MenuItem key={inv.id} value={inv.id}>
                                Venc: {new Date(inv.dueDate).toLocaleDateString('pt-BR')} - R$ {Number(inv.amount).toFixed(2)} (Pago em {inv.paidAt ? new Date(inv.paidAt).toLocaleDateString('pt-BR') : 'N/A'})
                              </MenuItem>
                            ))
                          )}
                        </TextField>
                      </Grid>

                      <Grid item xs={12} sm={6}>
                        <TextField
                          select
                          fullWidth
                          label="Fatura Atrasada / Aberta a Quitar *"
                          value={rebalanceOverdueInvoiceId}
                          onChange={(e) => setRebalanceOverdueInvoiceId(e.target.value)}
                          helperText="Fatura pendente que receberá a quitação"
                        >
                          {overdueUnpaidOptions.length === 0 ? (
                            <MenuItem disabled value="">Nenhuma fatura pendente encontrada</MenuItem>
                          ) : (
                            overdueUnpaidOptions.map((inv) => (
                              <MenuItem key={inv.id} value={inv.id}>
                                Venc: {new Date(inv.dueDate).toLocaleDateString('pt-BR')} - R$ {Number(inv.amount).toFixed(2)} ({inv.status})
                              </MenuItem>
                            ))
                          )}
                        </TextField>
                      </Grid>

                      <Grid item xs={12}>
                        <Alert severity="info" sx={{ borderRadius: 2 }}>
                          💡 <strong>Regra de Rebalanceamento Pro-Rata:</strong> Ao confirmar, a fatura atrasada selecionada será marcada como <strong>PAGA</strong> e a fatura futura será reaberta com seu vencimento original sem penalidades.
                        </Alert>
                      </Grid>

                      <Grid item xs={12}>
                        <Button
                          type="submit"
                          variant="contained"
                          color="primary"
                          disabled={rebalanceLoading || !rebalanceFutureInvoiceId || !rebalanceOverdueInvoiceId}
                          startIcon={<RebalanceIcon />}
                          sx={{ borderRadius: 2, px: 4, py: 1.2, fontWeight: 'bold' }}
                        >
                          {rebalanceLoading ? 'Executando Compensação...' : 'Executar Compensação Cruzada'}
                        </Button>
                      </Grid>
                    </>
                  )}
                </Grid>
              </form>
            </CardContent>
          </Card>
        </Box>
      )}

      {/* ABA 2: Régua de Cobrança & Dunning */}
      {currentTab === 2 && (
        <Box>
          <Grid container spacing={3}>
            <Grid item xs={12} md={7}>
              <Card sx={{ borderRadius: 3, height: '100%' }}>
                <CardContent sx={{ p: 3 }}>
                  <Typography variant="h6" fontWeight="bold" gutterBottom>
                    Régua de Cobrança Automatizada (Dunning)
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                    Processa a política de bloqueio automático para inadimplência superior ao período de carência (15 dias após vencimento), ignorando clientes com proteção ativa ou pagamento em compensação.
                  </Typography>

                  <Box sx={{ p: 2, bgcolor: '#f8f9fa', borderRadius: 2, mb: 3 }}>
                    <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
                      Regras de Suspensão Ativas:
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      • <strong>Dia 1 ao 5:</strong> Envio de lembrete cordial via WhatsApp / E-mail.<br />
                      • <strong>Dia 10:</strong> Notificação formal de pré-suspensão.<br />
                      • <strong>Dia 15+:</strong> Suspensão automática de sinal na OLT/RADIUS.<br />
                      • <strong>Proteção:</strong> Faturas marcadas como <em>ProtectedAgainstSuspension</em> são ignoradas.
                    </Typography>
                  </Box>

                  <Button
                    variant="contained"
                    color="error"
                    size="large"
                    startIcon={<DunningIcon />}
                    onClick={handleProcessDailyDunning}
                    disabled={dunningProcessing}
                    sx={{ borderRadius: 2, fontWeight: 'bold', py: 1.5 }}
                  >
                    {dunningProcessing ? 'Processando Inadimplência...' : 'Executar Dunning Diário Agora'}
                  </Button>

                  {dunningResult && (
                    <Alert severity="success" sx={{ mt: 3, borderRadius: 2 }}>
                      Execução finalizada com sucesso em {new Date(dunningResult.processedAt).toLocaleString('pt-BR')}.<br />
                      <strong>Total de contratos suspensos:</strong> {dunningResult.suspendedCount}
                    </Alert>
                  )}
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={5}>
              <Card sx={{ borderRadius: 3, height: '100%' }}>
                <CardContent sx={{ p: 3 }}>
                  <Typography variant="h6" fontWeight="bold" gutterBottom>
                    Desbloqueio em Confiança (Atendente)
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Permite ao atendente liberar a conexão de um assinante suspenso por 48 horas mediante justificativa de pagamento.
                  </Typography>

                  <Button
                    variant="outlined"
                    color="primary"
                    fullWidth
                    size="large"
                    startIcon={<UnblockIcon />}
                    onClick={() => setUnblockModalOpen(true)}
                    sx={{ mt: 2, borderRadius: 2, py: 1.5, fontWeight: 'bold' }}
                  >
                    Conceder Desbloqueio Manual
                  </Button>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Box>
      )}

      {/* Modal: Nova Fatura Manual */}
      <Dialog open={newInvoiceModalOpen} onClose={() => setNewInvoiceModalOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleCreateManualInvoice}>
          <DialogTitle sx={{ bgcolor: '#1976d2', color: '#fff', fontWeight: 'bold' }}>
            Emitir Nova Fatura Avulsa
          </DialogTitle>
          <DialogContent sx={{ pt: 3 }}>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={12}>
                <TextField
                  select
                  fullWidth
                  label="Contrato do Assinante *"
                  required
                  value={newInvoiceForm.contractId}
                  onChange={(e) => setNewInvoiceForm({ ...newInvoiceForm, contractId: e.target.value })}
                >
                  {contracts.map((c) => (
                    <MenuItem key={c.id} value={c.id}>
                      {c.contractNumber} - {getCustomerName(c.customerId)} (R$ {Number(c.monthlyFee || 0).toFixed(2)})
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  type="date"
                  label="Data de Vencimento *"
                  required
                  InputLabelProps={{ shrink: true }}
                  value={newInvoiceForm.dueDate}
                  onChange={(e) => setNewInvoiceForm({ ...newInvoiceForm, dueDate: e.target.value })}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ p: 2 }}>
            <Button onClick={() => setNewInvoiceModalOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Emitir Fatura com Pix
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Modal: Desbloqueio em Confiança pelo Atendente */}
      <Dialog open={unblockModalOpen} onClose={() => setUnblockModalOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleExecuteAttendantUnblock}>
          <DialogTitle sx={{ bgcolor: '#1976d2', color: '#fff', fontWeight: 'bold' }}>
            Desbloqueio em Confiança (Atendente)
          </DialogTitle>
          <DialogContent sx={{ pt: 3 }}>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={12}>
                <TextField
                  select
                  fullWidth
                  label="Selecione o Contrato Suspenso *"
                  required
                  value={unblockForm.contractId}
                  onChange={(e) => setUnblockForm({ ...unblockForm, contractId: e.target.value })}
                >
                  {contracts.map((c) => (
                    <MenuItem key={c.id} value={c.id}>
                      {c.contractNumber} - {getCustomerName(c.customerId)} ({c.status})
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  label="Justificativa do Atendente *"
                  required
                  value={unblockForm.reason}
                  onChange={(e) => setUnblockForm({ ...unblockForm, reason: e.target.value })}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ p: 2 }}>
            <Button onClick={() => setUnblockModalOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Liberar Sinal (48 Horas)
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Modal Pix Copia e Cola / QR Code */}
      <Dialog open={pixModalOpen} onClose={() => setPixModalOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ bgcolor: '#1976d2', color: '#fff', fontWeight: 'bold' }}>
          Pagamento via Pix (Xingubit Pay)
        </DialogTitle>
        <DialogContent dividers sx={{ textAlign: 'center', pt: 3 }}>
          {selectedInvoice && (
            <Box sx={{ py: 1 }}>
              <Typography variant="h5" fontWeight="bold" color="primary" gutterBottom>
                R$ {Number(selectedInvoice.amount).toFixed(2).replace('.', ',')}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Vencimento: {new Date(selectedInvoice.dueDate).toLocaleDateString('pt-BR')} | TxID: {selectedInvoice.externalTransactionId || selectedInvoice.id?.substring(0, 8)}
              </Typography>

              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'center',
                  my: 2,
                  p: 2,
                  bgcolor: '#fff',
                  borderRadius: 2,
                  border: '1px solid #e0e0e0',
                  maxWidth: 220,
                  mx: 'auto'
                }}
              >
                <img
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(
                    selectedInvoice?.pixCopiaECola || '00020126580014br.gov.bcb.pix...'
                  )}`}
                  alt="QR Code Pix"
                  style={{ width: '100%', height: 'auto' }}
                />
              </Box>

              <TextField
                fullWidth
                multiline
                rows={3}
                label="Código Pix Copia e Cola"
                value={selectedInvoice.pixCopiaECola || ''}
                InputProps={{ readOnly: true }}
                sx={{ mt: 1 }}
              />

              {copied && <Alert severity="success" sx={{ mt: 2 }}>Código Pix copiado para a área de transferência!</Alert>}
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setPixModalOpen(false)}>Fechar</Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<CopyIcon />}
            onClick={handleCopyPix}
          >
            Copiar Código Pix
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default InvoiceList;
