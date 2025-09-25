import React, { useState, useEffect } from 'react';
import {
  Container,
  Paper,
  Typography,
  Stepper,
  Step,
  StepLabel,
  Button,
  Box,
  TextField,
  Grid,
  Card,
  CardContent,
  Avatar,
  IconButton,
  Alert,
  CircularProgress,
} from '@mui/material';
import { Business, Person, Palette, PhotoCamera } from '@mui/icons-material';
import { Formik, Form, Field } from 'formik';
import * as Yup from 'yup';
import { toast } from 'react-toastify';
import { setupService } from '../../services/setupService';

const steps = ['Usuário Administrador', 'Informações da Empresa', 'Personalização do Site'];

const validationSchemas = [
  // Etapa 1: Usuário Administrador
  Yup.object({
    adminName: Yup.string().required('Nome é obrigatório'),
    adminEmail: Yup.string().email('Email inválido').required('Email é obrigatório'),
    adminPassword: Yup.string().min(6, 'Senha deve ter pelo menos 6 caracteres').required('Senha é obrigatória'),
    confirmPassword: Yup.string()
      .oneOf([Yup.ref('adminPassword'), null], 'Senhas devem coincidir')
      .required('Confirmação de senha é obrigatória')
  }),
  // Etapa 2: Informações da Empresa
  Yup.object({
    companyName: Yup.string().required('Nome da empresa é obrigatório'),
    companyCnpj: Yup.string(),
    companyAddress: Yup.string(),
    companyPhone: Yup.string(),
    companyEmail: Yup.string().email('Email inválido'),
    companyWebsite: Yup.string().url('URL inválida')
  }),
  // Etapa 3: Personalização do Site
  Yup.object({
    siteTitle: Yup.string().required('Título do site é obrigatório'),
    siteDescription: Yup.string(),
    primaryColor: Yup.string(),
    secondaryColor: Yup.string()
  })
];

const initialValues = {
  // Usuário Administrador
  adminName: '',
  adminEmail: '',
  adminPassword: '',
  confirmPassword: '',
  // Informações da Empresa
  companyName: '',
  companyCnpj: '',
  companyAddress: '',
  companyPhone: '',
  companyEmail: '',
  companyWebsite: '',
  // Personalização do Site
  siteTitle: '',
  siteDescription: '',
  primaryColor: '#1976d2',
  secondaryColor: '#dc004e'
};

