import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container,
  Typography,
  Box,
  Button,
  Paper,
  Grid,
  TextField,
  AppBar,
  Toolbar,
  IconButton,
  useTheme,
  useMediaQuery,
  CircularProgress,
  Card,
  CardContent,
  CardHeader,
  Divider,
} from '@mui/material';
import {
  Menu as MenuIcon,
  ArrowBack as ArrowBackIcon,
  Save as SaveIcon,
  Settings as SettingsIcon,
  Palette as PaletteIcon,
  Info as InfoIcon,
  ContactMail as ContactIcon,
} from '@mui/icons-material';
import { Formik, Form, Field } from 'formik';
import * as Yup from 'yup';
import { toast } from 'react-toastify';
import { useAuth } from '../../contexts/AuthContext';
import Sidebar from '../../components/Sidebar';

// Constantes de largura da sidebar (mesmas do componente Sidebar)
const DRAWER_WIDTH = 280;
const DRAWER_WIDTH_MOBILE = 260;

const validationSchema = Yup.object({
  siteTitle: Yup.string().required('Título do site é obrigatório'),
  siteDescription: Yup.string(),
  primaryColor: Yup.string(),
  secondaryColor: Yup.string(),
  logoUrl: Yup.string().url('URL inválida'),
  contactEmail: Yup.string().email('Email inválido'),
  contactPhone: Yup.string(),
  supportEmail: Yup.string().email('Email inválido'),
  footerText: Yup.string(),
});

