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
  useTheme,
  useMediaQuery,
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
  Menu as MenuIcon,
} from '@mui/icons-material';

const Home = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const isTablet = useMediaQuery(theme.breakpoints.between('md', 'lg'));

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
      <AppBar 
        position="static" 
        elevation={1} 
        sx={{ 
          bgcolor: 'white', 
          color: 'text.primary',
        }}
      >
        <Toolbar sx={{ px: { xs: 2, sm: 3 } }}>
          <Box sx={{ display: 'flex', alignItems: 'center', flexGrow: 1 }}>
            <Wifi sx={{ mr: 1, color: 'primary.main', fontSize: { xs: 20, sm: 24 } }} />
            <Typography 
              variant={isMobile ? "subtitle1" : "h6"} 
              component="div" 
              sx={{ 
                color: 'primary.main', 
                fontWeight: 'bold',
                fontSize: { xs: '1.1rem', sm: '1.25rem' },
              }}
            >
              ISP Connect
            </Typography>
          </Box>
          <Box sx={{ 
            display: { xs: 'none', md: 'flex' }, 
            gap: 2, 
            alignItems: 'center' 
          }}>
            <Button color="inherit" sx={{ fontSize: '0.875rem' }}>Planos</Button>
            <Button color="inherit" sx={{ fontSize: '0.875rem' }}>Cobertura</Button>
            <Button color="inherit" sx={{ fontSize: '0.875rem' }}>Suporte</Button>
            <Button color="inherit" sx={{ fontSize: '0.875rem' }}>Contato</Button>
            <Divider orientation="vertical" flexItem />
            <Button 
              variant="contained"
              onClick={handleLoginClick}
              startIcon={<LoginIcon sx={{ fontSize: { xs: 16, sm: 20 } }} />}
              sx={{ 
                ml: 1,
                fontSize: '0.875rem',
                py: 1,
                px: 2,
              }}
            >
              Área do Cliente
            </Button>
          </Box>
          {/* Mobile Menu Button */}
          <Box sx={{ display: { xs: 'flex', md: 'none' } }}>
            <Button 
              variant="contained"
              onClick={handleLoginClick}
              size="small"
              sx={{ 
                fontSize: '0.75rem',
                py: 0.5,
                px: 1.5,
              }}
            >
              Login
            </Button>
          </Box>
        </Toolbar>
      </AppBar>

      {/* Hero Section */}
      <Box
        sx={{
          background: 'linear-gradient(135deg, #1976d2 0%, #42a5f5 100%)',
          color: 'white',
          py: { xs: 6, sm: 8, md: 10 },
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <Container maxWidth="lg" sx={{ px: { xs: 2, sm: 3 } }}>
          <Grid container spacing={{ xs: 4, md: 6 }} alignItems="center">
            <Grid item xs={12} md={6}>
              <Typography 
                variant={isMobile ? "h3" : "h2"} 
                component="h1" 
                gutterBottom 
                fontWeight="bold"
                sx={{
                  fontSize: { xs: '2rem', sm: '2.5rem', md: '3.5rem' },
                  lineHeight: { xs: 1.2, sm: 1.3 },
                }}
              >
                Internet de Fibra Óptica
              </Typography>
              <Typography 
                variant={isMobile ? "h6" : "h5"} 
                component="h2" 
                gutterBottom 
                sx={{ 
                  mb: { xs: 2, sm: 3 },
                  fontSize: { xs: '1.1rem', sm: '1.25rem', md: '1.5rem' },
                }}
              >
                Velocidade real, sem oscilação
              </Typography>
              <Typography 
                variant="body1" 
                paragraph 
                sx={{ 
                  fontSize: { xs: '1rem', sm: '1.1rem', md: '1.2rem' }, 
                  mb: { xs: 3, sm: 4 }, 
                  opacity: 0.9,
                  lineHeight: { xs: 1.5, sm: 1.6 },
                }}
              >
                Conecte-se com a internet mais rápida e estável da região. 
                Planos a partir de R$ 79,90 com instalação gratuita e Wi-Fi incluso.
              </Typography>
              <Box sx={{ 
                display: 'flex', 
                gap: { xs: 1.5, sm: 2 }, 
                flexDirection: { xs: 'column', sm: 'row' },
                alignItems: { xs: 'stretch', sm: 'center' },
              }}>
                <Button
                  variant="contained"
                  size="large"
                  sx={{
                    bgcolor: 'white',
                    color: 'primary.main',
                    fontSize: { xs: '0.875rem', sm: '1rem' },
                    py: { xs: 1.5, sm: 2 },
                    px: { xs: 3, sm: 4 },
                    fontWeight: 'bold',
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
                    fontSize: { xs: '0.875rem', sm: '1rem' },
                    py: { xs: 1.5, sm: 2 },
                    px: { xs: 3, sm: 4 },
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
                  display: { xs: 'none', md: 'flex' },
                  justifyContent: 'center',
                  alignItems: 'center',
                  height: 400,
                }}
              >
                <Speed sx={{ fontSize: 200, opacity: 0.3 }} />
              </Box>
            </Grid>
          </Grid>
        </Container>
      </Box>

      {/* Plans Section */}
      <Container maxWidth="lg" sx={{ py: { xs: 6, sm: 8 }, px: { xs: 2, sm: 3 } }}>
        <Typography 
          variant={isMobile ? "h4" : "h3"} 
          component="h2" 
          align="center" 
          gutterBottom
          sx={{
            fontSize: { xs: '1.75rem', sm: '2.125rem', md: '3rem' },
            fontWeight: 'bold',
          }}
        >
          Nossos Planos
        </Typography>
        <Typography 
          variant="body1" 
          align="center" 
          color="textSecondary" 
          paragraph
          sx={{ 
            mb: { xs: 4, sm: 6 }, 
            fontSize: { xs: '1rem', sm: '1.1rem' },
          }}
        >
          Escolha o plano ideal para sua necessidade
        </Typography>

        <Grid container spacing={{ xs: 2, sm: 3, md: 4 }} justifyContent="center">
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
                      fontSize: { xs: '0.7rem', sm: '0.75rem' },
                    }}
                  />
                )}
                <CardContent sx={{ 
                  flexGrow: 1, 
                  textAlign: 'center', 
                  pt: plan.popular ? { xs: 3, sm: 4 } : { xs: 2, sm: 3 },
                  p: { xs: 2, sm: 3 },
                }}>
                  <Typography 
                    variant={isMobile ? "h6" : "h5"} 
                    component="h3" 
                    gutterBottom 
                    fontWeight="bold"
                    sx={{ fontSize: { xs: '1.1rem', sm: '1.25rem' } }}
                  >
                    {plan.name}
                  </Typography>
                  <Typography 
                    variant={isMobile ? "h4" : "h3"} 
                    color="primary" 
                    gutterBottom 
                    fontWeight="bold"
                    sx={{ fontSize: { xs: '1.75rem', sm: '2.125rem' } }}
                  >
                    {plan.speed}
                  </Typography>
                  <Typography 
                    variant={isMobile ? "h5" : "h4"} 
                    gutterBottom
                    sx={{ fontSize: { xs: '1.25rem', sm: '1.5rem' } }}
                  >
                    {plan.price}
                    <Typography 
                      component="span" 
                      variant="body2" 
                      color="textSecondary"
                      sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}
                    >
                      /mês
                    </Typography>
                  </Typography>
                  
                  <List sx={{ py: { xs: 1, sm: 2 } }}>
                    {plan.features.map((feature, featureIndex) => (
                      <ListItem key={featureIndex} sx={{ py: { xs: 0.25, sm: 0.5 } }}>
                        <ListItemIcon sx={{ minWidth: { xs: 32, sm: 40 } }}>
                          <CheckCircle 
                            color="primary" 
                            sx={{ fontSize: { xs: 16, sm: 20 } }} 
                          />
                        </ListItemIcon>
                        <ListItemText 
                          primary={feature} 
                          primaryTypographyProps={{ 
                            variant: 'body2',
                            fontSize: { xs: '0.8rem', sm: '0.875rem' },
                          }}
                        />
                      </ListItem>
                    ))}
                  </List>
                  
                  <Button
                    variant={plan.popular ? "contained" : "outlined"}
                    fullWidth
                    size="large"
                    sx={{ 
                      mt: { xs: 2, sm: 3 },
                      py: { xs: 1.5, sm: 2 },
                      fontSize: { xs: '0.875rem', sm: '1rem' },
                      fontWeight: 'medium',
                    }}
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
      <Box sx={{ bgcolor: 'grey.50', py: { xs: 6, sm: 8 } }}>
        <Container maxWidth="lg" sx={{ px: { xs: 2, sm: 3 } }}>
          <Typography 
            variant={isMobile ? "h4" : "h3"} 
            component="h2" 
            align="center" 
            gutterBottom
            sx={{
              fontSize: { xs: '1.75rem', sm: '2.125rem', md: '3rem' },
              fontWeight: 'bold',
            }}
          >
            Nossos Serviços
          </Typography>
          <Typography 
            variant="body1" 
            align="center" 
            color="textSecondary" 
            paragraph
            sx={{ 
              mb: { xs: 4, sm: 6 }, 
              fontSize: { xs: '1rem', sm: '1.1rem' },
            }}
          >
            Tecnologia de ponta para sua conectividade
          </Typography>

          <Grid container spacing={{ xs: 2, sm: 3, md: 4 }}>
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
                  <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
                    <Avatar
                      sx={{
                        bgcolor: service.color,
                        width: { xs: 48, sm: 56, md: 64 },
                        height: { xs: 48, sm: 56, md: 64 },
                        mx: 'auto',
                        mb: { xs: 1.5, sm: 2 },
                      }}
                    >
                      {React.cloneElement(service.icon, { 
                        sx: { fontSize: { xs: 24, sm: 28, md: 32 } } 
                      })}
                    </Avatar>
                    <Typography 
                      variant={isMobile ? "subtitle1" : "h6"} 
                      component="h3" 
                      gutterBottom 
                      fontWeight="bold"
                      sx={{ fontSize: { xs: '1rem', sm: '1.1rem', md: '1.25rem' } }}
                    >
                      {service.title}
                    </Typography>
                    <Typography 
                      variant="body2" 
                      color="textSecondary"
                      sx={{ 
                        fontSize: { xs: '0.8rem', sm: '0.875rem' },
                        lineHeight: { xs: 1.4, sm: 1.5 },
                      }}
                    >
                      {service.description}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* Contact Section */}
      <Container maxWidth="lg" sx={{ py: { xs: 6, sm: 8 }, px: { xs: 2, sm: 3 } }}>
        <Grid container spacing={{ xs: 4, md: 6 }} alignItems="center">
          <Grid item xs={12} md={6}>
            <Typography 
              variant={isMobile ? "h4" : "h3"} 
              component="h2" 
              gutterBottom
              sx={{
                fontSize: { xs: '1.75rem', sm: '2.125rem', md: '3rem' },
                fontWeight: 'bold',
              }}
            >
              Entre em Contato
            </Typography>
            <Typography 
              variant="body1" 
              paragraph 
              color="textSecondary"
              sx={{ 
                mb: { xs: 3, sm: 4 }, 
                fontSize: { xs: '1rem', sm: '1.1rem' },
                lineHeight: { xs: 1.5, sm: 1.6 },
              }}
            >
              Nossa equipe está pronta para atender você e esclarecer todas as suas dúvidas. 
              Entre em contato conosco através dos canais abaixo.
            </Typography>

            <Box sx={{ display: 'flex', flexDirection: 'column', gap: { xs: 2, sm: 3 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: { xs: 1.5, sm: 2 } }}>
                <Avatar sx={{ bgcolor: 'primary.main', width: { xs: 40, sm: 48 }, height: { xs: 40, sm: 48 } }}>
                  <Phone sx={{ fontSize: { xs: 20, sm: 24 } }} />
                </Avatar>
                <Box>
                  <Typography 
                    variant="subtitle1" 
                    fontWeight="bold"
                    sx={{ fontSize: { xs: '0.9rem', sm: '1rem' } }}
                  >
                    Telefone
                  </Typography>
                  <Typography 
                    variant="body2" 
                    color="textSecondary"
                    sx={{ fontSize: { xs: '0.8rem', sm: '0.875rem' } }}
                  >
                    (11) 9999-9999
                  </Typography>
                </Box>
              </Box>

              <Box sx={{ display: 'flex', alignItems: 'center', gap: { xs: 1.5, sm: 2 } }}>
                <Avatar sx={{ bgcolor: 'success.main', width: { xs: 40, sm: 48 }, height: { xs: 40, sm: 48 } }}>
                  <Email sx={{ fontSize: { xs: 20, sm: 24 } }} />
                </Avatar>
                <Box>
                  <Typography 
                    variant="subtitle1" 
                    fontWeight="bold"
                    sx={{ fontSize: { xs: '0.9rem', sm: '1rem' } }}
                  >
                    E-mail
                  </Typography>
                  <Typography 
                    variant="body2" 
                    color="textSecondary"
                    sx={{ fontSize: { xs: '0.8rem', sm: '0.875rem' } }}
                  >
                    contato@ispconnect.com.br
                  </Typography>
                </Box>
              </Box>

              <Box sx={{ display: 'flex', alignItems: 'center', gap: { xs: 1.5, sm: 2 } }}>
                <Avatar sx={{ bgcolor: 'warning.main', width: { xs: 40, sm: 48 }, height: { xs: 40, sm: 48 } }}>
                  <LocationOn sx={{ fontSize: { xs: 20, sm: 24 } }} />
                </Avatar>
                <Box>
                  <Typography 
                    variant="subtitle1" 
                    fontWeight="bold"
                    sx={{ fontSize: { xs: '0.9rem', sm: '1rem' } }}
                  >
                    Endereço
                  </Typography>
                  <Typography 
                    variant="body2" 
                    color="textSecondary"
                    sx={{ fontSize: { xs: '0.8rem', sm: '0.875rem' } }}
                  >
                    Rua das Fibras, 123 - Centro
                  </Typography>
                </Box>
              </Box>
            </Box>
          </Grid>

          <Grid item xs={12} md={6}>
            <Paper 
              elevation={3} 
              sx={{ 
                p: { xs: 3, sm: 4 }, 
                borderRadius: { xs: 2, sm: 3 },
              }}
            >
              <Typography 
                variant={isMobile ? "h6" : "h5"} 
                component="h3" 
                gutterBottom 
                fontWeight="bold"
                sx={{ fontSize: { xs: '1.1rem', sm: '1.25rem' } }}
              >
                Solicite um Orçamento
              </Typography>
              <Typography 
                variant="body2" 
                color="textSecondary" 
                paragraph
                sx={{ 
                  mb: { xs: 2, sm: 3 }, 
                  fontSize: { xs: '0.8rem', sm: '0.875rem' },
                }}
              >
                Preencha o formulário e nossa equipe entrará em contato
              </Typography>

              <Box component="form" sx={{ display: 'flex', flexDirection: 'column', gap: { xs: 2, sm: 3 } }}>
                <Grid container spacing={{ xs: 1.5, sm: 2 }}>
                  <Grid item xs={12} sm={6}>
                    <Typography 
                      variant="body2" 
                      sx={{ 
                        mb: 0.5, 
                        fontWeight: 'medium',
                        fontSize: { xs: '0.8rem', sm: '0.875rem' },
                      }}
                    >
                      Nome
                    </Typography>
                    <Box
                      sx={{
                        border: '1px solid',
                        borderColor: 'grey.300',
                        borderRadius: 1,
                        p: { xs: 1, sm: 1.5 },
                        fontSize: { xs: '0.8rem', sm: '0.875rem' },
                        '&:focus-within': {
                          borderColor: 'primary.main',
                        },
                      }}
                    >
                      <input
                        type="text"
                        placeholder="Seu nome completo"
                        style={{
                          border: 'none',
                          outline: 'none',
                          width: '100%',
                          fontSize: 'inherit',
                          fontFamily: 'inherit',
                        }}
                      />
                    </Box>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography 
                      variant="body2" 
                      sx={{ 
                        mb: 0.5, 
                        fontWeight: 'medium',
                        fontSize: { xs: '0.8rem', sm: '0.875rem' },
                      }}
                    >
                      Telefone
                    </Typography>
                    <Box
                      sx={{
                        border: '1px solid',
                        borderColor: 'grey.300',
                        borderRadius: 1,
                        p: { xs: 1, sm: 1.5 },
                        fontSize: { xs: '0.8rem', sm: '0.875rem' },
                        '&:focus-within': {
                          borderColor: 'primary.main',
                        },
                      }}
                    >
                      <input
                        type="tel"
                        placeholder="(11) 99999-9999"
                        style={{
                          border: 'none',
                          outline: 'none',
                          width: '100%',
                          fontSize: 'inherit',
                          fontFamily: 'inherit',
                        }}
                      />
                    </Box>
                  </Grid>
                </Grid>

                <Box>
                  <Typography 
                    variant="body2" 
                    sx={{ 
                      mb: 0.5, 
                      fontWeight: 'medium',
                      fontSize: { xs: '0.8rem', sm: '0.875rem' },
                    }}
                  >
                    E-mail
                  </Typography>
                  <Box
                    sx={{
                      border: '1px solid',
                      borderColor: 'grey.300',
                      borderRadius: 1,
                      p: { xs: 1, sm: 1.5 },
                      fontSize: { xs: '0.8rem', sm: '0.875rem' },
                      '&:focus-within': {
                        borderColor: 'primary.main',
                      },
                    }}
                  >
                    <input
                      type="email"
                      placeholder="seu@email.com"
                      style={{
                        border: 'none',
                        outline: 'none',
                        width: '100%',
                        fontSize: 'inherit',
                        fontFamily: 'inherit',
                      }}
                    />
                  </Box>
                </Box>

                <Box>
                  <Typography 
                    variant="body2" 
                    sx={{ 
                      mb: 0.5, 
                      fontWeight: 'medium',
                      fontSize: { xs: '0.8rem', sm: '0.875rem' },
                    }}
                  >
                    Mensagem
                  </Typography>
                  <Box
                    sx={{
                      border: '1px solid',
                      borderColor: 'grey.300',
                      borderRadius: 1,
                      p: { xs: 1, sm: 1.5 },
                      fontSize: { xs: '0.8rem', sm: '0.875rem' },
                      '&:focus-within': {
                        borderColor: 'primary.main',
                      },
                    }}
                  >
                    <textarea
                      rows={4}
                      placeholder="Conte-nos sobre suas necessidades..."
                      style={{
                        border: 'none',
                        outline: 'none',
                        width: '100%',
                        resize: 'vertical',
                        fontSize: 'inherit',
                        fontFamily: 'inherit',
                      }}
                    />
                  </Box>
                </Box>

                <Button
                  variant="contained"
                  size="large"
                  fullWidth
                  sx={{ 
                    py: { xs: 1.5, sm: 2 },
                    fontSize: { xs: '0.875rem', sm: '1rem' },
                    fontWeight: 'medium',
                  }}
                >
                  Enviar Solicitação
                </Button>
              </Box>
            </Paper>
          </Grid>
        </Grid>
      </Container>

      {/* Footer */}
      <Box sx={{ bgcolor: 'grey.900', color: 'white', py: { xs: 4, sm: 6 } }}>
        <Container maxWidth="lg" sx={{ px: { xs: 2, sm: 3 } }}>
          <Grid container spacing={{ xs: 3, sm: 4 }}>
            <Grid item xs={12} md={4}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: { xs: 2, sm: 3 } }}>
                <Wifi sx={{ mr: 1, color: 'primary.main', fontSize: { xs: 24, sm: 28 } }} />
                <Typography 
                  variant={isMobile ? "h6" : "h5"} 
                  component="div" 
                  sx={{ 
                    color: 'primary.main', 
                    fontWeight: 'bold',
                    fontSize: { xs: '1.1rem', sm: '1.25rem' },
                  }}
                >
                  ISP Connect
                </Typography>
              </Box>
              <Typography 
                variant="body2" 
                sx={{ 
                  mb: { xs: 2, sm: 3 }, 
                  opacity: 0.8,
                  fontSize: { xs: '0.8rem', sm: '0.875rem' },
                  lineHeight: { xs: 1.4, sm: 1.5 },
                }}
              >
                Conectando você ao futuro com a melhor internet de fibra óptica da região. 
                Velocidade, estabilidade e suporte de qualidade.
              </Typography>
              <Box sx={{ display: 'flex', gap: 1 }}>
                {[1, 2, 3, 4].map((item) => (
                  <Avatar 
                    key={item}
                    sx={{ 
                      bgcolor: 'primary.main', 
                      width: { xs: 32, sm: 36 }, 
                      height: { xs: 32, sm: 36 },
                      cursor: 'pointer',
                      '&:hover': { bgcolor: 'primary.dark' },
                    }}
                  >
                    <Star sx={{ fontSize: { xs: 16, sm: 18 } }} />
                  </Avatar>
                ))}
              </Box>
            </Grid>

            <Grid item xs={12} sm={6} md={4}>
              <Typography 
                variant="h6" 
                gutterBottom 
                fontWeight="bold"
                sx={{ fontSize: { xs: '1rem', sm: '1.1rem' } }}
              >
                Links Rápidos
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: { xs: 0.5, sm: 1 } }}>
                {['Planos', 'Cobertura', 'Suporte', 'Sobre Nós', 'Contato'].map((link) => (
                  <Typography 
                    key={link}
                    variant="body2" 
                    sx={{ 
                      opacity: 0.8, 
                      cursor: 'pointer',
                      fontSize: { xs: '0.8rem', sm: '0.875rem' },
                      '&:hover': { opacity: 1, color: 'primary.main' },
                    }}
                  >
                    {link}
                  </Typography>
                ))}
              </Box>
            </Grid>

            <Grid item xs={12} sm={6} md={4}>
              <Typography 
                variant="h6" 
                gutterBottom 
                fontWeight="bold"
                sx={{ fontSize: { xs: '1rem', sm: '1.1rem' } }}
              >
                Contato
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: { xs: 1, sm: 1.5 } }}>
                <Typography 
                  variant="body2" 
                  sx={{ 
                    opacity: 0.8,
                    fontSize: { xs: '0.8rem', sm: '0.875rem' },
                  }}
                >
                  📞 (11) 9999-9999
                </Typography>
                <Typography 
                  variant="body2" 
                  sx={{ 
                    opacity: 0.8,
                    fontSize: { xs: '0.8rem', sm: '0.875rem' },
                  }}
                >
                  ✉️ contato@ispconnect.com.br
                </Typography>
                <Typography 
                  variant="body2" 
                  sx={{ 
                    opacity: 0.8,
                    fontSize: { xs: '0.8rem', sm: '0.875rem' },
                  }}
                >
                  📍 Rua das Fibras, 123 - Centro
                </Typography>
              </Box>
            </Grid>
          </Grid>

          <Divider sx={{ my: { xs: 3, sm: 4 }, bgcolor: 'grey.700' }} />
          
          <Box sx={{ 
            display: 'flex', 
            flexDirection: { xs: 'column', sm: 'row' },
            justifyContent: 'space-between', 
            alignItems: { xs: 'center', sm: 'center' },
            gap: { xs: 2, sm: 0 },
          }}>
            <Typography 
              variant="body2" 
              sx={{ 
                opacity: 0.6,
                fontSize: { xs: '0.75rem', sm: '0.8rem' },
                textAlign: { xs: 'center', sm: 'left' },
              }}
            >
              © 2024 ISP Connect. Todos os direitos reservados.
            </Typography>
            <Box sx={{ 
              display: 'flex', 
              gap: { xs: 2, sm: 3 },
              flexWrap: 'wrap',
              justifyContent: { xs: 'center', sm: 'flex-end' },
            }}>
              <Typography 
                variant="body2" 
                sx={{ 
                  opacity: 0.6, 
                  cursor: 'pointer',
                  fontSize: { xs: '0.75rem', sm: '0.8rem' },
                  '&:hover': { opacity: 1 },
                }}
              >
                Política de Privacidade
              </Typography>
              <Typography 
                variant="body2" 
                sx={{ 
                  opacity: 0.6, 
                  cursor: 'pointer',
                  fontSize: { xs: '0.75rem', sm: '0.8rem' },
                  '&:hover': { opacity: 1 },
                }}
              >
                Termos de Uso
              </Typography>
            </Box>
          </Box>
        </Container>
      </Box>
    </Box>
  );
};

export default Home;