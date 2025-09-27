import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container,
  Typography,
  Button,
  Box,
  Grid,
  Card,
  CardContent,
  CardActions,
  AppBar,
  Toolbar,
  Paper,
  Avatar,
} from '@mui/material';
import {
  Business,
  People,
  TrendingUp,
  Security,
  Speed,
  Support,
  Login as LoginIcon,
} from '@mui/icons-material';

const Home = () => {
  const navigate = useNavigate();

  const handleLoginClick = () => {
    navigate('/login');
  };

  const features = [
    {
      icon: <Business />,
      title: 'Gestão Empresarial',
      description: 'Sistema completo para gerenciamento de empresas e processos.',
      color: '#1976d2',
    },
    {
      icon: <People />,
      title: 'Controle de Usuários',
      description: 'Gerencie usuários, permissões e acessos de forma segura.',
      color: '#2e7d32',
    },
    {
      icon: <TrendingUp />,
      title: 'Relatórios Avançados',
      description: 'Análises detalhadas e relatórios em tempo real.',
      color: '#ed6c02',
    },
    {
      icon: <Security />,
      title: 'Segurança Avançada',
      description: 'Proteção de dados com criptografia e autenticação JWT.',
      color: '#9c27b0',
    },
    {
      icon: <Speed />,
      title: 'Alta Performance',
      description: 'Sistema otimizado para máxima velocidade e eficiência.',
      color: '#d32f2f',
    },
    {
      icon: <Support />,
      title: 'Suporte 24/7',
      description: 'Suporte técnico disponível a qualquer momento.',
      color: '#0288d1',
    },
  ];

  return (
    <Box>
      {/* Header */}
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            ISP ERP
          </Typography>
          <Button 
            color="inherit" 
            onClick={handleLoginClick}
            startIcon={<LoginIcon />}
          >
            Entrar
          </Button>
        </Toolbar>
      </AppBar>

      {/* Hero Section */}
      <Box
        sx={{
          background: 'linear-gradient(135deg, #1976d2 0%, #42a5f5 100%)',
          color: 'white',
          py: 8,
        }}
      >
        <Container maxWidth="lg">
          <Grid container spacing={4} alignItems="center">
            <Grid item xs={12} md={6}>
              <Typography variant="h2" component="h1" gutterBottom fontWeight="bold">
                ISP ERP
              </Typography>
              <Typography variant="h5" component="h2" gutterBottom>
                Sistema de Gestão Empresarial Completo
              </Typography>
              <Typography variant="body1" paragraph sx={{ fontSize: '1.2rem', mb: 4 }}>
                Gerencie sua empresa de forma eficiente com nossa plataforma 
                integrada de ERP. Controle financeiro, estoque, vendas e muito mais 
                em um só lugar.
              </Typography>
              <Button
                variant="contained"
                size="large"
                onClick={handleLoginClick}
                sx={{
                  bgcolor: 'white',
                  color: 'primary.main',
                  px: 4,
                  py: 1.5,
                  fontSize: '1.1rem',
                  '&:hover': {
                    bgcolor: 'grey.100',
                  },
                }}
              >
                Acessar Sistema
              </Button>
            </Grid>
            <Grid item xs={12} md={6}>
              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  height: '300px',
                }}
              >
                <Paper
                  elevation={8}
                  sx={{
                    p: 4,
                    borderRadius: 4,
                    bgcolor: 'rgba(255, 255, 255, 0.1)',
                    backdropFilter: 'blur(10px)',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                  }}
                >
                  <Typography variant="h4" align="center" gutterBottom>
                    🚀
                  </Typography>
                  <Typography variant="h6" align="center">
                    Pronto para começar?
                  </Typography>
                  <Typography variant="body2" align="center" sx={{ mt: 1 }}>
                    Faça login e explore todas as funcionalidades
                  </Typography>
                </Paper>
              </Box>
            </Grid>
          </Grid>
        </Container>
      </Box>

      {/* Features Section */}
      <Container maxWidth="lg" sx={{ py: 8 }}>
        <Typography variant="h3" component="h2" align="center" gutterBottom>
          Funcionalidades
        </Typography>
        <Typography 
          variant="body1" 
          align="center" 
          color="textSecondary" 
          paragraph
          sx={{ mb: 6, fontSize: '1.1rem' }}
        >
          Descubra tudo o que nosso sistema pode fazer pela sua empresa
        </Typography>

        <Grid container spacing={4}>
          {features.map((feature, index) => (
            <Grid item xs={12} sm={6} md={4} key={index}>
              <Card
                sx={{
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  transition: 'transform 0.2s',
                  '&:hover': {
                    transform: 'translateY(-4px)',
                    boxShadow: 4,
                  },
                }}
              >
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Avatar
                      sx={{
                        bgcolor: feature.color,
                        mr: 2,
                      }}
                    >
                      {feature.icon}
                    </Avatar>
                    <Typography variant="h6" component="h3">
                      {feature.title}
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="textSecondary">
                    {feature.description}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>

      {/* Call to Action */}
      <Box sx={{ bgcolor: 'grey.100', py: 6 }}>
        <Container maxWidth="md">
          <Paper elevation={2} sx={{ p: 6, textAlign: 'center' }}>
            <Typography variant="h4" component="h2" gutterBottom>
              Pronto para começar?
            </Typography>
            <Typography variant="body1" paragraph color="textSecondary">
              Acesse o sistema e comece a gerenciar sua empresa de forma mais eficiente.
            </Typography>
            <Button
              variant="contained"
              size="large"
              onClick={handleLoginClick}
              sx={{ mt: 2, px: 4, py: 1.5 }}
            >
              Fazer Login
            </Button>
          </Paper>
        </Container>
      </Box>

      {/* Footer */}
      <Box sx={{ bgcolor: 'primary.main', color: 'white', py: 4 }}>
        <Container maxWidth="lg">
          <Typography variant="body2" align="center">
            © 2024 ISP ERP. Todos os direitos reservados.
          </Typography>
        </Container>
      </Box>
    </Box>
  );
};

export default Home;