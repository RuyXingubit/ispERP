import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Grid,
  IconButton,
  Switch,
  TextField,
  Typography,
  Alert,
} from '@mui/material';
import {
  AccountBalance as BankIcon,
  Edit as EditIcon,
  Lock as LockIcon,
  CheckCircle as ActiveIcon,
} from '@mui/icons-material';
import paymentGatewayService from '../../services/paymentGatewayService';

const GatewayConfig = () => {
  const [configs, setConfigs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [openModal, setOpenModal] = useState(false);
  const [editingConfig, setEditingConfig] = useState(null);

  const [formData, setFormData] = useState({
    name: '',
    gatewayType: 'XINGUBIT_PAY',
    apiKey: '',
    secretKey: '',
    webhookSecret: '',
    pixKey: '',
    sandbox: true,
    active: true,
  });

  const loadConfigs = async () => {
    try {
      setLoading(true);
      const res = await paymentGatewayService.getAllConfigs();
      setConfigs(res.data || []);
      setError(null);
    } catch (err) {
      setError('Erro ao carregar configurações de gateways.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadConfigs();
  }, []);

  const handleOpenEdit = (config) => {
    setEditingConfig(config);
    setFormData({
      name: config.name,
      gatewayType: config.gatewayType,
      apiKey: config.apiKey || '',
      secretKey: config.secretKey || '',
      webhookSecret: config.webhookSecret || '',
      pixKey: config.pixKey || '',
      sandbox: config.sandbox,
      active: config.active,
    });
    setOpenModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...editingConfig,
        ...formData,
      };
      await paymentGatewayService.saveConfig(payload);
      setOpenModal(false);
      loadConfigs();
    } catch (err) {
      alert('Erro ao salvar configuração do gateway: ' + (err.response?.data || err.message));
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight="bold" gutterBottom>
          Gateways de Pagamento (Multi-Gateway)
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Configure as chaves de API, segredos de webhook e chaves Pix para múltiplos provedores (Xingubit Pay, Asaas, etc.).
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 5 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Grid container spacing={3}>
          {configs.map((config) => (
            <Grid item xs={12} md={6} key={config.id}>
              <Card sx={{ borderRadius: 2, boxShadow: 2, height: '100%' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <BankIcon color="primary" />
                      <Typography variant="h6" fontWeight="bold">
                        {config.name}
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', gap: 1 }}>
                      <Chip
                        label={config.sandbox ? 'Sandbox / Testes' : 'Produção'}
                        color={config.sandbox ? 'warning' : 'success'}
                        size="small"
                      />
                      <Chip
                        label={config.active ? 'Ativo' : 'Inativo'}
                        color={config.active ? 'primary' : 'default'}
                        size="small"
                      />
                    </Box>
                  </Box>

                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>Tipo:</strong> {config.gatewayType}
                  </Typography>

                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>Chave Pix:</strong> {config.pixKey || 'Não configurada'}
                  </Typography>

                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>Webhook Secret:</strong> {config.webhookSecret ? '••••••••••••••••' : 'Não configurado'}
                  </Typography>

                  <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                    <Button
                      size="small"
                      variant="outlined"
                      startIcon={<EditIcon />}
                      onClick={() => handleOpenEdit(config)}
                    >
                      Configurar Chaves
                    </Button>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Modal de Configuração */}
      <Dialog open={openModal} onClose={() => setOpenModal(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleSave}>
          <DialogTitle>Configurar Gateway: {formData.name}</DialogTitle>
          <DialogContent dividers>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Nome da Configuração"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Chave Pix (para emissão COB/COBV)"
                  value={formData.pixKey}
                  onChange={(e) => setFormData({ ...formData, pixKey: e.target.value })}
                  placeholder="Ex: pix@xingubit.com.br"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="API Key / Client ID"
                  value={formData.apiKey}
                  onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Secret Key / Client Secret"
                  type="password"
                  value={formData.secretKey}
                  onChange={(e) => setFormData({ ...formData, secretKey: e.target.value })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Webhook Secret (para validação HMAC)"
                  value={formData.webhookSecret}
                  onChange={(e) => setFormData({ ...formData, webhookSecret: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.sandbox}
                      onChange={(e) => setFormData({ ...formData, sandbox: e.target.checked })}
                    />
                  }
                  label="Modo Sandbox (Ambiente de Testes)"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.active}
                      onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                      color="primary"
                    />
                  }
                  label="Gateway Ativo"
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenModal(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Salvar Configuração
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};

export default GatewayConfig;
