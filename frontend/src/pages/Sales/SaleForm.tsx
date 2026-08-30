import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Divider,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
  Alert,
  Paper,
} from '@mui/material';
import {
  ShoppingCartCheckout as SaleIcon,
  CheckCircleOutline as SuccessIcon,
} from '@mui/icons-material';
import planService from '../../services/planService';
import saleService from '../../services/saleService';
import { validateCPF } from '../../utils/cpfValidator';

const SaleForm = () => {
  const [plans, setPlans] = useState([]);
  const [loadingPlans, setLoadingPlans] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [successSale, setSuccessSale] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  const [formData, setFormData] = useState({
    planId: '',
    customerName: '',
    customerCpf: '',
    customerEmail: '',
    customerPhone: '',
    installationAddress: '',
    city: '',
    state: 'SP',
    zipCode: '',
    preferredDueDate: 10,
    notificationChannel: 'WHATSAPP',
    sellerName: 'Balcão / Loja Central',
  });

  useEffect(() => {
    const fetchActivePlans = async () => {
      try {
        const res: any = await planService.getActivePlans();
        const plansList = Array.isArray(res) ? res : (res?.data || []);
        setPlans(plansList);
        if (plansList.length > 0) {
          setFormData((prev) => ({ ...prev, planId: plansList[0].id }));
        }
      } catch (err) {
        setErrorMessage('Erro ao carregar planos disponíveis.');
      } finally {
        setLoadingPlans(false);
      }
    };
    fetchActivePlans();
  }, []);

  const handleChange = (e: any) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: any) => {
    e.preventDefault();
    setErrorMessage(null);

    // Validação de CPF
    if (!validateCPF(formData.customerCpf)) {
      setErrorMessage('CPF inválido. Por favor, verifique os dígitos digitados.');
      return;
    }

    try {
      setSubmitting(true);
      const res: any = await saleService.submitSale(formData as any);
      setSuccessSale(res?.data || res);
    } catch (err) {
      setErrorMessage(err.response?.data || err.message || 'Erro ao processar venda.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReset = () => {
    setSuccessSale(null);
    setFormData({
      planId: plans[0]?.id || '',
      customerName: '',
      customerCpf: '',
      customerEmail: '',
      customerPhone: '',
      installationAddress: '',
      city: '',
      state: 'SP',
      zipCode: '',
      preferredDueDate: 10,
      notificationChannel: 'WHATSAPP',
      sellerName: 'Balcão / Loja Central',
    });
  };

  if (successSale) {
    return (
      <Box sx={{ p: 4, maxWidth: 700, mx: 'auto' }}>
        <Paper elevation={3} sx={{ p: 4, textAlign: 'center', borderRadius: 3 }}>
          <SuccessIcon sx={{ fontSize: 72, color: 'success.main', mb: 2 }} />
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Venda Realizada com Sucesso!
          </Typography>
          <Typography variant="body1" color="text.secondary" paragraph>
            A venda foi registrada e o evento de domínio <code>SALE_SUBMITTED</code> foi publicado na Outbox.
          </Typography>
          <Alert severity="info" sx={{ my: 2, textAlign: 'left' }}>
            <strong>Automação Comercial Ativada:</strong>
            <br />• Cliente <strong>{successSale.customerName}</strong> cadastrado/sincronizado.
            <br />• Contrato gerado automaticamente em status <strong>PENDENTE DE INSTALAÇÃO</strong>.
          </Alert>
          <Button variant="contained" color="primary" onClick={handleReset} sx={{ mt: 2 }}>
            Nova Venda
          </Button>
        </Paper>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3, maxWidth: 900, mx: 'auto' }}>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight="bold" gutterBottom>
          Venda Rápida & Adesão
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Submeta contratações de internet com criação automática de contrato e ordem de instalação.
        </Typography>
      </Box>

      {errorMessage && <Alert severity="error" sx={{ mb: 3 }}>{errorMessage}</Alert>}

      <Card elevation={2} sx={{ borderRadius: 3 }}>
        <CardContent sx={{ p: 4 }}>
          {loadingPlans ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
              <CircularProgress />
            </Box>
          ) : (
            <form onSubmit={handleSubmit}>
              <Typography variant="h6" fontWeight="600" color="primary" gutterBottom>
                1. Plano Escolhido
              </Typography>
              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12}>
                  <FormControl fullWidth required>
                    <InputLabel id="plan-select-label">Selecione o Plano</InputLabel>
                    <Select
                      labelId="plan-select-label"
                      name="planId"
                      value={formData.planId}
                      label="Selecione o Plano"
                      onChange={handleChange}
                    >
                      {plans.map((p) => (
                        <MenuItem key={p.id} value={p.id}>
                          <strong>{p.name}</strong> — {p.downloadSpeed} Mbps (R$ {Number(p.price).toFixed(2)}/mês)
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
              </Grid>

              <Divider sx={{ my: 3 }} />

              <Typography variant="h6" fontWeight="600" color="primary" gutterBottom>
                2. Dados do Cliente
              </Typography>
              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={8}>
                  <TextField
                    fullWidth
                    required
                    label="Nome Completo"
                    name="customerName"
                    value={formData.customerName}
                    onChange={handleChange}
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField
                    fullWidth
                    required
                    label="CPF"
                    name="customerCpf"
                    placeholder="000.000.000-00"
                    value={formData.customerCpf}
                    onChange={handleChange}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="E-mail"
                    type="email"
                    name="customerEmail"
                    value={formData.customerEmail}
                    onChange={handleChange}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    required
                    label="WhatsApp / Celular"
                    name="customerPhone"
                    placeholder="(11) 99999-9999"
                    value={formData.customerPhone}
                    onChange={handleChange}
                  />
                </Grid>
              </Grid>

              <Divider sx={{ my: 3 }} />

              <Typography variant="h6" fontWeight="600" color="primary" gutterBottom>
                3. Endereço de Instalação
              </Typography>
              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={8}>
                  <TextField
                    fullWidth
                    required
                    label="Endereço (Rua, Número, Bairro, Compl.)"
                    name="installationAddress"
                    value={formData.installationAddress}
                    onChange={handleChange}
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField
                    fullWidth
                    required
                    label="CEP"
                    name="zipCode"
                    placeholder="00000-000"
                    value={formData.zipCode}
                    onChange={handleChange}
                  />
                </Grid>
                <Grid item xs={12} sm={8}>
                  <TextField
                    fullWidth
                    required
                    label="Cidade"
                    name="city"
                    value={formData.city}
                    onChange={handleChange}
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField
                    fullWidth
                    required
                    label="UF"
                    name="state"
                    value={formData.state}
                    onChange={handleChange}
                  />
                </Grid>
              </Grid>

              <Divider sx={{ my: 3 }} />

              <Typography variant="h6" fontWeight="600" color="primary" gutterBottom>
                4. Faturamento & Preferências
              </Typography>
              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={4}>
                  <FormControl fullWidth required>
                    <InputLabel id="due-day-label">Dia de Vencimento</InputLabel>
                    <Select
                      labelId="due-day-label"
                      name="preferredDueDate"
                      value={formData.preferredDueDate}
                      label="Dia de Vencimento"
                      onChange={handleChange}
                    >
                      {[5, 10, 15, 20, 25].map((day) => (
                        <MenuItem key={day} value={day}>
                          Todo dia {day}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <FormControl fullWidth required>
                    <InputLabel id="notif-label">Canal de Cobrança</InputLabel>
                    <Select
                      labelId="notif-label"
                      name="notificationChannel"
                      value={formData.notificationChannel}
                      label="Canal de Cobrança"
                      onChange={handleChange}
                    >
                      <MenuItem value="WHATSAPP">WhatsApp</MenuItem>
                      <MenuItem value="EMAIL">E-mail</MenuItem>
                      <MenuItem value="SMS">SMS</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField
                    fullWidth
                    label="Vendedor / Atendente"
                    name="sellerName"
                    value={formData.sellerName}
                    onChange={handleChange}
                  />
                </Grid>
              </Grid>

              <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 4 }}>
                <Button
                  type="submit"
                  variant="contained"
                  color="primary"
                  size="large"
                  startIcon={submitting ? <CircularProgress size={20} color="inherit" /> : <SaleIcon />}
                  disabled={submitting}
                >
                  {submitting ? 'Registrando Venda...' : 'Concluir Venda'}
                </Button>
              </Box>
            </form>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default SaleForm;