const Setup = () => {
  const [activeStep, setActiveStep] = useState(0);
  const [setupComplete, setSetupComplete] = useState(false);
  const [loading, setLoading] = useState(false);
  const [checkingStatus, setCheckingStatus] = useState(true);

  useEffect(() => {
    checkSetupStatus();
  }, []);

  const checkSetupStatus = async () => {
    try {
      const status = await setupService.getSetupStatus();
      if (status.isSetupCompleted) {
        setSetupComplete(true);
      }
    } catch (error) {
      console.error('Erro ao verificar status do setup:', error);
    } finally {
      setCheckingStatus(false);
    }
  };

  const handleNext = () => {
    setActiveStep((prevActiveStep) => prevActiveStep + 1);
  };

  const handleBack = () => {
    setActiveStep((prevActiveStep) => prevActiveStep - 1);
  };

  const handleSubmit = async (values) => {
    setLoading(true);
    try {
      const setupData = {
        adminName: values.adminName,
        adminEmail: values.adminEmail,
        adminPassword: values.adminPassword,
        companyName: values.companyName,
        companyCnpj: values.companyCnpj,
        companyAddress: values.companyAddress,
        companyPhone: values.companyPhone,
        companyEmail: values.companyEmail,
        companyWebsite: values.companyWebsite,
        siteTitle: values.siteTitle,
        siteDescription: values.siteDescription,
        primaryColor: values.primaryColor,
        secondaryColor: values.secondaryColor
      };

      const response = await setupService.performSetup(setupData);
      
      if (response.isSetupCompleted) {
        setSetupComplete(true);
        toast.success('Setup concluído com sucesso!');
      } else {
        toast.error(response.message || 'Erro durante o setup');
      }
    } catch (error) {
      console.error('Erro durante o setup:', error);
      toast.error('Erro durante o setup. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  if (checkingStatus) {
    return (
      <Container maxWidth="md" sx={{ mt: 4, display: 'flex', justifyContent: 'center' }}>
        <CircularProgress />
      </Container>
    );
  }

  const renderStepContent = (step, values, setFieldValue, errors, touched) => {
    switch (step) {
      case 0:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Criar Usuário Administrador
            </Typography>
            <Grid container spacing={3}>
              <Grid item xs={12}>
                <Field
                  as={TextField}
                  name="adminName"
                  label="Nome Completo"
                  fullWidth
                  error={touched.adminName && !!errors.adminName}
                  helperText={touched.adminName && errors.adminName}
                />
              </Grid>
              <Grid item xs={12}>
                <Field
                  as={TextField}
                  name="adminEmail"
                  label="Email"
                  type="email"
                  fullWidth
                  error={touched.adminEmail && !!errors.adminEmail}
                  helperText={touched.adminEmail && errors.adminEmail}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Field
                  as={TextField}
                  name="adminPassword"
                  label="Senha"
                  type="password"
                  fullWidth
                  error={touched.adminPassword && !!errors.adminPassword}
                  helperText={touched.adminPassword && errors.adminPassword}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Field
                  as={TextField}
                  name="confirmPassword"
                  label="Confirmar Senha"
                  type="password"
                  fullWidth
                  error={touched.confirmPassword && !!errors.confirmPassword}
                  helperText={touched.confirmPassword && errors.confirmPassword}
                />
              </Grid>
            </Grid>
          </Box>
        );
      case 1:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Informações da Empresa
            </Typography>
            <Grid container spacing={3}>
              <Grid item xs={12}>
                <Field
                  as={TextField}
                  name="companyName"
                  label="Nome da Empresa"
                  fullWidth
                  error={touched.companyName && !!errors.companyName}
                  helperText={touched.companyName && errors.companyName}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Field
                  as={TextField}
                  name="companyCnpj"
                  label="CNPJ"
                  fullWidth
                  error={touched.companyCnpj && !!errors.companyCnpj}
                  helperText={touched.companyCnpj && errors.companyCnpj}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Field
                  as={TextField}
                  name="companyPhone"
                  label="Telefone"
                  fullWidth
                  error={touched.companyPhone && !!errors.companyPhone}
                  helperText={touched.companyPhone && errors.companyPhone}
                />
              </Grid>
              <Grid item xs={12}>
                <Field
                  as={TextField}
                  name="companyAddress"
                  label="Endereço"
                  fullWidth
                  multiline
                  rows={2}
                  error={touched.companyAddress && !!errors.companyAddress}
                  helperText={touched.companyAddress && errors.companyAddress}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Field
                  as={TextField}
                  name="companyEmail"
                  label="Email da Empresa"
                  type="email"
                  fullWidth
                  error={touched.companyEmail && !!errors.companyEmail}
                  helperText={touched.companyEmail && errors.companyEmail}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Field
                  as={TextField}
                  name="companyWebsite"
                  label="Website"
                  fullWidth
                  error={touched.companyWebsite && !!errors.companyWebsite}
                  helperText={touched.companyWebsite && errors.companyWebsite}
                />
              </Grid>
            </Grid>
          </Box>
        );
      case 2:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Personalização do Site
            </Typography>
            <Grid container spacing={3}>
              <Grid item xs={12}>
                <Field
                  as={TextField}
                  name="siteTitle"
                  label="Título do Site"
                  fullWidth
                  error={touched.siteTitle && !!errors.siteTitle}
                  helperText={touched.siteTitle && errors.siteTitle}
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
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Field
                  as={TextField}
                  name="primaryColor"
                  label="Cor Primária"
                  type="color"
                  fullWidth
                  error={touched.primaryColor && !!errors.primaryColor}
                  helperText={touched.primaryColor && errors.primaryColor}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Field
                  as={TextField}
                  name="secondaryColor"
                  label="Cor Secundária"
                  type="color"
                  fullWidth
                  error={touched.secondaryColor && !!errors.secondaryColor}
                  helperText={touched.secondaryColor && errors.secondaryColor}
                />
              </Grid>
            </Grid>
          </Box>
        );
      default:
        return null;
    }
  };

  if (setupComplete) {
    return (
      <Container maxWidth="md" sx={{ mt: 4 }}>
        <Paper elevation={3} sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h4" gutterBottom color="primary">
            ✅ Setup Concluído!
          </Typography>
          <Typography variant="body1" sx={{ mb: 3 }}>
            A configuração inicial foi concluída com sucesso. 
            Você pode agora fazer login no sistema.
          </Typography>
          <Button 
            variant="contained" 
            color="primary" 
            onClick={() => window.location.href = '/login'}
          >
            Ir para Login
          </Button>
        </Paper>
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Typography variant="h4" align="center" gutterBottom>
          Configuração Inicial do Sistema
        </Typography>
        <Typography variant="body1" align="center" color="textSecondary" paragraph>
          Bem-vindo ao ISP ERP! Vamos configurar seu sistema em alguns passos simples.
        </Typography>

        <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
          {steps.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        <Formik
          initialValues={initialValues}
          validationSchema={validationSchemas[activeStep]}
          onSubmit={(values) => {
            if (activeStep === steps.length - 1) {
              handleSubmit(values);
            } else {
              handleNext();
            }
          }}
        >
          {({ values, setFieldValue, errors, touched }) => (
            <Form>
              {renderStepContent(activeStep, values, setFieldValue, errors, touched)}
              
              <Box sx={{ display: 'flex', flexDirection: 'row', pt: 4 }}>
                <Button
                  color="inherit"
                  disabled={activeStep === 0}
                  onClick={handleBack}
                  sx={{ mr: 1 }}
                >
                  Voltar
                </Button>
                <Box sx={{ flex: '1 1 auto' }} />
                <Button type="submit" variant="contained" disabled={loading}>
                  {loading ? (
                    <CircularProgress size={24} />
                  ) : activeStep === steps.length - 1 ? (
                    'Finalizar Setup'
                  ) : (
                    'Próximo'
                  )}
                </Button>
              </Box>
            </Form>
          )}
        </Formik>
      </Paper>
    </Container>
  );
};

export default Setup;