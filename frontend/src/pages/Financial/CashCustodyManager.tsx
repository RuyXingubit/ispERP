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
  Grid,
  IconButton,
  Paper,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
  Alert,
  MenuItem,
  InputAdornment
} from '@mui/material';
import {
  AccountBalanceWallet as WalletIcon,
  SwapHoriz as TransferIcon,
  AccountBalance as BankIcon,
  CheckCircle as CheckIcon,
  Cancel as RejectIcon,
  AttachMoney as MoneyIcon,
  Inventory2 as ToolIcon,
  Refresh as RefreshIcon,
  VerifiedUser as AuditIcon,
  OpenInNew as ExternalLinkIcon,
  Send as SendIcon
} from '@mui/icons-material';
import { custodyService } from '../../services/custodyService';
import {
  CashCustody,
  CashTransferLog,
  BankDepositConfirmation,
  MaterialCustody
} from '../../types/custody';

export const CashCustodyManager: React.FC = () => {
  const [activeTab, setActiveTab] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error' | 'info'; message: string } | null>(null);

  // Estados de dados
  const [custodies, setCustodies] = useState<CashCustody[]>([]);
  const [pendingTransfers, setPendingTransfers] = useState<CashTransferLog[]>([]);
  const [pendingDeposits, setPendingDeposits] = useState<BankDepositConfirmation[]>([]);
  const [selectedUserMaterials, setSelectedUserMaterials] = useState<MaterialCustody[]>([]);
  const [selectedUserIdForMaterials, setSelectedUserIdForMaterials] = useState<string>('');

  // Modais de Ação
  const [transferModalOpen, setTransferModalOpen] = useState<boolean>(false);
  const [depositModalOpen, setDepositModalOpen] = useState<boolean>(false);
  const [auditModalOpen, setAuditModalOpen] = useState<boolean>(false);
  const [selectedDepositForAudit, setSelectedDepositForAudit] = useState<BankDepositConfirmation | null>(null);

  // Formulários
  const [transferForm, setTransferForm] = useState({
    receiverUserId: '',
    amount: '',
    reason: 'Passagem de turno / fechamento de caixa',
    notes: ''
  });

  const [depositForm, setDepositForm] = useState({
    amount: '',
    bankName: 'Banco do Brasil',
    bankAgency: '0001-9',
    bankAccount: '12345-6',
    receiptFileUrl: '',
    notes: ''
  });

  const [auditForm, setAuditForm] = useState({
    notes: '',
    rejectionReason: ''
  });

  // Mock do usuário logado (usualmente provido pelo AuthContext)
  const currentUserId = '00000000-0000-0000-0000-000000000001';

  const formatBRL = (val: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0);
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const [allCustodies, allDeposits, myPendingTransfers] = await Promise.all([
        custodyService.getAllCashCustodies(),
        custodyService.getPendingBankDeposits(),
        custodyService.getPendingCashTransfers(currentUserId)
      ]);
      setCustodies(allCustodies);
      setPendingDeposits(allDeposits);
      setPendingTransfers(myPendingTransfers);

      if (allCustodies.length > 0 && !selectedUserIdForMaterials) {
        setSelectedUserIdForMaterials(allCustodies[0].userId);
        const mats = await custodyService.getMaterialsByUserId(allCustodies[0].userId);
        setSelectedUserMaterials(mats);
      }
    } catch (err: any) {
      console.error(err);
      setFeedback({ type: 'error', message: 'Erro ao carregar dados de custódia e caixa.' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleUserMaterialsChange = async (userId: string) => {
    setSelectedUserIdForMaterials(userId);
    try {
      const mats = await custodyService.getMaterialsByUserId(userId);
      setSelectedUserMaterials(mats);
    } catch (err) {
      console.error(err);
    }
  };

  const handleTransferSubmit = async () => {
    if (!transferForm.receiverUserId || !transferForm.amount) {
      setFeedback({ type: 'error', message: 'Preencha o colaborador recebedor e o valor.' });
      return;
    }
    setLoading(true);
    try {
      await custodyService.requestCashTransfer(currentUserId, {
        receiverUserId: transferForm.receiverUserId,
        amount: parseFloat(transferForm.amount),
        reason: transferForm.reason,
        notes: transferForm.notes
      });
      setFeedback({ type: 'success', message: 'Solicitação de transferência enviada! O valor só migra após o aceite do recebedor.' });
      setTransferModalOpen(false);
      setTransferForm({ receiverUserId: '', amount: '', reason: 'Passagem de turno / fechamento de caixa', notes: '' });
      await loadData();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err?.response?.data?.userMessage || 'Erro ao solicitar transferência.' });
    } finally {
      setLoading(false);
    }
  };

  const handleTransferResponse = async (transferId: string, accept: boolean) => {
    setLoading(true);
    try {
      await custodyService.respondCashTransfer(currentUserId, transferId, accept);
      setFeedback({
        type: accept ? 'success' : 'info',
        message: accept ? 'Transferência aceita com sucesso! O valor agora está sob sua custódia.' : 'Transferência recusada. O saldo permanece com o remetente.'
      });
      await loadData();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err?.response?.data?.userMessage || 'Erro ao processar transferência.' });
    } finally {
      setLoading(false);
    }
  };

  const handleDepositSubmit = async () => {
    if (!depositForm.amount || !depositForm.receiptFileUrl) {
      setFeedback({ type: 'error', message: 'Informe o valor e anexe o comprovante de depósito.' });
      return;
    }
    setLoading(true);
    try {
      await custodyService.submitBankDeposit(currentUserId, {
        amount: parseFloat(depositForm.amount),
        bankName: depositForm.bankName,
        bankAgency: depositForm.bankAgency,
        bankAccount: depositForm.bankAccount,
        receiptFileUrl: depositForm.receiptFileUrl,
        notes: depositForm.notes
      });
      setFeedback({ type: 'success', message: 'Comprovante enviado! O saldo só será liquidado após conciliação no extrato pelo CFO.' });
      setDepositModalOpen(false);
      setDepositForm({ amount: '', bankName: 'Banco do Brasil', bankAgency: '0001-9', bankAccount: '12345-6', receiptFileUrl: '', notes: '' });
      await loadData();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err?.response?.data?.userMessage || 'Erro ao submeter depósito.' });
    } finally {
      setLoading(false);
    }
  };

  const handleAuditDeposit = async (approved: boolean) => {
    if (!selectedDepositForAudit) return;
    setLoading(true);
    try {
      await custodyService.auditBankDeposit(currentUserId, selectedDepositForAudit.id, {
        approved,
        notes: auditForm.notes,
        rejectionReason: auditForm.rejectionReason
      });
      setFeedback({
        type: approved ? 'success' : 'info',
        message: approved ? 'Depósito conciliado com sucesso! Saldo do colaborador baixado.' : 'Depósito rejeitado. O colaborador permanece com saldo devedor.'
      });
      setAuditModalOpen(false);
      setSelectedDepositForAudit(null);
      setAuditForm({ notes: '', rejectionReason: '' });
      await loadData();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err?.response?.data?.userMessage || 'Erro ao auditar depósito.' });
    } finally {
      setLoading(false);
    }
  };

  const totalInCustody = custodies.reduce((acc, c) => acc + (c.currentBalance || 0), 0);

  return (
    <Box sx={{ p: 3, maxWidth: 1400, margin: '0 auto' }}>
      {/* Header Corporativo */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 700, color: '#1a202c' }}>
            Custódia & Blindagem de Caixa por CPF
          </Typography>
          <Typography variant="body2" sx={{ color: '#718096' }}>
            Rastreabilidade absoluta de dinheiro vivo e carga material de técnicos. Nenhum centavo sem dono.
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1.5 }}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={loadData}
            disabled={loading}
            size="small"
            sx={{ textTransform: 'none' }}
          >
            Atualizar
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<TransferIcon />}
            onClick={() => setTransferModalOpen(true)}
            size="small"
            sx={{ textTransform: 'none', fontWeight: 600 }}
          >
            Transferir Valores (Gaveta)
          </Button>
          <Button
            variant="contained"
            color="success"
            startIcon={<BankIcon />}
            onClick={() => setDepositModalOpen(true)}
            size="small"
            sx={{ textTransform: 'none', fontWeight: 600 }}
          >
            Prestar Contas (Depósito)
          </Button>
        </Box>
      </Box>

      {feedback && (
        <Alert severity={feedback.type} sx={{ mb: 2 }} onClose={() => setFeedback(null)}>
          {feedback.message}
        </Alert>
      )}

      {/* KPI Cards de Alta Densidade */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ borderColor: '#e2e8f0', borderRadius: 2 }}>
            <CardContent sx={{ py: 2 }}>
              <Typography variant="caption" sx={{ color: '#718096', fontWeight: 600, textTransform: 'uppercase' }}>
                Total sob Custódia na Empresa
              </Typography>
              <Typography variant="h5" sx={{ fontWeight: 800, color: '#2d3748', fontFamily: 'monospace', mt: 0.5 }}>
                {formatBRL(totalInCustody)}
              </Typography>
              <Typography variant="caption" sx={{ color: '#a0aec0' }}>
                Distribuído entre {custodies.length} colaboradores
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ borderColor: '#e2e8f0', borderRadius: 2 }}>
            <CardContent sx={{ py: 2 }}>
              <Typography variant="caption" sx={{ color: '#718096', fontWeight: 600, textTransform: 'uppercase' }}>
                Depósitos Pendentes de Conciliação
              </Typography>
              <Typography variant="h5" sx={{ fontWeight: 800, color: pendingDeposits.length > 0 ? '#d69e2e' : '#38a169', fontFamily: 'monospace', mt: 0.5 }}>
                {pendingDeposits.length} comprovante(s)
              </Typography>
              <Typography variant="caption" sx={{ color: '#a0aec0' }}>
                Aguardando conferência no extrato real pelo CFO
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ borderColor: '#e2e8f0', borderRadius: 2 }}>
            <CardContent sx={{ py: 2 }}>
              <Typography variant="caption" sx={{ color: '#718096', fontWeight: 600, textTransform: 'uppercase' }}>
                Transferências Pendentes de Aceite
              </Typography>
              <Typography variant="h5" sx={{ fontWeight: 800, color: pendingTransfers.length > 0 ? '#e53e3e' : '#38a169', fontFamily: 'monospace', mt: 0.5 }}>
                {pendingTransfers.length} pendente(s)
              </Typography>
              <Typography variant="caption" sx={{ color: '#a0aec0' }}>
                Passagens de gaveta aguardando contagem física
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Navegação de Abas */}
      <Paper variant="outlined" sx={{ borderRadius: 2, mb: 3 }}>
        <Tabs
          value={activeTab}
          onChange={(_, v) => setActiveTab(v)}
          indicatorColor="primary"
          textColor="primary"
          sx={{ borderBottom: 1, borderColor: '#edf2f7', px: 2 }}
        >
          <Tab icon={<WalletIcon />} iconPosition="start" label="Livro-Caixa por Colaborador" sx={{ textTransform: 'none', fontWeight: 600 }} />
          <Tab
            icon={<TransferIcon />}
            iconPosition="start"
            label={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                Passagem de Turno (Duplo Aceite)
                {pendingTransfers.length > 0 && <Chip label={pendingTransfers.length} size="small" color="error" />}
              </Box>
            }
            sx={{ textTransform: 'none', fontWeight: 600 }}
          />
          <Tab
            icon={<AuditIcon />}
            iconPosition="start"
            label={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                Conciliação de Depósitos (CFO)
                {pendingDeposits.length > 0 && <Chip label={pendingDeposits.length} size="small" color="warning" />}
              </Box>
            }
            sx={{ textTransform: 'none', fontWeight: 600 }}
          />
          <Tab icon={<ToolIcon />} iconPosition="start" label="Carga de Materiais por CPF" sx={{ textTransform: 'none', fontWeight: 600 }} />
        </Tabs>

        {/* ABA 0: Livro-Caixa por Colaborador */}
        {activeTab === 0 && (
          <Box sx={{ p: 2 }}>
            <TableContainer>
              <Table size="small">
                <TableHead sx={{ backgroundColor: '#f7fafc' }}>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 700, color: '#4a5568' }}>Colaborador</TableCell>
                    <TableCell sx={{ fontWeight: 700, color: '#4a5568' }}>CPF</TableCell>
                    <TableCell sx={{ fontWeight: 700, color: '#4a5568' }}>Papel / Função</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 700, color: '#4a5568' }}>Saldo em Mãos</TableCell>
                    <TableCell sx={{ fontWeight: 700, color: '#4a5568' }}>Última Movimentação</TableCell>
                    <TableCell align="center" sx={{ fontWeight: 700, color: '#4a5568' }}>Ações</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {custodies.map((c) => (
                    <TableRow key={c.id} hover>
                      <TableCell sx={{ fontWeight: 600, color: '#2d3748' }}>{c.userName}</TableCell>
                      <TableCell sx={{ fontFamily: 'monospace', color: '#4a5568' }}>{c.cpf || 'Não informado'}</TableCell>
                      <TableCell>
                        <Chip label={c.userRole} size="small" variant="outlined" sx={{ fontSize: '0.75rem' }} />
                      </TableCell>
                      <TableCell align="right" sx={{ fontFamily: 'monospace', fontWeight: 700, color: c.currentBalance > 0 ? '#e53e3e' : '#38a169', fontSize: '1rem' }}>
                        {formatBRL(c.currentBalance)}
                      </TableCell>
                      <TableCell sx={{ fontSize: '0.8rem', color: '#718096' }}>
                        {new Date(c.updatedAt).toLocaleString('pt-BR')}
                      </TableCell>
                      <TableCell align="center">
                        <Button
                          size="small"
                          variant="text"
                          startIcon={<TransferIcon />}
                          onClick={() => {
                            setTransferForm((prev) => ({ ...prev, receiverUserId: c.userId }));
                            setTransferModalOpen(true);
                          }}
                          sx={{ textTransform: 'none', fontSize: '0.8rem' }}
                        >
                          Passar Valor
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                  {custodies.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} align="center" sx={{ py: 3, color: '#a0aec0' }}>
                        Nenhum colaborador com saldo de custódia registrado.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {/* ABA 1: Passagem de Turno (Duplo Aceite) */}
        {activeTab === 1 && (
          <Box sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, color: '#2d3748', mb: 1.5 }}>
              Transferências Aguardando Sua Confirmação de Recebimento
            </Typography>
            <TableContainer>
              <Table size="small">
                <TableHead sx={{ backgroundColor: '#f7fafc' }}>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 700 }}>Remetente</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 700 }}>Valor em Notas</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Motivo</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Data/Hora</TableCell>
                    <TableCell align="center" sx={{ fontWeight: 700 }}>Conferência Física</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {pendingTransfers.map((t) => (
                    <TableRow key={t.id} hover>
                      <TableCell sx={{ fontWeight: 600 }}>{t.senderUserName}</TableCell>
                      <TableCell align="right" sx={{ fontFamily: 'monospace', fontWeight: 700, color: '#e53e3e', fontSize: '1rem' }}>
                        {formatBRL(t.amount)}
                      </TableCell>
                      <TableCell sx={{ color: '#4a5568' }}>{t.reason}</TableCell>
                      <TableCell sx={{ fontSize: '0.8rem', color: '#718096' }}>
                        {new Date(t.requestedAt).toLocaleString('pt-BR')}
                      </TableCell>
                      <TableCell align="center">
                        <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                          <Button
                            size="small"
                            variant="contained"
                            color="success"
                            startIcon={<CheckIcon />}
                            onClick={() => handleTransferResponse(t.id, true)}
                            sx={{ textTransform: 'none', fontWeight: 600 }}
                          >
                            Contei e Recebi
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            color="error"
                            startIcon={<RejectIcon />}
                            onClick={() => handleTransferResponse(t.id, false)}
                            sx={{ textTransform: 'none' }}
                          >
                            Recusar
                          </Button>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                  {pendingTransfers.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center" sx={{ py: 3, color: '#a0aec0' }}>
                        Nenhuma transferência de dinheiro pendente para você neste momento.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {/* ABA 2: Conciliação de Depósitos (CFO) */}
        {activeTab === 2 && (
          <Box sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, color: '#2d3748', mb: 1.5 }}>
              Depósitos Realizados por Colaboradores (Exige Conferência no Extrato Real)
            </Typography>
            <TableContainer>
              <Table size="small">
                <TableHead sx={{ backgroundColor: '#f7fafc' }}>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 700 }}>Depositante</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Banco Destino</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 700 }}>Valor Depositado</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Comprovante</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Data do Depósito</TableCell>
                    <TableCell align="center" sx={{ fontWeight: 700 }}>Auditoria CFO</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {pendingDeposits.map((d) => (
                    <TableRow key={d.id} hover>
                      <TableCell sx={{ fontWeight: 600 }}>{d.depositorUserName}</TableCell>
                      <TableCell sx={{ color: '#4a5568' }}>{d.bankName} (Ag: {d.bankAgency} / CC: {d.bankAccount})</TableCell>
                      <TableCell align="right" sx={{ fontFamily: 'monospace', fontWeight: 700, color: '#2b6cb0', fontSize: '1rem' }}>
                        {formatBRL(d.amount)}
                      </TableCell>
                      <TableCell>
                        <Button
                          size="small"
                          startIcon={<ExternalLinkIcon />}
                          href={d.receiptFileUrl}
                          target="_blank"
                          sx={{ textTransform: 'none', fontSize: '0.8rem' }}
                        >
                          Ver Comprovante
                        </Button>
                      </TableCell>
                      <TableCell sx={{ fontSize: '0.8rem', color: '#718096' }}>
                        {new Date(d.depositDate).toLocaleString('pt-BR')}
                      </TableCell>
                      <TableCell align="center">
                        <Button
                          size="small"
                          variant="contained"
                          color="primary"
                          startIcon={<AuditIcon />}
                          onClick={() => {
                            setSelectedDepositForAudit(d);
                            setAuditModalOpen(true);
                          }}
                          sx={{ textTransform: 'none', fontWeight: 600 }}
                        >
                          Conciliar Extrato
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                  {pendingDeposits.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} align="center" sx={{ py: 3, color: '#a0aec0' }}>
                        Nenhum comprovante de depósito aguardando conciliação.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {/* ABA 3: Carga de Materiais por CPF */}
        {activeTab === 3 && (
          <Box sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', mb: 2 }}>
              <Typography variant="body2" sx={{ fontWeight: 600, color: '#4a5568' }}>
                Selecionar Técnico:
              </Typography>
              <TextField
                select
                size="small"
                value={selectedUserIdForMaterials}
                onChange={(e) => handleUserMaterialsChange(e.target.value)}
                sx={{ minWidth: 260 }}
              >
                {custodies.map((c) => (
                  <MenuItem key={c.userId} value={c.userId}>
                    {c.userName} ({c.userRole})
                  </MenuItem>
                ))}
              </TextField>
            </Box>

            <TableContainer>
              <Table size="small">
                <TableHead sx={{ backgroundColor: '#f7fafc' }}>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 700 }}>Item / Equipamento</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Tipo</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Número de Série / MAC</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 700 }}>Quantidade</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Data da Carga</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {selectedUserMaterials.map((m) => (
                    <TableRow key={m.id} hover>
                      <TableCell sx={{ fontWeight: 600 }}>{m.itemName}</TableCell>
                      <TableCell>
                        <Chip label={m.itemType} size="small" variant="outlined" />
                      </TableCell>
                      <TableCell sx={{ fontFamily: 'monospace', color: '#4a5568' }}>
                        {m.serialNumber || m.macAddress || 'N/A'}
                      </TableCell>
                      <TableCell align="right" sx={{ fontFamily: 'monospace', fontWeight: 700 }}>
                        {m.quantity} {m.unit}
                      </TableCell>
                      <TableCell sx={{ fontSize: '0.8rem', color: '#718096' }}>
                        {new Date(m.allocatedAt).toLocaleString('pt-BR')}
                      </TableCell>
                    </TableRow>
                  ))}
                  {selectedUserMaterials.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center" sx={{ py: 3, color: '#a0aec0' }}>
                        Nenhum equipamento ou insumo sob a carga deste colaborador.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}
      </Paper>

      {/* Modal: Transferência de Custódia (Passagem de Gaveta) */}
      <Dialog open={transferModalOpen} onClose={() => setTransferModalOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>Passagem de Gaveta / Transferência de Custódia</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" sx={{ color: '#718096', mb: 2 }}>
            O dinheiro permanece sob sua responsabilidade até que o colaborador de destino conte as cédulas e confirme o aceite.
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                size="small"
                label="Colaborador Recebedor"
                value={transferForm.receiverUserId}
                onChange={(e) => setTransferForm({ ...transferForm, receiverUserId: e.target.value })}
              >
                {custodies
                  .filter((c) => c.userId !== currentUserId)
                  .map((c) => (
                    <MenuItem key={c.userId} value={c.userId}>
                      {c.userName} ({c.userRole})
                    </MenuItem>
                  ))}
              </TextField>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                size="small"
                label="Valor a Transferir"
                type="number"
                InputProps={{
                  startAdornment: <InputAdornment position="start">R$</InputAdornment>
                }}
                value={transferForm.amount}
                onChange={(e) => setTransferForm({ ...transferForm, amount: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                size="small"
                label="Motivo da Transferência"
                value={transferForm.reason}
                onChange={(e) => setTransferForm({ ...transferForm, reason: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTransferModalOpen(false)} color="inherit" sx={{ textTransform: 'none' }}>
            Cancelar
          </Button>
          <Button onClick={handleTransferSubmit} variant="contained" color="primary" sx={{ textTransform: 'none', fontWeight: 600 }}>
            Enviar para Aceite
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal: Prestação de Contas (Depósito Bancário) */}
      <Dialog open={depositModalOpen} onClose={() => setDepositModalOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>Prestação de Contas (Comprovante de Depósito)</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" sx={{ color: '#718096', mb: 2 }}>
            Anexe o comprovante de depósito bancário em favor da empresa. O saldo só será baixado após conferência do extrato pelo CFO.
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                size="small"
                label="Valor Depositado"
                type="number"
                InputProps={{
                  startAdornment: <InputAdornment position="start">R$</InputAdornment>
                }}
                value={depositForm.amount}
                onChange={(e) => setDepositForm({ ...depositForm, amount: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                size="small"
                label="Banco Destino"
                value={depositForm.bankName}
                onChange={(e) => setDepositForm({ ...depositForm, bankName: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                size="small"
                label="Link do Comprovante (URL ou S3)"
                placeholder="https://storage.isperp.dev/receipts/comprovante.jpg"
                value={depositForm.receiptFileUrl}
                onChange={(e) => setDepositForm({ ...depositForm, receiptFileUrl: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                size="small"
                label="Observações / Agência e Conta"
                value={depositForm.notes}
                onChange={(e) => setDepositForm({ ...depositForm, notes: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDepositModalOpen(false)} color="inherit" sx={{ textTransform: 'none' }}>
            Cancelar
          </Button>
          <Button onClick={handleDepositSubmit} variant="contained" color="success" sx={{ textTransform: 'none', fontWeight: 600 }}>
            Enviar para Auditoria
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal: Conciliação Bancária CFO */}
      <Dialog open={auditModalOpen} onClose={() => setAuditModalOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 700 }}>Conciliação de Depósito Bancário (CFO)</DialogTitle>
        <DialogContent dividers>
          {selectedDepositForAudit && (
            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" sx={{ fontWeight: 600, color: '#2d3748' }}>
                Depositante: {selectedDepositForAudit.depositorUserName} (CPF: {selectedDepositForAudit.depositorCpf})
              </Typography>
              <Typography variant="h6" sx={{ color: '#2b6cb0', fontFamily: 'monospace', my: 1 }}>
                Valor: {formatBRL(selectedDepositForAudit.amount)}
              </Typography>
              <Typography variant="body2" sx={{ color: '#4a5568', mb: 1 }}>
                Banco: {selectedDepositForAudit.bankName}
              </Typography>
              <Button
                variant="outlined"
                size="small"
                startIcon={<ExternalLinkIcon />}
                href={selectedDepositForAudit.receiptFileUrl}
                target="_blank"
                sx={{ textTransform: 'none', mb: 2 }}
              >
                Abrir Comprovante em Nova Aba
              </Button>
              <TextField
                fullWidth
                size="small"
                label="Notas de Auditoria (Opcional)"
                value={auditForm.notes}
                onChange={(e) => setAuditForm({ ...auditForm, notes: e.target.value })}
                sx={{ mb: 1.5 }}
              />
              <TextField
                fullWidth
                size="small"
                label="Motivo de Rejeição (caso recuse)"
                value={auditForm.rejectionReason}
                onChange={(e) => setAuditForm({ ...auditForm, rejectionReason: e.target.value })}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => handleAuditDeposit(false)} variant="outlined" color="error" sx={{ textTransform: 'none' }}>
            Rejeitar Depósito
          </Button>
          <Button onClick={() => handleAuditDeposit(true)} variant="contained" color="primary" sx={{ textTransform: 'none', fontWeight: 600 }}>
            Conferido no Extrato & Aprovar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CashCustodyManager;
