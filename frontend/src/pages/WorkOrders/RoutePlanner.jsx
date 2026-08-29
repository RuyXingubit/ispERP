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
  Checkbox,
  CircularProgress,
  Alert,
  Divider
} from '@mui/material';
import {
  Route as RouteIcon,
  Navigation as NavigationIcon,
  CheckCircle as CheckIcon,
  Schedule as ScheduleIcon,
  LocationOn as LocationIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';
import { geoCepService } from '../../services/geoCepService';
import { workOrderService } from '../../services/workOrderService';
import { toast } from 'react-toastify';

const RoutePlanner = () => {
  const [loading, setLoading] = useState(true);
  const [optimizing, setOptimizing] = useState(false);
  const [workOrders, setWorkOrders] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [currentRoute, setCurrentRoute] = useState(null);
  const [routeStops, setRouteStops] = useState([]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [woRes, routesRes] = await Promise.all([
        workOrderService.getAllWorkOrders(),
        geoCepService.getRoutesByDate(new Date().toISOString().split('T')[0])
      ]);

      const scheduledOrders = (woRes.data || []).filter(
        (w) => w.status === 'SCHEDULED' || w.status === 'PENDING'
      );
      setWorkOrders(scheduledOrders);
      setSelectedIds(scheduledOrders.map((w) => w.id));

      if (routesRes.data && routesRes.data.length > 0) {
        const latestRoute = routesRes.data[0];
        setCurrentRoute(latestRoute);
        const stopsRes = await geoCepService.getStopsByRouteId(latestRoute.id);
        setRouteStops(stopsRes.data || []);
      }
    } catch (err) {
      console.error('Erro ao carregar roteirizador:', err);
      toast.error('Erro ao carregar ordens de serviço para roteirização.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleToggleSelect = (id) => {
    if (selectedIds.includes(id)) {
      setSelectedIds(selectedIds.filter((item) => item !== id));
    } else {
      setSelectedIds([...selectedIds, id]);
    }
  };

  const handleOptimizeRoute = async () => {
    if (selectedIds.length === 0) {
      toast.warning('Selecione pelo menos uma Ordem de Serviço para roteirizar.');
      return;
    }

    try {
      setOptimizing(true);
      const res = await geoCepService.createOptimizedRoute({
        routeDate: new Date().toISOString().split('T')[0],
        originLatitude: -3.2033, // Altamira - Depósito Central
        originLongitude: -52.2064,
        workOrderIds: selectedIds,
      });

      toast.success('Melhor rota calculada pelo GeoCEP com sucesso!');
      setCurrentRoute(res.data);
      const stopsRes = await geoCepService.getStopsByRouteId(res.data.id);
      setRouteStops(stopsRes.data || []);
    } catch (err) {
      toast.error('Erro ao calcular rota: ' + (err.response?.data?.message || err.message));
    } finally {
      setOptimizing(false);
    }
  };

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Roteirizador Inteligente de O.S. (GeoCEP)
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Otimização matemática de rotas (TSP) pelo menor caminho para equipes técnicas e frotas.
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={loadData}
          disabled={loading}
          sx={{ borderRadius: 2 }}
        >
          Atualizar
        </Button>
      </Box>

      {/* Cards de Resumo da Rota Otimizada */}
      {currentRoute && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12} sm={4}>
            <Card sx={{ borderRadius: 3, borderLeft: '6px solid #1976d2' }}>
              <CardContent sx={{ p: 2 }}>
                <Typography variant="caption" color="text.secondary" fontWeight="bold">
                  CÓDIGO DA ROTA
                </Typography>
                <Typography variant="h5" fontWeight="bold" color="primary">
                  {currentRoute.code}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Status: {currentRoute.status}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={4}>
            <Card sx={{ borderRadius: 3, borderLeft: '6px solid #2e7d32' }}>
              <CardContent sx={{ p: 2 }}>
                <Typography variant="caption" color="text.secondary" fontWeight="bold">
                  DISTÂNCIA TOTAL (MENOR TRAJETO)
                </Typography>
                <Typography variant="h5" fontWeight="bold" color="success.main">
                  {currentRoute.totalDistanceKm} km
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Calculado pela malha viária GeoCEP / OSM
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={4}>
            <Card sx={{ borderRadius: 3, borderLeft: '6px solid #ed6c02' }}>
              <CardContent sx={{ p: 2 }}>
                <Typography variant="caption" color="text.secondary" fontWeight="bold">
                  TEMPO ESTIMADO DE ATENDIMENTO
                </Typography>
                <Typography variant="h5" fontWeight="bold" sx={{ color: '#ed6c02' }}>
                  {Math.floor(currentRoute.estimatedDurationMinutes / 60)}h{' '}
                  {currentRoute.estimatedDurationMinutes % 60}min
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Deslocamento + tempo de execução
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Grid container spacing={3}>
          {/* Lado Esquerdo: Seleção de O.S. do Dia */}
          <Grid item xs={12} md={5}>
            <Paper elevation={2} sx={{ p: 2.5, borderRadius: 3, height: '100%' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6" fontWeight="bold">
                  Ordens Agendadas para Hoje ({workOrders.length})
                </Typography>
              </Box>

              {workOrders.length === 0 ? (
                <Alert severity="info" sx={{ borderRadius: 2 }}>
                  Nenhuma ordem de serviço pendente de roteirização.
                </Alert>
              ) : (
                <TableContainer sx={{ maxHeight: 400 }}>
                  <Table size="small">
                    <TableHead sx={{ bgcolor: '#f8f9fa' }}>
                      <TableRow>
                        <TableCell padding="checkbox">
                          <Checkbox
                            checked={selectedIds.length === workOrders.length && workOrders.length > 0}
                            onChange={(e) => {
                              if (e.target.checked) setSelectedIds(workOrders.map((w) => w.id));
                              else setSelectedIds([]);
                            }}
                          />
                        </TableCell>
                        <TableCell><strong>O.S. / Tipo</strong></TableCell>
                        <TableCell><strong>Equipe</strong></TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {workOrders.map((wo) => (
                        <TableRow key={wo.id} hover onClick={() => handleToggleSelect(wo.id)} sx={{ cursor: 'pointer' }}>
                          <TableCell padding="checkbox">
                            <Checkbox checked={selectedIds.includes(wo.id)} />
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2" fontWeight="bold">{wo.code}</Typography>
                            <Typography variant="caption" color="text.secondary">{wo.type}</Typography>
                          </TableCell>
                          <TableCell>
                            <Chip label={wo.assignedTeam || 'Sem equipe'} size="small" variant="outlined" />
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}

              <Box sx={{ mt: 3 }}>
                <Button
                  fullWidth
                  variant="contained"
                  color="primary"
                  size="large"
                  startIcon={optimizing ? <CircularProgress size={20} color="inherit" /> : <NavigationIcon />}
                  disabled={optimizing || selectedIds.length === 0}
                  onClick={handleOptimizeRoute}
                  sx={{ borderRadius: 2.5, py: 1.5, fontWeight: 'bold' }}
                >
                  {optimizing ? 'Otimizando com GeoCEP...' : `Otimizar Rota com ${selectedIds.length} Visitas`}
                </Button>
              </Box>
            </Paper>
          </Grid>

          {/* Lado Direito: Sequência Otimizada (1, 2, 3, 4, 5) */}
          <Grid item xs={12} md={7}>
            <Paper elevation={2} sx={{ p: 2.5, borderRadius: 3, height: '100%' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6" fontWeight="bold">
                  Sequência de Visitas Ordenada (Menor Trajeto)
                </Typography>
                {routeStops.length > 0 && (
                  <Chip
                    label={`${routeStops.length} Paradas`}
                    color="success"
                    size="small"
                    icon={<CheckIcon />}
                  />
                )}
              </Box>

              {routeStops.length === 0 ? (
                <Box sx={{ textAlign: 'center', py: 6 }}>
                  <RouteIcon sx={{ fontSize: 60, color: 'text.secondary', mb: 1, opacity: 0.5 }} />
                  <Typography variant="body1" color="text.secondary">
                    Selecione as ordens de serviço à esquerda e clique em <strong>Otimizar Rota</strong> para gerar a sequência do dia.
                  </Typography>
                </Box>
              ) : (
                <Box>
                  <Box sx={{ mb: 2, p: 1.5, bgcolor: '#e3f2fd', borderRadius: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                    <LocationIcon color="primary" />
                    <Typography variant="body2" fontWeight="bold" color="primary.main">
                      Ponto de Partida: Depósito Central Altamira (PA)
                    </Typography>
                  </Box>

                  {routeStops.map((stop) => (
                    <Card
                      key={stop.id}
                      sx={{
                        mb: 1.5,
                        borderRadius: 2,
                        border: '1px solid #e0e0e0',
                        boxShadow: 'none',
                        '&:hover': { bgcolor: '#fafafa' },
                      }}
                    >
                      <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                            <Box
                              sx={{
                                width: 32,
                                height: 32,
                                borderRadius: '50%',
                                bgcolor: '#1976d2',
                                color: '#fff',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                fontWeight: 'bold',
                                fontSize: '0.9rem',
                              }}
                            >
                              {stop.sequenceOrder}
                            </Box>
                            <Box>
                              <Typography variant="subtitle2" fontWeight="bold">
                                {stop.customerName}
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                📍 {stop.address || 'Endereço registrado na O.S.'}
                              </Typography>
                            </Box>
                          </Box>
                          <Chip
                            label={stop.completed ? 'Concluída' : 'Aguardando Visita'}
                            color={stop.completed ? 'success' : 'default'}
                            size="small"
                          />
                        </Box>
                      </CardContent>
                    </Card>
                  ))}
                </Box>
              )}
            </Paper>
          </Grid>
        </Grid>
      )}
    </Box>
  );
};

export default RoutePlanner;
