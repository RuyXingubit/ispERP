import React, { useEffect, useState } from 'react';
import {
  Container,
  Typography,
  Grid,
  Card,
  CardContent,
  AppBar,
  Toolbar,
  IconButton,
  Box,
  useTheme,
  useMediaQuery,
  Button,
  Chip,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  CircularProgress,
  Alert
} from '@mui/material';
import {
  ExitToApp as ExitToAppIcon,
  People,
  AttachMoney,
  Menu as MenuIcon,
  TrendingUp,
  Warning as WarningIcon,
  Sensors as OnuIcon,
  QrCode as QrCodeIcon,
  Refresh as RefreshIcon,
  SignalCellularConnectedNoInternet0Bar as SignalAlertIcon,
  TrendingDown as ChurnIcon
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { toast } from 'react-toastify';
import Sidebar from '../../components/Sidebar';
import dashboardBiService from '../../services/dashboardBiService';

const DRAWER_WIDTH = 280;
const DRAWER_WIDTH_MOBILE = 260;

const Dashboard = () => {
  const { user, logout } = useAuth();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile);
  const [loading, setLoading] = useState(true);
  const [metrics, setMetrics] = useState(null);

  useEffect(() => {
    setSidebarOpen(!isMobile);
  }, [isMobile]);

  const loadMetrics = async () => {
    try {
      setLoading(true);
      const data = await dashboardBiService.getMetrics();
      setMetrics(data);
    } catch (error) {
      console.error('Erro ao carregar métricas de BI:', error);
      toast.error('Erro ao conectar com módulo de BI');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMetrics();
  }, []);

  const handleSidebarToggle = () => {
    setSidebarOpen(!sidebarOpen);
  };

  const handleSidebarClose = () => {
    setSidebarOpen(false);
  };

  const handleLogout = async () => {
    try {
      await logout();
      toast.success('Logout realizado com sucesso!');
    } catch (error) {
      console.error('Erro no logout:', error);
      toast.error('Erro ao fazer logout');
    }
  };

  const mrrFormatted = metrics?.mrr ? `R$ ${Number(metrics.mrr).toFixed(2).replace('.', ',')}` : 'R$ 0,00';
  const arrFormatted = metrics?.arr ? `R$ ${Number(metrics.arr).toFixed(2).replace('.', ',')}` : 'R$ 0,00';
  const arpuFormatted = metrics?.arpu ? `R$ ${Number(metrics.arpu).toFixed(2).replace('.', ',')}` : 'R$ 0,00';
  const overdueFormatted = metrics?.overdueAmount ? `R$ ${Number(metrics.overdueAmount).toFixed(2).replace('.', ',')}` : 'R$ 0,00';

  const executiveStats = [
    {
      title: 'MRR (Receita Recorrente)',
      value: mrrFormatted,
      subtitle: `ARR Projeção: ${arrFormatted}`,
      icon: <AttachMoney />,
      color: '#2e7d32',
    },
    {
      title: 'ARPU (Ticket Médio)',
      value: arpuFormatted,
      subtitle: `${metrics?.activeContracts || 0} contratos ativos`,
      icon: <People />,
      color: '#1976d2',
    },
    {
      title: 'Inadimplência (% e R$)',
      value: `${metrics?.defaultRate || 0}%`,
      subtitle: `Total Vencido: ${overdueFormatted}`,
      icon: <WarningIcon />,
      color: '#d32f2f',
    },
    {
      title: 'Churn Rate (30 dias)',
      value: `${metrics?.churnRate || 0}%`,
      subtitle: `${metrics?.canceledContractsLast30Days || 0} cancelamentos`,
      icon: <ChurnIcon />,
      color: '#ed6c02',
    },
    {
      title: 'Conversão Xingubit Pay (Pix)',
      value: `${metrics?.pixConversionRate || 100}%`,
      subtitle: `R$ ${Number(metrics?.totalReceivedMonth || 0).toFixed(2).replace('.', ',')} recebidos este mês`,
      icon: <QrCodeIcon />,
      color: '#00897b',
    },
    {
      title: 'Saúde NOC & Rede Óptica',
      value: `${metrics?.provisionedOnus || 0} ONUs`,
      subtitle: `${metrics?.criticalSignalOnus || 0} com sinal atenuado (< -25 dBm)`,
      icon: <OnuIcon />,
      color: metrics?.criticalSignalOnus > 0 ? '#e65100' : '#43a047',
    },
  ];

  return (
    <Box sx={{ display: 'flex' }}>
      <Sidebar
        open={sidebarOpen}
        onClose={handleSidebarClose}
        onToggle={handleSidebarToggle}
      />

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          marginLeft: 0,
          minHeight: '100vh',
          bgcolor: 'grey.50',
        }}
      >
        <AppBar
          position="fixed"
          sx={{
            zIndex: theme.zIndex.drawer + 1,
            marginLeft: 0,
            width: '100%',
          }}
        >
          <Toolbar sx={{ px: { xs: 2, sm: 3 } }}>
            {(!sidebarOpen || isMobile) && (
              <IconButton
                color="inherit"
                aria-label="Abrir menu"
                onClick={handleSidebarToggle}
                edge="start"
                sx={{ mr: 2 }}
              >
                <MenuIcon />
              </IconButton>
            )}

            <Typography 
              variant="h6" 
              noWrap 
              component="div" 
              sx={{ 
                flexGrow: 1,
                fontSize: { xs: '1rem', sm: '1.25rem' },
                fontWeight: 'bold',
              }}
            >
              ISP ERP • Cockpit Executivo & BI
            </Typography>

            <Chip
              label={`Perfil: ${user?.role || 'ADMIN'}`}
              color="secondary"
              size="small"
              sx={{ mr: 2, fontWeight: 'bold' }}
            />

            <Typography 
              variant="body2" 
              sx={{ 
                mr: 2,
                display: { xs: 'none', sm: 'block' },
              }}
            >
              Olá, {user?.name || 'Gestor'}!
            </Typography>

            <IconButton
              color="inherit"
              onClick={handleLogout}
              aria-label="Sair do sistema"
            >
              <ExitToAppIcon />
            </IconButton>
          </Toolbar>
        </AppBar>

        <Container 
          maxWidth="xl" 
          sx={{ 
            mt: { xs: 8, sm: 9 },
            mb: 4,
            px: { xs: 1, sm: 2, md: 3 },
          }}
        >
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Box>
              <Typography variant="h4" fontWeight="bold" color="text.primary">
                Painel de Controle Executivo
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Visão consolidada de indicadores financeiros, comerciais e de NOC em tempo real.
              </Typography>
            </Box>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={loadMetrics}
              disabled={loading}
              sx={{ borderRadius: 2, fontWeight: 'bold' }}
            >
              Atualizar Métricas
            </Button>
          </Box>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 8 }}>
              <CircularProgress size={48} />
            </Box>
          ) : (
            <>
              {/* Cards de Estatísticas Executivas */}
              <Grid container spacing={3} sx={{ mb: 4 }}>
                {executiveStats.map((stat, index) => (
                  <Grid item xs={12} sm={6} md={4} key={index}>
                    <Card
                      sx={{
                        borderRadius: 3,
                        boxShadow: 2,
                        borderLeft: `6px solid ${stat.color}`,
                        transition: 'transform 0.2s',
                        '&:hover': { transform: 'translateY(-4px)', boxShadow: 6 }
                      }}
                    >
                      <CardContent sx={{ p: 2.5 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
                          <Typography variant="subtitle2" color="text.secondary" fontWeight="bold">
                            {stat.title}
                          </Typography>
                          <Box
                            sx={{
                              p: 1,
                              borderRadius: 2,
                              bgcolor: `${stat.color}15`,
                              color: stat.color,
                              display: 'flex',
                              alignItems: 'center'
                            }}
                          >
                            {stat.icon}
                          </Box>
                        </Box>
                        <Typography variant="h4" fontWeight="bold" sx={{ color: stat.color, my: 0.5 }}>
                          {stat.value}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {stat.subtitle}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>

              {/* Tabelas de Ação Rápida */}
              <Grid container spacing={3}>
                {/* Cobranças Vencidas Recentes */}
                <Grid item xs={12} md={6}>
                  <Card sx={{ borderRadius: 3, height: '100%' }}>
                    <CardContent sx={{ p: 3 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                        <Typography variant="h6" fontWeight="bold">
                          Inadimplência • Ação Rápida
                        </Typography>
                        <Chip label="Top Vencidas" color="error" size="small" />
                      </Box>

                      {metrics?.recentOverdueInvoices?.length === 0 ? (
                        <Alert severity="success" sx={{ borderRadius: 2 }}>
                          Nenhuma fatura em atraso pendente de cobrança!
                        </Alert>
                      ) : (
                        <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2 }}>
                          <Table size="small">
                            <TableHead>
                              <TableRow>
                                <TableCell><strong>Vencimento</strong></TableCell>
                                <TableCell><strong>Valor</strong></TableCell>
                                <TableCell align="right"><strong>Ação</strong></TableCell>
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {metrics?.recentOverdueInvoices?.map((inv, idx) => (
                                <TableRow key={idx}>
                                  <TableCell>{inv.dueDate}</TableCell>
                                  <TableCell sx={{ color: 'error.main', fontWeight: 'bold' }}>
                                    R$ {Number(inv.amount).toFixed(2).replace('.', ',')}
                                  </TableCell>
                                  <TableCell align="right">
                                    <Button size="small" variant="contained" color="success" href="/invoices">
                                      Disparar Pix
                                    </Button>
                                  </TableCell>
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </CardContent>
                  </Card>
                </Grid>

                {/* Alertas Críticos do NOC */}
                <Grid item xs={12} md={6}>
                  <Card sx={{ borderRadius: 3, height: '100%' }}>
                    <CardContent sx={{ p: 3 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                        <Typography variant="h6" fontWeight="bold">
                          Alertas do NOC • Sinal Atenuado
                        </Typography>
                        <Chip label="Atenção" color="warning" size="small" />
                      </Box>

                      {metrics?.criticalSignalAlerts?.length === 0 ? (
                        <Alert severity="success" sx={{ borderRadius: 2 }}>
                          Todas as ONUs operando com excelente nível de potência óptica!
                        </Alert>
                      ) : (
                        <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2 }}>
                          <Table size="small">
                            <TableHead>
                              <TableRow>
                                <TableCell><strong>MAC / Serial</strong></TableCell>
                                <TableCell><strong>Sinal dBm</strong></TableCell>
                                <TableCell align="right"><strong>NOC</strong></TableCell>
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {metrics?.criticalSignalAlerts?.map((alert, idx) => (
                                <TableRow key={idx}>
                                  <TableCell>{alert.mac} ({alert.serial})</TableCell>
                                  <TableCell sx={{ color: 'warning.dark', fontWeight: 'bold' }}>
                                    {alert.rxPowerDbm} dBm
                                  </TableCell>
                                  <TableCell align="right">
                                    <Button size="small" variant="outlined" color="primary" href="/onus">
                                      Diagnosticar
                                    </Button>
                                  </TableCell>
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
            </>
          )}
        </Container>
      </Box>
    </Box>
  );
};

export default Dashboard;