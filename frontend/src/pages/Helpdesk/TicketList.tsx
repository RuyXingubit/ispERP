import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  TextField,
  InputAdornment,
  MenuItem,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
  Card,
  CardContent,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  Search as SearchIcon,
  Add as AddIcon,
  HeadsetMic as HelpdeskIcon,
  AccessTime as TimeIcon,
  CheckCircle as ResolvedIcon,
  Warning as WarningIcon,
  ContentCopy as CopyIcon
} from '@mui/icons-material';
import { helpdeskService } from '../../services/helpdeskService';
import { customerService } from '../../services/customerService';
import { contractService } from '../../services/contractService';
import { useAuth } from '../../contexts/AuthContext';
import TicketDetailModal from './TicketDetailModal';
import { toast } from 'react-toastify';

const categories = [
  { value: 'CONNECTION_OUTAGE', label: 'Sem Conexão / LOS (24h SLA)' },
  { value: 'SLOW_SPEED', label: 'Lentidão na Conexão (48h SLA)' },
  { value: 'FINANCIAL', label: 'Financeiro / Pagamentos (24h SLA)' },
  { value: 'ROUTER_CONFIG', label: 'Configuração Roteador/Wi-Fi (48h SLA)' },
  { value: 'ADDRESS_CHANGE', label: 'Mudança de Endereço (72h SLA)' },
  { value: 'ROOM_TRANSFER', label: 'Troca de Cômodo (72h SLA)' },
  { value: 'CANCELLATION_REQUEST', label: 'Solicitação de Cancelamento (24h SLA)' },
  { value: 'OTHER', label: 'Outros Assuntos (48h SLA)' },
];

const channels = [
  { value: 'PHONE', label: 'Telefone (Central Telefônica)' },
  { value: 'WHATSAPP_BOT', label: 'WhatsApp / Bot' },
  { value: 'PORTAL', label: 'Central do Assinante' },
  { value: 'IN_PERSON', label: 'Presencial / Loja' },
  { value: 'EMAIL', label: 'E-mail' },
];

const priorities = [
  { value: 'LOW', label: 'Baixa', color: 'default' },
  { value: 'NORMAL', label: 'Normal', color: 'info' },
  { value: 'HIGH', label: 'Alta', color: 'warning' },
  { value: 'URGENT', label: 'Urgente (Bloqueio Total)', color: 'error' },
];

