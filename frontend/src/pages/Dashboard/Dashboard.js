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
} from '@mui/material';
import {
  ExitToApp as ExitToAppIcon,
  People,
  AttachMoney,
  Assignment as AssignmentIcon,
  Business,
  Menu as MenuIcon,
  TrendingUp,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { authService } from '../../services/authService';
import { toast } from 'react-toastify';
import Sidebar from '../../components/Sidebar';

// Constantes de largura da sidebar (mesmas do componente Sidebar)
const DRAWER_WIDTH = 280;
const DRAWER_WIDTH_MOBILE = 260;

const Dashboard = () => {
  const { user, logout } = useAuth();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const isTablet = useMediaQuery(theme.breakpoints.between('md', 'lg'));
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile);

  // Calcular largura dinâmica da sidebar
  const sidebarWidth = isMobile ? DRAWER_WIDTH_MOBILE : DRAWER_WIDTH;

  // Ajustar sidebar baseado no tamanho da tela
  useEffect(() => {
    setSidebarOpen(!isMobile);
  }, [isMobile]);

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
  const stats = [
    {
      title: 'Clientes Ativos',
      value: '1,234',
      icon: <People />,
      color: '#1976d2',
      description: 'Total de clientes ativos',
    },
    {
      title: 'Receita Mensal',
      value: 'R$ 45.678',
      icon: <AttachMoney />,
      color: '#2e7d32',
      description: 'Receita do mês atual',
    },
    {
      title: 'Novos Clientes',
      value: '89',
      icon: <TrendingUp />,
      color: '#ed6c02',
      description: 'Novos clientes este mês',
    },
    {
      title: 'Empresas Parceiras',
      value: '12',
      icon: <Business />,
      color: '#9c27b0',
      description: 'Empresas cadastradas',
    },
  ];

  return (
    <Box sx={{ display: 'flex' }}>
      {/* Sidebar */}
      <Sidebar
        open={sidebarOpen}
        onClose={handleSidebarClose}
        onToggle={handleSidebarToggle}
      />

      {/* Conteúdo Principal */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          transition: theme.transitions.create(['margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
          // Layout dinâmico baseado no estado da sidebar - removido marginLeft
          marginLeft: 0, // Removido espaçamento lateral
          minHeight: '100vh',
          bgcolor: 'grey.50',
        }}
      >
        {/* AppBar */}
        <AppBar
          position="fixed"
          sx={{
            zIndex: theme.zIndex.drawer + 1,
            transition: theme.transitions.create(['margin', 'width'], {
              easing: theme.transitions.easing.sharp,
              duration: theme.transitions.duration.leavingScreen,
            }),
            // Layout dinâmico baseado no estado da sidebar - removido marginLeft
            marginLeft: 0, // Removido espaçamento lateral
            width: '100%', // Largura total
          }}
        >
          <Toolbar sx={{ px: { xs: 2, sm: 3 } }}>
            {/* Botão do Menu (visível quando sidebar está fechada ou em mobile) */}
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
              variant={isMobile ? "h6" : "h6"} 
              noWrap 
              component="div" 
              sx={{ 
                flexGrow: 1,
                fontSize: { xs: '1rem', sm: '1.25rem' },
                fontWeight: 'medium',
              }}
            >
              ISP ERP - Dashboard
            </Typography>

            <Typography 
              variant="body2" 
              sx={{ 
                mr: 2,
                display: { xs: 'none', sm: 'block' },
                fontSize: { sm: '0.875rem', md: '1rem' },
              }}
            >
              Olá, {user?.name || 'Usuário'}!
            </Typography>

            <IconButton
              color="inherit"
              onClick={handleLogout}
              aria-label="Sair do sistema"
              sx={{
                '&:hover': {
                  bgcolor: 'rgba(255, 255, 255, 0.1)',
                },
              }}
            >
              <ExitToAppIcon />
            </IconButton>
          </Toolbar>
        </AppBar>
      
        {/* Conteúdo do Dashboard */}
        <Container 
          maxWidth="xl" 
          sx={{ 
            mt: { xs: 8, sm: 8.5 }, // Margem superior para compensar a AppBar fixa
            mb: 4,
            px: { xs: 1, sm: 2, md: 3 }, // Reduzido padding horizontal
          }}
        >
          <Typography 
            variant={isMobile ? "h5" : "h4"} 
            gutterBottom
            sx={{
              mb: { xs: 2, sm: 2.5 },
              fontWeight: 'bold',
              color: 'text.primary',
            }}
          >
            Dashboard Administrativo
          </Typography>

          {/* Cards de Estatísticas */}
          <Grid container spacing={{ xs: 2, sm: 3, md: 4 }} sx={{ mb: { xs: 3, sm: 4 } }}>
            {stats.map((stat, index) => (
              <Grid item xs={12} sm={6} md={3} key={index}>
                <Card
                  sx={{
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    transition: 'transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out',
                    borderRadius: { xs: 2, sm: 3 },
                    '&:hover': {
                      transform: 'translateY(-4px)',
                      boxShadow: theme.shadows[8],
                    },
                  }}
                >
                  <CardContent 
                    sx={{ 
                      flexGrow: 1,
                      p: { xs: 2, sm: 3 },
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                      <Box
                        sx={{
                          p: { xs: 1, sm: 1.5 },
                          borderRadius: 2,
                          bgcolor: stat.color,
                          color: 'white',
                          mr: 2,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        {React.cloneElement(stat.icon, {
                          sx: { fontSize: { xs: 20, sm: 24 } }
                        })}
                      </Box>
                      <Typography 
                        variant={isMobile ? "subtitle1" : "h6"} 
                        component="div"
                        sx={{
                          fontSize: { xs: '0.875rem', sm: '1rem', md: '1.125rem' },
                          fontWeight: 'medium',
                          lineHeight: 1.2,
                        }}
                      >
                        {stat.title}
                      </Typography>
                    </Box>
                    <Typography 
                      variant={isMobile ? "h5" : "h4"} 
                      component="div" 
                      sx={{ 
                        mb: 1,
                        fontWeight: 'bold',
                        color: stat.color,
                        fontSize: { xs: '1.5rem', sm: '2rem', md: '2.125rem' },
                      }}
                    >
                      {stat.value}
                    </Typography>
                    <Typography 
                      variant="body2" 
                      color="text.secondary"
                      sx={{
                        fontSize: { xs: '0.75rem', sm: '0.875rem' },
                        lineHeight: 1.4,
                      }}
                    >
                      {stat.description}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>

          {/* Seção de Atividades Recentes */}
          <Grid container spacing={{ xs: 2, sm: 3, md: 4 }}>
            <Grid item xs={12}>
              <Card
                sx={{
                  borderRadius: { xs: 2, sm: 3 },
                  boxShadow: theme.shadows[2],
                }}
              >
                <CardContent sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
                  <Typography 
                    variant={isMobile ? "h6" : "h5"} 
                    gutterBottom
                    sx={{
                      mb: { xs: 2, sm: 3 },
                      fontWeight: 'bold',
                      color: 'text.primary',
                    }}
                  >
                    Atividades Recentes
                  </Typography>
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      minHeight: { xs: 100, sm: 150, md: 200 },
                      bgcolor: 'grey.50',
                      borderRadius: 2,
                      border: '2px dashed',
                      borderColor: 'grey.300',
                    }}
                  >
                    <Typography 
                      variant="body1" 
                      color="text.secondary"
                      sx={{
                        textAlign: 'center',
                        fontSize: { xs: '0.875rem', sm: '1rem' },
                      }}
                    >
                      Nenhuma atividade recente para exibir
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Container>
      </Box>
    </Box>
  );
};

export default Dashboard;