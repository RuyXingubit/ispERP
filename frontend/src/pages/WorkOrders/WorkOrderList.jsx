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
  MenuItem,
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
  Typography,
  Alert,
} from '@mui/material';
import {
  Build as ToolIcon,
  CalendarMonth as CalendarIcon,
  CheckCircle as DoneIcon,
  Warning as WarningIcon,
  Router as RouterIcon,
  SignalCellularAlt as SignalIcon,
  Engineering as TechIcon,
} from '@mui/icons-material';
import workOrderService from '../../services/workOrderService';

const statusConfig = {
  PENDING_SCHEDULE: { label: 'Aguardando Agendamento', color: 'error' },
  SCHEDULED: { label: 'Agendada / Em Campo', color: 'warning' },
  IN_PROGRESS: { label: 'Em Execução', color: 'info' },
  COMPLETED: { label: 'Concluída & Ativada', color: 'success' },
  CANCELED: { label: 'Cancelada', color: 'default' },
};

const WorkOrderList = () => {
  const [workOrders, setWorkOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentTab, setCurrentTab] = useState(0);

  // Modais
  const [scheduleModalOpen, setScheduleModalOpen] = useState(false);
  const [completeModalOpen, setCompleteModalOpen] = useState(false);
  const [selectedWO, setSelectedWO] = useState(null);

  // Form de Agendamento
  const [scheduleForm, setScheduleForm] = useState({
    scheduledDate: new Date().toISOString().split('T')[0],
    scheduledPeriod: 'MANHA',
    technicianName: 'Carlos Silva (Equipe 01)',
    notes: '',
  });

  // Form de Baixa Técnica
  const [completeForm, setCompleteForm] = useState({
    onuMac: '',
    onuSerial: '',
    fiberSignalDbm: '-19.50',
    technicianLatitude: null,
    technicianLongitude: null,
    notes: '',
  });
  const [gpsLoading, setGpsLoading] = useState(false);
  const [gpsSuccess, setGpsSuccess] = useState(false);

  const handleCaptureGps = () => {
    if (!navigator.geolocation) {
      alert('Geolocalização não é suportada pelo seu navegador/dispositivo.');
      return;
    }
    setGpsLoading(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setCompleteForm((prev) => ({
          ...prev,
          technicianLatitude: position.coords.latitude,
          technicianLongitude: position.coords.longitude,
        }));
        setGpsSuccess(true);
        setGpsLoading(false);
      },
      (err) => {
        console.error('Erro ao obter GPS:', err);
        alert('Não foi possível obter a localização GPS. Verifique a permissão do navegador.');
        setGpsLoading(false);
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  const loadWorkOrders = async () => {
    try {
      setLoading(true);
      const res = await workOrderService.getAllWorkOrders();
      setWorkOrders(res.data || []);
      setError(null);
    } catch (err) {
      setError('Erro ao carregar ordens de serviço.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWorkOrders();
  }, []);

  const handleOpenSchedule = (wo) => {
    setSelectedWO(wo);
    setScheduleForm({
      scheduledDate: new Date().toISOString().split('T')[0],
      scheduledPeriod: 'MANHA',
      technicianName: 'Carlos Silva (Equipe 01)',
      notes: '',
    });
    setScheduleModalOpen(true);
  };

  const handleOpenComplete = (wo) => {
    setSelectedWO(wo);
    setCompleteForm({
      onuMac: '',
      onuSerial: '',
      fiberSignalDbm: '-19.50',
      notes: '',
    });
    setCompleteModalOpen(true);
  };

  const handleScheduleSubmit = async (e) => {
    e.preventDefault();
    try {
      await workOrderService.scheduleWorkOrder(selectedWO.id, scheduleForm);
      setScheduleModalOpen(false);
      loadWorkOrders();
    } catch (err) {
      alert('Erro ao agendar O.S.: ' + (err.response?.data || err.message));
    }
  };

  const handleCompleteSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...completeForm,
        fiberSignalDbm: Number(completeForm.fiberSignalDbm),
      };
      await workOrderService.completeWorkOrder(selectedWO.id, payload);
      setCompleteModalOpen(false);
      loadWorkOrders();
    } catch (err) {
      alert('Erro ao concluir O.S.: ' + (err.response?.data || err.message));
    }
  };

  const filterByTab = () => {
    switch (currentTab) {
      case 0:
        return workOrders.filter((wo) => wo.status === 'PENDING_SCHEDULE');
      case 1:
        return workOrders.filter((wo) => wo.status === 'SCHEDULED' || wo.status === 'IN_PROGRESS');
      case 2:
        return workOrders.filter((wo) => wo.status === 'COMPLETED');
      default:
        return workOrders;
    }
  };

  const filteredList = filterByTab();

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight="bold" gutterBottom>
          Central de Ordens de Serviço & Campo
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Agendamento técnico, despacho de equipes e ativação de assinantes pós-visita.
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Paper elevation={1} sx={{ mb: 3, borderRadius: 2 }}>
        <Tabs
          value={currentTab}
          onChange={(e, val) => setCurrentTab(val)}
          indicatorColor="primary"
          textColor="primary"
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab
            label={`Aguardando Agendamento (${workOrders.filter((w) => w.status === 'PENDING_SCHEDULE').length})`}
            icon={<WarningIcon fontSize="small" color="error" />}
            iconPosition="start"
          />
          <Tab
            label={`Agendadas / Em Campo (${workOrders.filter((w) => w.status === 'SCHEDULED' || w.status === 'IN_PROGRESS').length})`}
            icon={<CalendarIcon fontSize="small" color="warning" />}
            iconPosition="start"
          />
          <Tab
            label={`Concluídas & Ativadas (${workOrders.filter((w) => w.status === 'COMPLETED').length})`}
            icon={<DoneIcon fontSize="small" color="success" />}
            iconPosition="start"
          />
          <Tab label={`Todas as O.S. (${workOrders.length})`} />
        </Tabs>
      </Paper>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 5 }}>
          <CircularProgress />
        </Box>
      ) : (
        <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 2 }}>
          <Table>
            <TableHead sx={{ backgroundColor: '#f8f9fa' }}>
              <TableRow>
                <TableCell><strong>Tipo / O.S.</strong></TableCell>
                <TableCell><strong>Status</strong></TableCell>
                <TableCell><strong>Agendamento / Técnico</strong></TableCell>
                <TableCell><strong>Equipamentos / Sinal</strong></TableCell>
                <TableCell><strong>Observações</strong></TableCell>
                <TableCell align="right"><strong>Ações</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredList.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                    Nenhuma ordem de serviço encontrada nesta aba.
                  </TableCell>
                </TableRow>
              ) : (
                filteredList.map((wo) => {
                  const conf = statusConfig[wo.status] || { label: wo.status, color: 'default' };
                  return (
                    <TableRow key={wo.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <ToolIcon color="primary" />
                          <div>
                            <Typography variant="body2" fontWeight="bold">
                              {wo.type}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              Contrato: {wo.contractId?.substring(0, 8)}...
                            </Typography>
                          </div>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip label={conf.label} color={conf.color} size="small" />
                      </TableCell>
                      <TableCell>
                        {wo.scheduledDate ? (
                          <>
                            <Typography variant="body2" fontWeight="500">
                              {wo.scheduledDate} ({wo.scheduledPeriod})
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              <TechIcon fontSize="inherit" sx={{ verticalAlign: 'middle', mr: 0.5 }} />
                              {wo.technicianName}
                            </Typography>
                          </>
                        ) : (
                          <Typography variant="caption" color="error">
                            Pendente de Agendamento
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        {wo.status === 'COMPLETED' ? (
                          <>
                            <Typography variant="body2">
                              <RouterIcon fontSize="inherit" sx={{ verticalAlign: 'middle', mr: 0.5 }} />
                              MAC: {wo.onuMac}
                            </Typography>
                            <Typography variant="caption" color="success.main" fontWeight="bold">
                              <SignalIcon fontSize="inherit" sx={{ verticalAlign: 'middle', mr: 0.5 }} />
                              Sinal: {wo.fiberSignalDbm} dBm
                            </Typography>
                          </>
                        ) : (
                          <Typography variant="caption" color="text.secondary">
                            Aguardando campo
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" color="text.secondary" sx={{ maxWidth: 200, display: 'block' }}>
                          {wo.notes || '-'}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        {wo.status === 'PENDING_SCHEDULE' && (
                          <Button
                            variant="contained"
                            color="primary"
                            size="small"
                            onClick={() => handleOpenSchedule(wo)}
                          >
                            Agendar Visita
                          </Button>
                        )}
                        {(wo.status === 'SCHEDULED' || wo.status === 'IN_PROGRESS') && (
                          <Button
                            variant="contained"
                            color="success"
                            size="small"
                            onClick={() => handleOpenComplete(wo)}
                          >
                            Baixa Técnica
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Modal de Agendamento */}
      <Dialog open={scheduleModalOpen} onClose={() => setScheduleModalOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleScheduleSubmit}>
          <DialogTitle>Agendamento de Instalação Técnica</DialogTitle>
          <DialogContent dividers>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="date"
                  label="Data da Visita"
                  required
                  InputLabelProps={{ shrink: true }}
                  value={scheduleForm.scheduledDate}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, scheduledDate: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  select
                  label="Período"
                  required
                  value={scheduleForm.scheduledPeriod}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, scheduledPeriod: e.target.value })}
                >
                  <MenuItem value="MANHA">Manhã (08h às 12h)</MenuItem>
                  <MenuItem value="TARDE">Tarde (13h às 18h)</MenuItem>
                  <MenuItem value="SABADO_MANHA">Sábado Manhã (08h às 12h)</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Técnico / Equipe de Instalação"
                  required
                  value={scheduleForm.technicianName}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, technicianName: e.target.value })}
                  placeholder="Ex: Carlos Silva (Equipe Fibra 01)"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  label="Instruções / Observações para o Técnico"
                  value={scheduleForm.notes}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, notes: e.target.value })}
                  placeholder="Ex: Interfone 42, cliente pediu para ligar 15min antes."
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setScheduleModalOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Confirmar Agendamento
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Modal de Baixa Técnica / Ativação */}
      <Dialog open={completeModalOpen} onClose={() => setCompleteModalOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleCompleteSubmit}>
          <DialogTitle>Baixa Técnica de Campo & Ativação</DialogTitle>
          <DialogContent dividers>
            <Alert severity="info" sx={{ mb: 2 }}>
              Ao concluir a O.S., o evento <code>WORK_ORDER_COMPLETED</code> ativará automaticamente o contrato no sistema!
            </Alert>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="MAC da ONU / ONT"
                  required
                  placeholder="AA:BB:CC:DD:EE:FF"
                  value={completeForm.onuMac}
                  onChange={(e) => setCompleteForm({ ...completeForm, onuMac: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Número de Série (Serial)"
                  required
                  placeholder="ZTEG12345678"
                  value={completeForm.onuSerial}
                  onChange={(e) => setCompleteForm({ ...completeForm, onuSerial: e.target.value })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Sinal Óptico Medido (dBm)"
                  required
                  type="number"
                  inputProps={{ step: '0.01' }}
                  value={completeForm.fiberSignalDbm}
                  onChange={(e) => setCompleteForm({ ...completeForm, fiberSignalDbm: e.target.value })}
                  helperText="Valor ideal entre -15.00 e -24.00 dBm"
                />
              </Grid>
              <Grid item xs={12}>
                <Box sx={{ p: 2, bgcolor: '#f0fdf4', border: '1px dashed #22c55e', borderRadius: 2, textAlign: 'center' }}>
                  <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
                    📍 Localização da Instalação (GPS do Técnico)
                  </Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
                    Capture as coordenadas em frente à residência do cliente para registrar o ponto exato da fibra.
                  </Typography>
                  {gpsSuccess ? (
                    <Chip
                      label={`GPS Gravado: ${completeForm.technicianLatitude?.toFixed(6)}, ${completeForm.technicianLongitude?.toFixed(6)}`}
                      color="success"
                      icon={<DoneIcon />}
                    />
                  ) : (
                    <Button
                      variant="outlined"
                      color="success"
                      onClick={handleCaptureGps}
                      disabled={gpsLoading}
                    >
                      {gpsLoading ? 'Capturando Coordenadas...' : 'Marcar GPS em Frente à Casa'}
                    </Button>
                  )}
                </Box>
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={2}
                  label="Observações da Instalação"
                  value={completeForm.notes}
                  onChange={(e) => setCompleteForm({ ...completeForm, notes: e.target.value })}
                  placeholder="Ex: Ponto instalado na sala, sinal excelente, Wi-Fi testado a 580Mbps."
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCompleteModalOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="success">
              Concluir & Ativar Assinante
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};

export default WorkOrderList;