const TicketList = () => {
  const { user } = useAuth();
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);

  // Modal Novo Chamado
  const [newModalOpen, setNewModalOpen] = useState(false);
  const [customers, setCustomers] = useState([]);
  const [customerContracts, setCustomerContracts] = useState([]);
  const [loadingContracts, setLoadingContracts] = useState(false);
  const [newForm, setNewForm] = useState({
    customerId: '',
    contractId: '',
    category: 'CONNECTION_OUTAGE',
    priority: 'NORMAL',
    channel: 'PHONE',
    subject: '',
    description: '',
  });
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const [ticketsRes, customersRes]: any[] = await Promise.all([
        helpdeskService.getAllTickets(),
        customerService.getAllCustomers(),
      ]);
      setTickets(Array.isArray(ticketsRes) ? ticketsRes : (ticketsRes?.data || []));
      setCustomers(Array.isArray(customersRes) ? customersRes : (customersRes?.data || []));
    } catch (err) {
      console.error('Erro ao carregar chamados:', err);
      toast.error('Erro ao carregar lista de chamados.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCustomerChange = async (customerId: string) => {
    setNewForm((prev) => ({ ...prev, customerId, contractId: '' }));
    if (!customerId) {
      setCustomerContracts([]);
      return;
    }
    try {
      setLoadingContracts(true);
      const res: any = await contractService.getContractsByCustomerId(customerId);
      const contracts = Array.isArray(res) ? res : (res?.data || []);
      setCustomerContracts(contracts);
      if (contracts.length === 1) {
        setNewForm((prev) => ({ ...prev, customerId, contractId: contracts[0].id }));
      }
    } catch (err) {
      console.error('Erro ao carregar contratos do cliente:', err);
      setCustomerContracts([]);
    } finally {
      setLoadingContracts(false);
    }
  };

  const handleCreateTicket = async () => {
    if (!newForm.customerId || !newForm.subject.trim() || !newForm.description.trim()) {
      toast.warning('Preencha cliente, assunto e descrição.');
      return;
    }

    try {
      setSubmitting(true);
      const payload: any = {
        customerId: newForm.customerId,
        contractId: newForm.contractId || null,
        category: newForm.category,
        priority: newForm.priority,
        channel: newForm.channel,
        subject: newForm.subject.trim(),
        description: newForm.description.trim(),
        attendantUserId: user?.id || null,
        attendantName: user?.name || user?.username || 'Atendente N1',
      };

      const res: any = await helpdeskService.createTicket(payload);
      const ticketResult = res?.data || res;
      toast.success(`Chamado aberto com Protocolo ANATEL: ${ticketResult?.protocol || 'OK'}`);
      setNewModalOpen(false);
      setNewForm({
        customerId: '',
        contractId: '',
        category: 'CONNECTION_OUTAGE',
        priority: 'NORMAL',
        channel: 'PHONE',
        subject: '',
        description: '',
      });
      setCustomerContracts([]);
      loadData();
    } catch (err) {
      toast.error('Erro ao criar chamado: ' + (err.response?.data?.message || err.message));
    } finally {
      setSubmitting(false);
    }
  };

  const copyProtocol = (protocol) => {
    navigator.clipboard.writeText(protocol);
    toast.info(`Protocolo ${protocol} copiado!`);
  };

  const getCustomerName = (id) => {
    const c = customers.find((item) => item.id === id);
    return c ? c.name : 'Cliente ' + id?.substring(0, 8);
  };

  const filteredTickets = tickets.filter((t) => {
    const matchSearch =
      (t.protocol && t.protocol.toLowerCase().includes(search.toLowerCase())) ||
      (t.subject && t.subject.toLowerCase().includes(search.toLowerCase())) ||
      getCustomerName(t.customerId).toLowerCase().includes(search.toLowerCase());
    const matchStatus = statusFilter === 'ALL' || t.status === statusFilter;
    const matchPriority = priorityFilter === 'ALL' || t.priority === priorityFilter;
    return matchSearch && matchStatus && matchPriority;
  });

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Central de Atendimento & Helpdesk
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Gestão regulatória de chamados N1 / N2 com protocolos ANATEL e controle de SLA em tempo real.
          </Typography>
        </Box>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => setNewModalOpen(true)}
          sx={{ borderRadius: 2, fontWeight: 'bold', px: 3, py: 1 }}
        >
          Novo Chamado ANATEL
        </Button>
      </Box>

      {/* Cards de Métricas */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card sx={{ borderRadius: 3, borderLeft: '6px solid #1976d2' }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                CHAMADOS EM ABERTO / N1
              </Typography>
              <Typography variant="h4" fontWeight="bold" color="primary.main">
                {tickets.filter((t) => t.status === 'OPEN').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card sx={{ borderRadius: 3, borderLeft: '6px solid #ed6c02' }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                EM ANÁLISE / SUPORTE N2
              </Typography>
              <Typography variant="h4" fontWeight="bold" sx={{ color: '#ed6c02' }}>
                {tickets.filter((t) => t.status === 'IN_PROGRESS' || t.status === 'WAITING_FIELD_VISIT').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card sx={{ borderRadius: 3, borderLeft: '6px solid #2e7d32' }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                RESOLVIDOS & ENCERRADOS
              </Typography>
              <Typography variant="h4" fontWeight="bold" color="success.main">
                {tickets.filter((t) => t.status === 'RESOLVED' || t.status === 'CLOSED').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Filtros */}
      <Paper elevation={1} sx={{ p: 2, mb: 3, borderRadius: 2, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
        <TextField
          placeholder="Buscar por protocolo ANATEL, cliente ou assunto..."
          size="small"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          sx={{ minWidth: 280, flexGrow: 1 }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
        />
        <TextField
          select
          size="small"
          label="Status"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          sx={{ minWidth: 160 }}
        >
          <MenuItem value="ALL">Todos os Status</MenuItem>
          <MenuItem value="OPEN">Abertos</MenuItem>
          <MenuItem value="IN_PROGRESS">Em Andamento (N2)</MenuItem>
          <MenuItem value="WAITING_FIELD_VISIT">Aguardando O.S. Campo</MenuItem>
          <MenuItem value="RESOLVED">Resolvidos</MenuItem>
          <MenuItem value="CLOSED">Encerrados</MenuItem>
        </TextField>
        <TextField
          select
          size="small"
          label="Prioridade"
          value={priorityFilter}
          onChange={(e) => setPriorityFilter(e.target.value)}
          sx={{ minWidth: 140 }}
        >
          <MenuItem value="ALL">Todas Prioridades</MenuItem>
          <MenuItem value="LOW">Baixa</MenuItem>
          <MenuItem value="NORMAL">Normal</MenuItem>
          <MenuItem value="HIGH">Alta</MenuItem>
          <MenuItem value="URGENT">Urgente</MenuItem>
        </TextField>
      </Paper>

      {/* Tabela de Chamados */}
      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 2 }}>
          <Table>
            <TableHead sx={{ bgcolor: '#f8f9fa' }}>
              <TableRow>
                <TableCell><strong>Protocolo ANATEL</strong></TableCell>
                <TableCell><strong>Cliente</strong></TableCell>
                <TableCell><strong>Categoria / Assunto</strong></TableCell>
                <TableCell><strong>Prioridade</strong></TableCell>
                <TableCell><strong>Canal</strong></TableCell>
                <TableCell><strong>Status</strong></TableCell>
                <TableCell><strong>Prazo SLA</strong></TableCell>
                <TableCell align="right"><strong>Ações</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredTickets.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                    Nenhum chamado encontrado.
                  </TableCell>
                </TableRow>
              ) : (
                filteredTickets.map((t) => {
                  const isOverdue = new Date(t.slaDeadline) < new Date() && t.status !== 'CLOSED' && t.status !== 'RESOLVED';
                  return (
                    <TableRow key={t.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          <Typography variant="body2" fontWeight="bold" color="primary.main" sx={{ fontFamily: 'monospace' }}>
                            {t.protocol}
                          </Typography>
                          <Tooltip title="Copiar protocolo">
                            <IconButton size="small" onClick={() => copyProtocol(t.protocol)}>
                              <CopyIcon fontSize="inherit" />
                            </IconButton>
                          </Tooltip>
                        </Box>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(t.createdAt).toLocaleDateString('pt-BR')}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight="bold">
                          {getCustomerName(t.customerId)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{t.subject}</Typography>
                        <Chip label={t.category} size="small" variant="outlined" sx={{ mt: 0.5 }} />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={t.priority || 'NORMAL'}
                          size="small"
                          color={
                            t.priority === 'URGENT'
                              ? 'error'
                              : t.priority === 'HIGH'
                              ? 'warning'
                              : t.priority === 'LOW'
                              ? 'default'
                              : 'info'
                          }
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>
                        <Chip label={t.channel} size="small" />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={t.status}
                          color={
                            t.status === 'RESOLVED' || t.status === 'CLOSED'
                              ? 'success'
                              : t.status === 'IN_PROGRESS' || t.status === 'WAITING_FIELD_VISIT'
                              ? 'warning'
                              : 'primary'
                          }
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          {isOverdue ? <WarningIcon color="error" fontSize="small" /> : <TimeIcon fontSize="small" color="action" />}
                          <Typography variant="caption" color={isOverdue ? 'error.main' : 'text.secondary'} fontWeight={isOverdue ? 'bold' : 'normal'}>
                            {new Date(t.slaDeadline).toLocaleString('pt-BR')}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell align="right">
                        <Button
                          variant="outlined"
                          size="small"
                          onClick={() => {
                            setSelectedTicket(t);
                            setModalOpen(true);
                          }}
                        >
                          Atender
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Modal Detalhes do Chamado */}
      <TicketDetailModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        ticket={selectedTicket}
        onUpdated={loadData}
        currentUserRole={user?.role}
        currentUserId={user?.id}
        currentUserName={user?.name || user?.username || 'Atendente'}
      />

      {/* Modal Abertura de Novo Chamado ANATEL */}
      <Dialog open={newModalOpen} onClose={() => setNewModalOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ bgcolor: '#1976d2', color: '#fff', fontWeight: 'bold' }}>
          Abertura de Chamado (Protocolo ANATEL)
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                label="Cliente Solicitante *"
                value={newForm.customerId}
                onChange={(e) => handleCustomerChange(e.target.value)}
              >
                {customers.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.name} - CPF/CNPJ: {c.cpf}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            {customerContracts.length > 0 && (
              <Grid item xs={12}>
                <TextField
                  select
                  fullWidth
                  label="Contrato / Ponto de Acesso Vinculado"
                  value={newForm.contractId}
                  onChange={(e) => setNewForm({ ...newForm, contractId: e.target.value })}
                  helperText="Selecione caso o cliente possua múltiplos contratos"
                >
                  <MenuItem value="">Nenhum contrato específico</MenuItem>
                  {customerContracts.map((cnt) => (
                    <MenuItem key={cnt.id} value={cnt.id}>
                      Contrato #{cnt.id.substring(0, 8)} - Status: {cnt.status}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
            )}

            <Grid item xs={12} sm={6}>
              <TextField
                select
                fullWidth
                label="Categoria do Chamado *"
                value={newForm.category}
                onChange={(e) => setNewForm({ ...newForm, category: e.target.value })}
              >
                {categories.map((cat) => (
                  <MenuItem key={cat.value} value={cat.value}>
                    {cat.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                select
                fullWidth
                label="Prioridade *"
                value={newForm.priority}
                onChange={(e) => setNewForm({ ...newForm, priority: e.target.value })}
              >
                {priorities.map((p) => (
                  <MenuItem key={p.value} value={p.value}>
                    {p.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                label="Canal de Entrada *"
                value={newForm.channel}
                onChange={(e) => setNewForm({ ...newForm, channel: e.target.value })}
              >
                {channels.map((ch) => (
                  <MenuItem key={ch.value} value={ch.value}>
                    {ch.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Assunto / Resumo do Problema *"
                placeholder="Ex: ONU sem sinal / LOS vermelho"
                value={newForm.subject}
                onChange={(e) => setNewForm({ ...newForm, subject: e.target.value })}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Descrição Detalhada do Atendimento *"
                placeholder="Relato do cliente, testes iniciais de N1 executados..."
                value={newForm.description}
                onChange={(e) => setNewForm({ ...newForm, description: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setNewModalOpen(false)}>Cancelar</Button>
          <Button
            variant="contained"
            color="primary"
            onClick={handleCreateTicket}
            disabled={submitting}
          >
            {submitting ? 'Gerando Protocolo...' : 'Gerar Protocolo ANATEL & Abrir'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TicketList;
