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
  AppBar,
  Toolbar,
  Paper,
  Avatar,
  Chip,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import {
  Wifi,
  Speed,
  Support,
  Login as LoginIcon,
  Phone,
  Email,
  LocationOn,
  CheckCircle,
  Router,
  Security,
  TrendingUp,
  Business,
  Schedule,
  Star,
} from '@mui/icons-material';

const Home = () => {
  const navigate = useNavigate();

  const handleLoginClick = () => {
    navigate('/login');
  };

  const plans = [
    {
      name: 'Básico',
      speed: '100 MB',
      price: 'R$ 79,90',
      features: [
        'Download até 100 Mbps',
        'Upload até 50 Mbps',
        'Wi-Fi grátis',
        'Instalação gratuita',
        'Suporte 24h',
      ],
      popular: false,
      color: '#2e7d32',
    },
    {
      name: 'Família',
      speed: '300 MB',
      price: 'R$ 119,90',
      features: [
        'Download até 300 Mbps',
        'Upload até 150 Mbps',
        'Wi-Fi 6 grátis',
        'Instalação gratuita',
        'Suporte 24h',
        'Netflix incluso',
      ],
      popular: true,
      color: '#1976d2',
    },
    {
      name: 'Premium',
      speed: '600 MB',
      price: 'R$ 179,90',
      features: [
        'Download até 600 Mbps',
        'Upload até 300 Mbps',
        'Wi-Fi 6 grátis',
        'Instalação gratuita',
        'Suporte 24h',
        'Netflix + Amazon Prime',
        'IP fixo incluso',
      ],
      popular: false,
      color: '#9c27b0',
    },
  ];

  const services = [
    {
      icon: <Wifi />,
      title: 'Internet Fibra Óptica',
      description: 'Conexão ultra-rápida e estável com tecnologia de ponta.',
      color: '#1976d2',
    },
    {
      icon: <Router />,
      title: 'Wi-Fi Profissional',
      description: 'Equipamentos de última geração para máxima cobertura.',
      color: '#2e7d32',
    },
    {
      icon: <Security />,
      title: 'Segurança Digital',
      description: 'Proteção avançada contra ameaças online.',
      color: '#ed6c02',
    },
    {
      icon: <Support />,
      title: 'Suporte 24/7',
      description: 'Atendimento especializado a qualquer hora do dia.',
      color: '#9c27b0',
    },
  ];

  return (
    <Box>
      {/* Header */}
      <AppBar position="static" elevation={1} sx={{ bgcolor: 'white', color: 'text.primary' }}>
        <Toolbar>
          <Box sx={{ display: 'flex', alignItems: 'center', flexGrow: 1 }}>
            <Wifi sx={{ mr: 1, color: 'primary.main' }} />
            <Typography variant="h6" component="div" sx={{ color: 'primary.main', fontWeight: 'bold' }}>
              ISP Connect
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <Button color="inherit">Planos</Button>
            <Button color="inherit">Cobertura</Button>
            <Button color="inherit">Suporte</Button>
            <Button color="inherit">Contato</Button>
            <Divider orientation="vertical" flexItem />
            <Button 
              variant="contained"
              onClick={handleLoginClick}
              startIcon={<LoginIcon />}
              sx={{ ml: 1 }}
            >
              Área do Cliente
            </Button>
          </Box>
        </Toolbar>
      </AppBar>

      {/* Hero Section */}
      <Box
        sx={{
          background: 'linear-gradient(135deg, #1976d2 0%, #42a5f5 100%)',
          color: 'white',
          py: 10,
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <Container maxWidth="lg">
          <Grid container spacing={6} alignItems="center">
            <Grid item xs={12} md={6}>
              <Typography variant="h2" component="h1" gutterBottom fontWeight="bold">
                Internet de Fibra Óptica
              </Typography>
              <Typography variant="h5" component="h2" gutterBottom sx={{ mb: 3 }}>
                Velocidade real, sem oscilação
              </Typography>
              <Typography variant="body1" paragraph sx={{ fontSize: '1.2rem', mb: 4, opacity: 0.9 }}>
                Conecte-se com a internet mais rápida e estável da região. 
                Planos a partir de R$ 79,90 com instalação gratuita e Wi-Fi incluso.
              </Typography>
              <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                <Button
                  variant="contained"
                  size="large"
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
                  Ver Planos
                </Button>
                <Button
                  variant="outlined"
                  size="large"
                  sx={{
                    borderColor: 'white',
                    color: 'white',
                    px: 4,
                    py: 1.5,
                    fontSize: '1.1rem',
                    '&:hover': {
                      borderColor: 'white',
                      bgcolor: 'rgba(255, 255, 255, 0.1)',
                    },
                  }}
                >
                  Verificar Cobertura
                </Button>
              </Box>
            </Grid>
            <Grid item xs={12} md={6}>
              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  height: '400px',
                }}
              >
                <Paper
                  elevation={8}
                  sx={{
                    p: 4,
                    borderRadius: 4,
                    bgcolor: 'rgba(255, 255, 255, 0.95)',
                    color: 'text.primary',
                    textAlign: 'center',
                    minWidth: '280px',
                  }}
                >
                  <Speed sx={{ fontSize: 60, color: 'primary.main', mb: 2 }} />
                  <Typography variant="h4" gutterBottom color="primary.main" fontWeight="bold">
                    600 MB
                  </Typography>
                  <Typography variant="h6" gutterBottom>
                    Velocidade máxima
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    Download e upload simétrico
                  </Typography>
                </Paper>
              </Box>
            </Grid>
          </Grid>
        </Container>
      </Box>

      {/* Plans Section */}
      <Container maxWidth="lg" sx={{ py: 8 }}>
        <Typography variant="h3" component="h2" align="center" gutterBottom>
          Nossos Planos
        </Typography>
        <Typography 
          variant="body1" 
          align="center" 
          color="textSecondary" 
          paragraph
          sx={{ mb: 6, fontSize: '1.1rem' }}
        >
          Escolha o plano ideal para sua casa ou empresa
        </Typography>

        <Grid container spacing={4} justifyContent="center">
          {plans.map((plan, index) => (
            <Grid item xs={12} sm={6} md={4} key={index}>
              <Card
                sx={{
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  position: 'relative',
                  transition: 'transform 0.2s',
                  '&:hover': {
                    transform: 'translateY(-8px)',
                    boxShadow: 6,
                  },
                  ...(plan.popular && {
                    border: '2px solid',
                    borderColor: 'primary.main',
                  }),
                }}
              >
                {plan.popular && (
                  <Chip
                    label="MAIS POPULAR"
                    color="primary"
                    sx={{
                      position: 'absolute',
                      top: -10,
                      left: '50%',
                      transform: 'translateX(-50%)',
                      fontWeight: 'bold',
                    }}
                  />
                )}
                <CardContent sx={{ flexGrow: 1, textAlign: 'center', pt: plan.popular ? 4 : 3 }}>
                  <Typography variant="h5" component="h3" gutterBottom fontWeight="bold">
                    {plan.name}
                  </Typography>
                  <Typography variant="h3" color="primary" gutterBottom fontWeight="bold">
                    {plan.speed}
                  </Typography>
                  <Typography variant="h4" gutterBottom>
                    {plan.price}
                    <Typography component="span" variant="body2" color="textSecondary">
                      /mês
                    </Typography>
                  </Typography>
                  
                  <List sx={{ mt: 2 }}>
                    {plan.features.map((feature, idx) => (
                      <ListItem key={idx} sx={{ py: 0.5 }}>
                        <ListItemIcon sx={{ minWidth: 36 }}>
                          <CheckCircle color="success" fontSize="small" />
                        </ListItemIcon>
                        <ListItemText 
                          primary={feature} 
                          primaryTypographyProps={{ variant: 'body2' }}
                        />
                      </ListItem>
                    ))}
                  </List>
                  
                  <Button
                    variant={plan.popular ? "contained" : "outlined"}
                    fullWidth
                    size="large"
                    sx={{ mt: 3 }}
                  >
                    Contratar Agora
                  </Button>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>

      {/* Services Section */}
      <Box sx={{ bgcolor: 'grey.50', py: 8 }}>
        <Container maxWidth="lg">
          <Typography variant="h3" component="h2" align="center" gutterBottom>
            Nossos Serviços
          </Typography>
          <Typography 
            variant="body1" 
            align="center" 
            color="textSecondary" 
            paragraph
            sx={{ mb: 6, fontSize: '1.1rem' }}
          >
            Tecnologia de ponta para sua conectividade
          </Typography>

          <Grid container spacing={4}>
            {services.map((service, index) => (
              <Grid item xs={12} sm={6} md={3} key={index}>
                <Card
                  sx={{
                    height: '100%',
                    textAlign: 'center',
                    transition: 'transform 0.2s',
                    '&:hover': {
                      transform: 'translateY(-4px)',
                      boxShadow: 4,
                    },
                  }}
                >
                  <CardContent>
                    <Avatar
                      sx={{
                        bgcolor: service.color,
                        width: 64,
                        height: 64,
                        mx: 'auto',
                        mb: 2,
                      }}
                    >
                      {service.icon}
                    </Avatar>
                    <Typography variant="h6" component="h3" gutterBottom>
                      {service.title}
                    </Typography>
                    <Typography variant="body2" color="textSecondary">
                      {service.description}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* About Section */}
      <Container maxWidth="lg" sx={{ py: 8 }}>
        <Grid container spacing={6} alignItems="center">
          <Grid item xs={12} md={6}>
            <Typography variant="h3" component="h2" gutterBottom>
              Por que escolher a ISP Connect?
            </Typography>
            <Typography variant="body1" paragraph sx={{ fontSize: '1.1rem', mb: 3 }}>
              Somos uma empresa especializada em telecomunicações com mais de 10 anos 
              de experiência no mercado. Nossa missão é levar internet de qualidade 
              para todos os cantos da cidade.
            </Typography>
            <List>
              <ListItem sx={{ px: 0 }}>
                <ListItemIcon>
                  <Star color="primary" />
                </ListItemIcon>
                <ListItemText primary="Mais de 50.000 clientes satisfeitos" />
              </ListItem>
              <ListItem sx={{ px: 0 }}>
                <ListItemIcon>
                  <Schedule color="primary" />
                </ListItemIcon>
                <ListItemText primary="Instalação em até 24 horas" />
              </ListItem>
              <ListItem sx={{ px: 0 }}>
                <ListItemIcon>
                  <TrendingUp color="primary" />
                </ListItemIcon>
                <ListItemText primary="99,9% de disponibilidade da rede" />
              </ListItem>
            </List>
          </Grid>
          <Grid item xs={12} md={6}>
            <Paper
              elevation={4}
              sx={{
                p: 4,
                textAlign: 'center',
                bgcolor: 'primary.main',
                color: 'white',
              }}
            >
              <Business sx={{ fontSize: 60, mb: 2 }} />
              <Typography variant="h4" gutterBottom>
                Área do Cliente
              </Typography>
              <Typography variant="body1" paragraph>
                Acesse sua conta, consulte faturas, solicite suporte técnico 
                e gerencie seus serviços online.
              </Typography>
              <Button
                variant="contained"
                size="large"
                onClick={handleLoginClick}
                sx={{
                  bgcolor: 'white',
                  color: 'primary.main',
                  '&:hover': {
                    bgcolor: 'grey.100',
                  },
                }}
              >
                Acessar Agora
              </Button>
            </Paper>
          </Grid>
        </Grid>
      </Container>

      {/* Contact Section */}
      <Box sx={{ bgcolor: 'grey.100', py: 6 }}>
        <Container maxWidth="lg">
          <Typography variant="h4" component="h2" align="center" gutterBottom>
            Entre em Contato
          </Typography>
          <Grid container spacing={4} sx={{ mt: 2 }}>
            <Grid item xs={12} md={4}>
              <Box sx={{ textAlign: 'center' }}>
                <Phone sx={{ fontSize: 40, color: 'primary.main', mb: 1 }} />
                <Typography variant="h6" gutterBottom>
                  Telefone
                </Typography>
                <Typography variant="body1">
                  (11) 3000-0000
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  Atendimento 24h
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} md={4}>
              <Box sx={{ textAlign: 'center' }}>
                <Email sx={{ fontSize: 40, color: 'primary.main', mb: 1 }} />
                <Typography variant="h6" gutterBottom>
                  E-mail
                </Typography>
                <Typography variant="body1">
                  contato@ispconnect.com.br
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  Resposta em até 2h
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} md={4}>
              <Box sx={{ textAlign: 'center' }}>
                <LocationOn sx={{ fontSize: 40, color: 'primary.main', mb: 1 }} />
                <Typography variant="h6" gutterBottom>
                  Endereço
                </Typography>
                <Typography variant="body1">
                  Rua das Telecomunicações, 123
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  Centro - São Paulo/SP
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </Container>
      </Box>

      {/* Footer */}
      <Box sx={{ bgcolor: 'primary.main', color: 'white', py: 4 }}>
        <Container maxWidth="lg">
          <Grid container spacing={4}>
            <Grid item xs={12} md={6}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <Wifi sx={{ mr: 1 }} />
                <Typography variant="h6" fontWeight="bold">
                  ISP Connect
                </Typography>
              </Box>
              <Typography variant="body2" sx={{ opacity: 0.8 }}>
                Conectando você ao futuro com internet de fibra óptica de alta qualidade.
              </Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="body2" align="right">
                © 2024 ISP Connect. Todos os direitos reservados.
              </Typography>
              <Typography variant="body2" align="right" sx={{ opacity: 0.8, mt: 1 }}>
                CNPJ: 00.000.000/0001-00 | Anatel: 000000
              </Typography>
            </Grid>
          </Grid>
        </Container>
      </Box>
    </Box>
  );
};

export default Home;