const SiteSettings = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile);
  const [settings, setSettings] = useState(null);
  const [loading, setLoading] = useState(true);

  // Calcular largura dinâmica da sidebar
  const sidebarWidth = isMobile ? DRAWER_WIDTH_MOBILE : DRAWER_WIDTH;

  // Ajustar sidebar baseado no tamanho da tela
  useEffect(() => {
    setSidebarOpen(!isMobile);
  }, [isMobile]);

  // Carregar configurações
  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      setLoading(true);
      // Simulando dados para desenvolvimento
      const mockSettings = {
        siteTitle: 'ISP Connect',
        siteDescription: 'Provedor de Internet de Alta Velocidade',
        primaryColor: '#1976d2',
        secondaryColor: '#dc004e',
        logoUrl: '',
        contactEmail: 'contato@ispconnect.com',
        contactPhone: '(11) 99999-9999',
        supportEmail: 'suporte@ispconnect.com',
        footerText: '© 2025 ISP Connect. Todos os direitos reservados.',
      };
      setSettings(mockSettings);
    } catch (error) {
      console.error('Erro ao carregar configurações:', error);
      toast.error('Erro ao carregar configurações');
    } finally {
      setLoading(false);
    }
  };

  const handleSidebarToggle = () => {
    setSidebarOpen(!sidebarOpen);
  };

  const handleSidebarClose = () => {
    setSidebarOpen(false);
  };

  const handleSubmit = async (values, { setSubmitting }) => {
    if (user.role !== 'ADMIN') {
      toast.error('Apenas administradores podem alterar as configurações');
      return;
    }

    try {
      // Simular salvamento
      setSettings(values);
      toast.success('Configurações salvas com sucesso!');
    } catch (error) {
      console.error('Erro ao salvar configurações:', error);
      toast.error('Erro ao salvar configurações');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

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
          marginLeft: 0,
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
            marginLeft: 0,
            width: '100%',
          }}
        >
          <Toolbar>
            <IconButton
              color="inherit"
              aria-label="toggle sidebar"
              onClick={handleSidebarToggle}
              edge="start"
              sx={{ mr: 2 }}
            >
              <MenuIcon />
            </IconButton>
            <IconButton
              color="inherit"
              aria-label="voltar"
              onClick={() => navigate('/dashboard')}
              edge="start"
              sx={{ mr: 2 }}
            >
              <ArrowBackIcon />
            </IconButton>
            <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
              Configurações do Site
            </Typography>
          </Toolbar>
        </AppBar>

        {/* Conteúdo */}
        <Container maxWidth="lg" sx={{ mt: 10, mb: 4, px: { xs: 1, sm: 2 } }}>
          <Box sx={{ mb: 3 }}>
            <Typography variant="h4" component="h1" gutterBottom>
              Configurações do Site
            </Typography>
            <Typography variant="body1" color="text.secondary">
              Personalize as configurações gerais do seu site
            </Typography>
          </Box>

          <Formik
            initialValues={settings}
            validationSchema={validationSchema}
            onSubmit={handleSubmit}
            enableReinitialize
          >
            {({ errors, touched, isSubmitting }) => (
              <Form>
                <Grid container spacing={3}>
                  {/* Informações Gerais */}
                  <Grid item xs={12}>
                    <Card>
                      <CardHeader
                        avatar={<InfoIcon color="primary" />}
                        title="Informações Gerais"
                        subheader="Configurações básicas do site"
                      />
                      <CardContent>
                        <Grid container spacing={2}>
                          <Grid item xs={12} md={6}>
                            <Field
                              as={TextField}
                              name="siteTitle"
                              label="Título do Site"
                              fullWidth
                              error={touched.siteTitle && !!errors.siteTitle}
                              helperText={touched.siteTitle && errors.siteTitle}
                              disabled={user.role !== 'ADMIN'}
                            />
                          </Grid>
                          <Grid item xs={12} md={6}>
                            <Field
                              as={TextField}
                              name="logoUrl"
                              label="URL do Logo"
                              fullWidth
                              error={touched.logoUrl && !!errors.logoUrl}
                              helperText={touched.logoUrl && errors.logoUrl}
                              disabled={user.role !== 'ADMIN'}
                            />
                          </Grid>
                          <Grid item xs={12}>
                            <Field
                              as={TextField}
                              name="siteDescription"
                              label="Descrição do Site"
                              fullWidth
                              multiline
                              rows={3}
                              error={touched.siteDescription && !!errors.siteDescription}
                              helperText={touched.siteDescription && errors.siteDescription}
                              disabled={user.role !== 'ADMIN'}
                            />
                          </Grid>
                          <Grid item xs={12}>
                            <Field
                              as={TextField}
                              name="footerText"
                              label="Texto do Rodapé"
                              fullWidth
                              error={touched.footerText && !!errors.footerText}
                              helperText={touched.footerText && errors.footerText}
                              disabled={user.role !== 'ADMIN'}
                            />
                          </Grid>
                        </Grid>
                      </CardContent>
                    </Card>
                  </Grid>

                  {/* Cores e Tema */}
                  <Grid item xs={12}>
                    <Card>
                      <CardHeader
                        avatar={<PaletteIcon color="primary" />}
                        title="Cores e Tema"
                        subheader="Personalização visual do site"
                      />
                      <CardContent>
                        <Grid container spacing={2}>
                          <Grid item xs={12} md={6}>
                            <Field
                              as={TextField}
                              name="primaryColor"
                              label="Cor Primária"
                              type="color"
                              fullWidth
                              error={touched.primaryColor && !!errors.primaryColor}
                              helperText={touched.primaryColor && errors.primaryColor}
                              disabled={user.role !== 'ADMIN'}
                            />
                          </Grid>
                          <Grid item xs={12} md={6}>
                            <Field
                              as={TextField}
                              name="secondaryColor"
                              label="Cor Secundária"
                              type="color"
                              fullWidth
                              error={touched.secondaryColor && !!errors.secondaryColor}
                              helperText={touched.secondaryColor && errors.secondaryColor}
                              disabled={user.role !== 'ADMIN'}
                            />
                          </Grid>
                        </Grid>
                      </CardContent>
                    </Card>
                  </Grid>

                  {/* Informações de Contato */}
                  <Grid item xs={12}>
                    <Card>
                      <CardHeader
                        avatar={<ContactIcon color="primary" />}
                        title="Informações de Contato"
                        subheader="Dados de contato exibidos no site"
                      />
                      <CardContent>
                        <Grid container spacing={2}>
                          <Grid item xs={12} md={6}>
                            <Field
                              as={TextField}
                              name="contactEmail"
                              label="Email de Contato"
                              type="email"
                              fullWidth
                              error={touched.contactEmail && !!errors.contactEmail}
                              helperText={touched.contactEmail && errors.contactEmail}
                              disabled={user.role !== 'ADMIN'}
                            />
                          </Grid>
                          <Grid item xs={12} md={6}>
                            <Field
                              as={TextField}
                              name="contactPhone"
                              label="Telefone de Contato"
                              fullWidth
                              error={touched.contactPhone && !!errors.contactPhone}
                              helperText={touched.contactPhone && errors.contactPhone}
                              disabled={user.role !== 'ADMIN'}
                            />
                          </Grid>
                          <Grid item xs={12} md={6}>
                            <Field
                              as={TextField}
                              name="supportEmail"
                              label="Email de Suporte"
                              type="email"
                              fullWidth
                              error={touched.supportEmail && !!errors.supportEmail}
                              helperText={touched.supportEmail && errors.supportEmail}
                              disabled={user.role !== 'ADMIN'}
                            />
                          </Grid>
                        </Grid>
                      </CardContent>
                    </Card>
                  </Grid>

                  {/* Botão de Salvar */}
                  {user.role === 'ADMIN' && (
                    <Grid item xs={12}>
                      <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                        <Button
                          type="submit"
                          variant="contained"
                          size="large"
                          startIcon={isSubmitting ? <CircularProgress size={20} /> : <SaveIcon />}
                          disabled={isSubmitting}
                        >
                          {isSubmitting ? 'Salvando...' : 'Salvar Configurações'}
                        </Button>
                      </Box>
                    </Grid>
                  )}

                  {user.role !== 'ADMIN' && (
                    <Grid item xs={12}>
                      <Paper sx={{ p: 2, bgcolor: 'warning.light', color: 'warning.contrastText' }}>
                        <Typography variant="body2">
                          <strong>Aviso:</strong> Apenas administradores podem alterar as configurações do site.
                        </Typography>
                      </Paper>
                    </Grid>
                  )}
                </Grid>
              </Form>
            )}
          </Formik>
        </Container>
      </Box>
    </Box>
  );
};

export default SiteSettings;