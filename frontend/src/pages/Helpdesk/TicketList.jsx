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
  CardContent
} from '@mui/material';
import {
  Search as SearchIcon,
  Add as AddIcon,
  HeadsetMic as HelpdeskIcon,
  AccessTime as TimeIcon,
  CheckCircle as ResolvedIcon,
  Warning as WarningIcon
} from '@mui/icons-material';
import { helpdeskService } from '../../services/helpdeskService';
import { customerService } from '../../services/customerService';
import TicketDetailModal from './TicketDetailModal';
import { toast } from 'react-toastify';

const categories = [
  { value: 'CONNECTION_OUTAGE', label: 'Sem Conexão / LOS (24h)' },
  { value: 'SLOW_SPEED', label: 'Lentidão na Conexão (48h)' },
  { value: 'FINANCIAL', label: 'Financeiro / Pagamentos (24h)' },
  { value: 'ROUTER_CONFIG', label: 'Configuração Roteador/Wi-Fi (48h)' },
  { value: 'ADDRESS_CHANGE', label: 'Mudança de Endereço (72h)' },
  { value: 'ROOM_TRANSFER', label: 'Troca de Cômodo (72h)' },
  { value: 'CANCELLATION_REQUEST', label: 'Solicitação Cancelamento (24h)' },
  { value: 'OTHER', label: 'Outros Assuntos (48h)' },
];

const channels = [
  { value: 'PHONE', label: 'Telefone (Central)' },
  { value: 'WHATSAPP_BOT', label: 'WhatsApp / Bot' },
  { value: 'PORTAL', label: 'Central do Assinante' },
  { value: 'IN_PERSON', label: 'Presencial / Loja' },
  { value: 'EMAIL', label: 'E-mail' },
];

const TicketList = () => {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);

  // Modal Novo Chamado
  const [newModalOpen, setNewModalOpen] = useState(false);
  const [customers, setCustomers] = useState([]);
  const [newForm, setNewForm] = useState({
    customerId: '',
    category: 'CONNECTION_OUTAGE',
    channel: 'PHONE',
    subject: '',
    description: '',
  });
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const [ticketsRes, customersRes] = await Promise.all([
        helpdeskService.getAllTickets(),
        customerService.getAllCustomers(),
      ]);
      setTickets(ticketsRes.data || []);
      setCustomers(customersRes.data || []);
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

  const handleCreateTicket = async () => {
    if (!newForm.customerId || !newForm.subject.trim() || !newForm.description.trim()) {
      toast.warning('Preencha cliente, assunto e descrição.');
      return;
    }

    try {
      setSubmitting(true);
      const res = await helpdeskService.createTicket({
        ...newForm,
        attendantName: 'Atendente N1',
      });
      toast.success(`Chamado aberto com Protocolo ANATEL: ${res.data.protocol}`);
      setNewModalOpen(false);
      setNewForm({
        customerId: '',
        category: 'CONNECTION_OUTAGE',
        channel: 'PHONE',
        subject: '',
        description: '',
      });
      loadData();
    } catch (err) {
      toast.error('Erro ao criar chamado: ' + (err.response?.data?.message || err.message));
    } finally {
      setSubmitting(false);
    }
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
    return matchSearch && matchStatus;
  });

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Helpdesk & Atendimento (Protocolo ANATEL)
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Gestão regulatória de chamados N1 / N2 com controle de SLA e tramitação de O.S.
          </Typography>
        </Box>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => setNewModalOpen(true)}
          sx={{ borderRadius: 2, fontWeight: 'bold' }}
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
          sx={{ minWidth: 300, flexGrow: 1 }}
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
                <TableCell><strong>Canal</strong></TableCell>
                <TableCell><strong>Status</strong></TableCell>
                <TableCell><strong>Limite SLA</strong></TableCell>
                <TableCell align="right"><strong>Ações</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredTickets.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                    Nenhum chamado encontrado.
                  </TableCell>
                </TableRow>
              ) : (
                filteredTickets.map((t) => {
                  const isOverdue = new Date(t.slaDeadline) < new Date() && t.status !== 'CLOSED' && t.status !== 'RESOLVED';
                  return (
                    <TableRow key={t.id} hover>
                      <TableCell>
                        <Typography variant="body2" fontWeight="bold" color="primary.main">
                          {t.protocol}
                        </Typography>
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
                onChange={(e) => setNewForm({ ...newForm, customerId: e.target.value })}
              >
                {customers.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.name} - CPF/CNPJ: {c.cpf}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
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
