import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  CircularProgress,
  Alert,
  Snackbar,
  Paper,
  Switch,
  FormControlLabel
} from '@mui/material';
import {
  Add as AddIcon,
  WhatsApp as WhatsAppIcon,
  Edit as EditIcon,
  CheckCircle as CheckCircleIcon
} from '@mui/icons-material';
import notificationConfigService from '../../services/notificationConfigService';

const NotificationConfigList = () => {
  const [configs, setConfigs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openModal, setOpenModal] = useState(false);
  const [editingConfig, setEditingConfig] = useState(null);

  const [formData, setFormData] = useState({
    name: '',
    providerType: 'TWILIO',
    accountSid: '',
    authToken: '',
    apiUrl: '',
    apiToken: '',
    fromPhoneNumber: 'whatsapp:+14155238886',
    active: true
  });

  const [toast, setToast] = useState({ open: false, message: '', severity: 'success' });

  const loadConfigs = async () => {
    try {
      setLoading(true);
      const data = await notificationConfigService.getAll();
      setConfigs(data);
    } catch (err) {
      console.error('Erro ao carregar configurações de notificações:', err);
      setToast({
        open: true,
        message: 'Erro ao carregar configurações de WhatsApp',
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadConfigs();
  }, []);

  const handleOpenModal = (config = null) => {
    if (config) {
      setEditingConfig(config);
      setFormData({
        name: config.name || '',
        providerType: config.providerType || 'TWILIO',
        accountSid: config.accountSid || '',
        authToken: config.authToken || '',
        apiUrl: config.apiUrl || '',
        apiToken: config.apiToken || '',
        fromPhoneNumber: config.fromPhoneNumber || '',
        active: config.active ?? true
      });
    } else {
      setEditingConfig(null);
      setFormData({
        name: '',
        providerType: 'TWILIO',
        accountSid: '',
        authToken: '',
        apiUrl: '',
        apiToken: '',
        fromPhoneNumber: 'whatsapp:+14155238886',
        active: true
      });
    }
    setOpenModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      if (editingConfig) {
        await notificationConfigService.update(editingConfig.id, formData);
        setToast({ open: true, message: 'Provedor atualizado com sucesso!', severity: 'success' });
      } else {
        await notificationConfigService.create(formData);
        setToast({ open: true, message: 'Provedor configurado com sucesso!', severity: 'success' });
      }
      setOpenModal(false);
      loadConfigs();
    } catch (err) {
      setToast({
        open: true,
        message: 'Erro ao salvar configuração: ' + (err.response?.data?.message || err.message),
        severity: 'error'
      });
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 8 }}>
        <CircularProgress size={48} />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 4, maxWidth: 1200, mx: 'auto' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Notificações & Mensageria WhatsApp
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Gerenciamento de provedores (Twilio, Evolution API, Z-API) para envio automático de cobranças Pix e avisos.
          </Typography>
        </Box>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpenModal()}
          sx={{ borderRadius: 2, fontWeight: 'bold' }}
        >
          Novo Provedor
        </Button>
      </Box>

      <Grid container spacing={3}>
        {configs.map((cfg) => (
          <Grid item xs={12} md={4} key={cfg.id}>
            <Card
              sx={{
                borderRadius: 3,
                borderTop: cfg.active ? '5px solid #2e7d32' : '5px solid #9e9e9e',
                height: '100%'
              }}
            >
              <CardContent sx={{ p: 3 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Chip
                    icon={<WhatsAppIcon />}
                    label={cfg.providerType}
                    color={cfg.providerType === 'TWILIO' ? 'secondary' : 'primary'}
                    size="small"
                  />
                  <Chip
                    label={cfg.active ? 'Ativo' : 'Inativo'}
                    color={cfg.active ? 'success' : 'default'}
                    size="small"
                  />
                </Box>

                <Typography variant="h6" fontWeight="bold" gutterBottom>
                  {cfg.name}
                </Typography>

                <Paper variant="outlined" sx={{ p: 1.5, my: 2, borderRadius: 2, bgcolor: 'background.default' }}>
                  <Typography variant="caption" color="text.secondary" display="block">
                    Número Remetente:
                  </Typography>
                  <Typography variant="body2" fontWeight="bold">
                    {cfg.fromPhoneNumber || 'N/A'}
                  </Typography>
                  {cfg.accountSid && (
                    <>
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                        Account SID (Twilio):
                      </Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {cfg.accountSid}
                      </Typography>
                    </>
                  )}
                  {cfg.apiUrl && (
                    <>
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                        Endpoint URL:
                      </Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {cfg.apiUrl}
                      </Typography>
                    </>
                  )}
                </Paper>

                <Button
                  variant="outlined"
                  fullWidth
                  startIcon={<EditIcon />}
                  onClick={() => handleOpenModal(cfg)}
                  sx={{ mt: 1, borderRadius: 2 }}
                >
                  Editar Configuração
                </Button>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* MODAL CONFIGURAÇÃO PROVEDOR */}
      <Dialog
        open={openModal}
        onClose={() => setOpenModal(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { borderRadius: 3, p: 2 } }}
      >
        <DialogTitle sx={{ fontWeight: 'bold' }}>
          {editingConfig ? 'Editar Provedor WhatsApp' : 'Novo Provedor WhatsApp'}
        </DialogTitle>
        <Box component="form" onSubmit={handleSave}>
          <DialogContent>
            <TextField
              label="Nome do Provedor"
              fullWidth
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
              sx={{ mb: 2 }}
            />

            <FormControl fullWidth sx={{ mb: 2 }}>
              <InputLabel>Tipo de Provedor</InputLabel>
              <Select
                value={formData.providerType}
                label="Tipo de Provedor"
                onChange={(e) => setFormData({ ...formData, providerType: e.target.value })}
              >
                <MenuItem value="TWILIO">Twilio WhatsApp (Oficial)</MenuItem>
                <MenuItem value="EVOLUTION_API">Evolution API (Self-Hosted / Baileys)</MenuItem>
                <MenuItem value="Z_API">Z-API Gateway</MenuItem>
                <MenuItem value="MOCK">Simulador (Mock)</MenuItem>
              </Select>
            </FormControl>

            {formData.providerType === 'TWILIO' && (
              <>
                <TextField
                  label="Account SID"
                  fullWidth
                  value={formData.accountSid}
                  onChange={(e) => setFormData({ ...formData, accountSid: e.target.value })}
                  sx={{ mb: 2 }}
                />
                <TextField
                  label="Auth Token"
                  type="password"
                  fullWidth
                  value={formData.authToken}
                  onChange={(e) => setFormData({ ...formData, authToken: e.target.value })}
                  sx={{ mb: 2 }}
                />
                <TextField
                  label="Número Remetente (Ex: whatsapp:+14155238886)"
                  fullWidth
                  value={formData.fromPhoneNumber}
                  onChange={(e) => setFormData({ ...formData, fromPhoneNumber: e.target.value })}
                  sx={{ mb: 2 }}
                />
              </>
            )}

            {(formData.providerType === 'EVOLUTION_API' || formData.providerType === 'Z_API') && (
              <>
                <TextField
                  label="API URL (Ex: http://evolution-api:8080)"
                  fullWidth
                  value={formData.apiUrl}
                  onChange={(e) => setFormData({ ...formData, apiUrl: e.target.value })}
                  sx={{ mb: 2 }}
                />
                <TextField
                  label="API Token / Key"
                  type="password"
                  fullWidth
                  value={formData.apiToken}
                  onChange={(e) => setFormData({ ...formData, apiToken: e.target.value })}
                  sx={{ mb: 2 }}
                />
                <TextField
                  label="Número Remetente / Instância"
                  fullWidth
                  value={formData.fromPhoneNumber}
                  onChange={(e) => setFormData({ ...formData, fromPhoneNumber: e.target.value })}
                  sx={{ mb: 2 }}
                />
              </>
            )}

            <FormControlLabel
              control={
                <Switch
                  checked={formData.active}
                  onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                  color="primary"
                />
              }
              label="Provedor Ativo"
            />
          </DialogContent>
          <DialogActions sx={{ p: 2 }}>
            <Button onClick={() => setOpenModal(false)} color="inherit">
              Cancelar
            </Button>
            <Button type="submit" variant="contained" color="primary" sx={{ fontWeight: 'bold', borderRadius: 2 }}>
              Salvar Configuração
            </Button>
          </DialogActions>
        </Box>
      </Dialog>

      <Snackbar
        open={toast.open}
        autoHideDuration={4000}
        onClose={() => setToast({ ...toast, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert severity={toast.severity} sx={{ borderRadius: 2, fontWeight: 'bold' }}>
          {toast.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default NotificationConfigList;
