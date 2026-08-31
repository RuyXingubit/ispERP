import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  Button,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  IconButton,
  Tooltip,
  Divider,
} from '@mui/material';
import {
  Sensors as OnuIcon,
  Route as RouteIcon,
  Engineering as TechIcon,
  Inventory as StockIcon,
  CheckCircle as CheckIcon,
  Warning as WarningIcon,
  DirectionsCar as VehicleIcon,
  Send as DispatchIcon,
  Refresh as RefreshIcon,
  LocationOn as LocationIcon,
} from '@mui/icons-material';
import { toast } from 'react-toastify';
import {
  InstallationMaterialDemand,
  TechnicianDispatchCandidate,
} from '../../types/onboarding-dispatch';
import { onboardingDispatchService } from '../../services/onboardingDispatchService';

export const InstallationDispatchDashboard: React.FC = () => {
  const [demands, setDemands] = useState<InstallationMaterialDemand[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedDemand, setSelectedDemand] = useState<InstallationMaterialDemand | null>(null);
  const [candidates, setCandidates] = useState<TechnicianDispatchCandidate[]>([]);
  const [loadingCandidates, setLoadingCandidates] = useState(false);
  const [dispatching, setDispatching] = useState(false);

  useEffect(() => {
    loadDemands();
  }, []);

  const loadDemands = async () => {
    setLoading(true);
    try {
      const data = await onboardingDispatchService.listDemands();
      setDemands(data);
    } catch (err: any) {
      toast.error('Erro ao carregar demandas de instalação FTTH');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDispatch = async (demand: InstallationMaterialDemand) => {
    setSelectedDemand(demand);
    setLoadingCandidates(true);
    try {
      const list = await onboardingDispatchService.listCandidates(demand.workOrderId);
      setCandidates(list);
    } catch (err: any) {
      toast.error('Erro ao buscar técnicos disponíveis para despacho');
    } finally {
      setLoadingCandidates(false);
    }
  };

  const handleConfirmDispatch = async (candidate: TechnicianDispatchCandidate) => {
    if (!selectedDemand) return;
    setDispatching(true);
    try {
      await onboardingDispatchService.dispatchWorkOrder(
        selectedDemand.workOrderId,
        candidate.technicianId
      );
      toast.success(
        `O.S. despachada com sucesso para ${candidate.technicianName}! Materiais alocados no veículo.`
      );
      setSelectedDemand(null);
      loadDemands();
    } catch (err: any) {
      toast.error('Erro ao despachar O.S.');
    } finally {
      setDispatching(false);
    }
  };

  const pendingCount = demands.filter((d) => d.status === 'PENDING_ALLOCATION').length;
  const allocatedCount = demands.filter((d) => d.status === 'ALLOCATED_VEHICLE').length;
  const avgDrop = demands.length
    ? Math.round(demands.reduce((acc, d) => acc + d.estimatedDropMeters, 0) / demands.length)
    : 50;

  return (
    <Box sx={{ p: 3, maxWidth: 1400, margin: '0 auto' }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <RouteIcon color="primary" fontSize="large" /> Despacho Inteligente de Instalações FTTH
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Triagem automatizada de O.S. com dimensionamento de cabo drop, verificação de estoque em veículos e roteamento.
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={loadDemands}
          disabled={loading}
        >
          Atualizar
        </Button>
      </Box>

      {/* KPI Cards */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card elevation={2} sx={{ borderLeft: '4px solid #ed6c02', bgcolor: 'background.paper' }}>
            <CardContent>
              <Typography variant="overline" color="text.secondary" fontWeight="bold">
                Aguardando Despacho
              </Typography>
              <Typography variant="h4" fontWeight="bold" color="warning.main">
                {pendingCount}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                O.S. com venda assinada via Pix R$ 1,00
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={4}>
          <Card elevation={2} sx={{ borderLeft: '4px solid #0288d1', bgcolor: 'background.paper' }}>
            <CardContent>
              <Typography variant="overline" color="text.secondary" fontWeight="bold">
                Metragem Média de Drop
              </Typography>
              <Typography variant="h4" fontWeight="bold" color="info.main">
                {avgDrop} m
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Distância CTO-Cliente + 20% folga técnica
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={4}>
          <Card elevation={2} sx={{ borderLeft: '4px solid #2e7d32', bgcolor: 'background.paper' }}>
            <CardContent>
              <Typography variant="overline" color="text.secondary" fontWeight="bold">
                Alocados em Veículos
              </Typography>
              <Typography variant="h4" fontWeight="bold" color="success.main">
                {allocatedCount}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Técnicos a caminho com kit FTTH completo
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Demands Table */}
      <Paper elevation={3} sx={{ borderRadius: 2, overflow: 'hidden' }}>
        <TableContainer>
          <Table>
            <TableHead sx={{ bgcolor: 'grey.100' }}>
              <TableRow>
                <TableCell><strong>Contrato / Cliente</strong></TableCell>
                <TableCell><strong>Endereço de Instalação</strong></TableCell>
                <TableCell><strong>Topologia FTTH</strong></TableCell>
                <TableCell><strong>Kit de Materiais</strong></TableCell>
                <TableCell><strong>Status / Alocação</strong></TableCell>
                <TableCell align="center"><strong>Ações</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                    <CircularProgress />
                  </TableCell>
                </TableRow>
              ) : demands.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    Nenhuma instalação pendente de despacho no momento.
                  </TableCell>
                </TableRow>
              ) : (
                demands.map((demand) => (
                  <TableRow key={demand.id} hover>
                    <TableCell>
                      <Typography variant="subtitle2" fontWeight="bold">
                        {demand.customerName || 'Cliente'}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" display="block">
                        {demand.contractNumber || 'Contrato'} • {demand.customerPhone || 'S/ Tel'}
                      </Typography>
                    </TableCell>

                    <TableCell sx={{ maxWidth: 260 }}>
                      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 0.5 }}>
                        <LocationIcon fontSize="small" color="action" />
                        <Typography variant="body2">{demand.customerAddress || 'Endereço não informado'}</Typography>
                      </Box>
                    </TableCell>

                    <TableCell>
                      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                        <Chip
                          size="small"
                          icon={<RouteIcon />}
                          label={`${demand.ctoName || 'CTO'} (Porta ${demand.ctoPortNumber || 1})`}
                          color="primary"
                          variant="outlined"
                        />
                        <Typography variant="caption" color="text.secondary">
                          Cabo Drop: <strong>{demand.estimatedDropMeters} metros</strong> (+20%)
                        </Typography>
                      </Box>
                    </TableCell>

                    <TableCell>
                      <Typography variant="body2" fontWeight="medium">
                        {demand.onuModelRequired}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {demand.fastConnectorsCount}x Conectores SC/APC • {demand.ptoRosetteCount}x Roseta PTO
                      </Typography>
                    </TableCell>

                    <TableCell>
                      {demand.status === 'PENDING_ALLOCATION' ? (
                        <Chip size="small" label="Aguardando Despacho" color="warning" />
                      ) : (
                        <Box>
                          <Chip
                            size="small"
                            icon={<VehicleIcon />}
                            label={demand.allocatedTechnicianName || 'Técnico Alocado'}
                            color="success"
                          />
                          {demand.allocatedWarehouseName && (
                            <Typography variant="caption" color="text.secondary" display="block">
                              {demand.allocatedWarehouseName}
                            </Typography>
                          )}
                        </Box>
                      )}
                    </TableCell>

                    <TableCell align="center">
                      <Button
                        variant="contained"
                        size="small"
                        color="primary"
                        startIcon={<DispatchIcon />}
                        onClick={() => handleOpenDispatch(demand)}
                        disabled={demand.status === 'CONSUMED'}
                      >
                        {demand.status === 'PENDING_ALLOCATION' ? 'Despachar' : 'Redespachar'}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Modal de Seleção de Técnico com Estoque e Proximidade */}
      <Dialog
        open={Boolean(selectedDemand)}
        onClose={() => setSelectedDemand(null)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1.5, bgcolor: 'primary.main', color: 'white' }}>
          <TechIcon /> Despacho Inteligente de O.S. - Seleção de Técnico
        </DialogTitle>
        <DialogContent sx={{ p: 3 }}>
          {selectedDemand && (
            <Box sx={{ mb: 3, p: 2, bgcolor: 'grey.50', borderRadius: 2 }}>
              <Typography variant="subtitle1" fontWeight="bold">
                Instalação para {selectedDemand.customerName}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                📍 {selectedDemand.customerAddress}
              </Typography>
              <Typography variant="body2" color="primary.main" fontWeight="medium" sx={{ mt: 0.5 }}>
                Demanda: {selectedDemand.estimatedDropMeters}m Cabo Drop • {selectedDemand.onuModelRequired} • 2x Conectores
              </Typography>
            </Box>
          )}

          <Typography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
            Técnicos Ranqueados (Disponibilidade no Veículo & Proximidade GPS):
          </Typography>

          {loadingCandidates ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : candidates.length === 0 ? (
            <Typography color="text.secondary" align="center" sx={{ py: 3 }}>
              Nenhum técnico disponível no momento.
            </Typography>
          ) : (
            <Grid container spacing={2}>
              {candidates.map((cand, idx) => (
                <Grid item xs={12} key={cand.technicianId}>
                  <Paper
                    elevation={idx === 0 ? 3 : 1}
                    sx={{
                      p: 2,
                      borderRadius: 2,
                      border: idx === 0 ? '2px solid #2e7d32' : '1px solid #e0e0e0',
                      bgcolor: idx === 0 ? '#f1f8e9' : 'background.paper',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <Box sx={{ flex: 1 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="subtitle1" fontWeight="bold">
                          {cand.technicianName}
                        </Typography>
                        {idx === 0 && (
                          <Chip size="small" label="Mais Recomendado" color="success" />
                        )}
                        <Chip
                          size="small"
                          icon={<VehicleIcon />}
                          label={cand.vehicleWarehouseName || 'Veículo'}
                          variant="outlined"
                        />
                      </Box>

                      {/* Status de Materiais no Veículo */}
                      <Box sx={{ display: 'flex', gap: 1, mt: 1, flexWrap: 'wrap' }}>
                        <Chip
                          size="small"
                          icon={cand.hasCompleteKit ? <CheckIcon /> : <WarningIcon />}
                          label={cand.hasCompleteKit ? 'Kit FTTH Completo no Veículo' : 'Falta Material'}
                          color={cand.hasCompleteKit ? 'success' : 'warning'}
                        />
                        <Chip
                          size="small"
                          icon={<StockIcon />}
                          label={`Bobina Drop: ${cand.dropCableBalanceMeters}m disponível`}
                          variant="outlined"
                        />
                        <Chip
                          size="small"
                          icon={<LocationIcon />}
                          label={`${cand.distanceKmToCustomer} km de distância`}
                          variant="outlined"
                        />
                      </Box>

                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                        Último atendimento: {cand.lastServiceAddress} • Score: <strong>{cand.recommendedScore} pts</strong>
                      </Typography>
                    </Box>

                    <Button
                      variant="contained"
                      color={idx === 0 ? 'success' : 'primary'}
                      startIcon={<DispatchIcon />}
                      onClick={() => handleConfirmDispatch(cand)}
                      disabled={dispatching}
                    >
                      Atribuir O.S.
                    </Button>
                  </Paper>
                </Grid>
              ))}
            </Grid>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setSelectedDemand(null)}>Fechar</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
