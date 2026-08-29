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
  FormControl,
  FormControlLabel,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Switch,
  TextField,
  Typography,
  Alert,
} from '@mui/material';
import {
  Storage as OltIcon,
  Add as AddIcon,
  CheckCircle as ActiveIcon,
} from '@mui/icons-material';
import networkService from '../../services/networkService';

const NetworkDeviceList = () => {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [openModal, setOpenModal] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    deviceType: 'OLT',
    driverType: 'SMARTOLT',
    ipAddress: '',
    apiPort: 443,
    apiToken: '',
    snmpCommunity: 'public',
    active: true,
  });

  const loadDevices = async () => {
    try {
      setLoading(true);
      const res = await networkService.getAllDevices();
      setDevices(res.data || []);
      setError(null);
    } catch (err) {
      setError('Erro ao carregar equipamentos de rede.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDevices();
  }, []);

  const handleOpenNew = () => {
    setFormData({
      name: '',
      deviceType: 'OLT',
      driverType: 'SMARTOLT',
      ipAddress: '',
      apiPort: 443,
      apiToken: '',
      snmpCommunity: 'public',
      active: true,
    });
    setOpenModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      await networkService.saveDevice(formData);
      setOpenModal(false);
      loadDevices();
    } catch (err) {
      alert('Erro ao salvar equipamento de rede.');
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <div>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            OLTs & Concentradores de Rede
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Configuração de servidores de provisionamento (SmartOLT, MikroTik RouterOS e Microserviço dedicado).
          </Typography>
        </div>

        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleOpenNew}
        >
          Novo Equipamento
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 5 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Grid container spacing={3}>
          {devices.map((dev) => (
            <Grid item xs={12} md={6} key={dev.id}>
              <Card sx={{ borderRadius: 2, boxShadow: 2, height: '100%' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <OltIcon color="primary" />
                      <Typography variant="h6" fontWeight="bold">
                        {dev.name}
                      </Typography>
                    </Box>
                    <Chip
                      label={dev.active ? 'Operacional' : 'Inativo'}
                      color={dev.active ? 'success' : 'default'}
                      size="small"
                    />
                  </Box>

                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>Driver de Provisionamento:</strong> {dev.driverType}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>Tipo:</strong> {dev.deviceType}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>IP / Host:</strong> {dev.ipAddress}:{dev.apiPort}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>Comunidade SNMP:</strong> {dev.snmpCommunity || 'public'}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Modal de Criação */}
      <Dialog open={openModal} onClose={() => setOpenModal(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleSave}>
          <DialogTitle>Cadastrar Novo Equipamento de Rede</DialogTitle>
          <DialogContent dividers>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Nome da OLT / Concentrador"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth>
                  <InputLabel>Driver de Rede</InputLabel>
                  <Select
                    value={formData.driverType}
                    label="Driver de Rede"
                    onChange={(e) => setFormData({ ...formData, driverType: e.target.value })}
                  >
                    <MenuItem value="SMARTOLT">SmartOLT (Cloud API)</MenuItem>
                    <MenuItem value="EXTERNAL_MICROSERVICE">Microserviço Externo Dedicado</MenuItem>
                    <MenuItem value="MIKROTIK_ROUTEROS">MikroTik RouterOS API</MenuItem>
                    <MenuItem value="RADIUS">Servidor RADIUS</MenuItem>
                    <MenuItem value="MOCK">Driver Simulado (Mock)</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth>
                  <InputLabel>Tipo de Dispositivo</InputLabel>
                  <Select
                    value={formData.deviceType}
                    label="Tipo de Dispositivo"
                    onChange={(e) => setFormData({ ...formData, deviceType: e.target.value })}
                  >
                    <MenuItem value="OLT">OLT GPON / EPON</MenuItem>
                    <MenuItem value="BRAS_PPPOE">Concentrador BRAS / PPPoE</MenuItem>
                    <MenuItem value="RADIUS_SERVER">Servidor de Autenticação</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={8}>
                <TextField
                  fullWidth
                  label="IP / Hostname"
                  required
                  value={formData.ipAddress}
                  onChange={(e) => setFormData({ ...formData, ipAddress: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  type="number"
                  label="Porta API"
                  value={formData.apiPort}
                  onChange={(e) => setFormData({ ...formData, apiPort: Number(e.target.value) })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="API Token / Senha de Acesso"
                  type="password"
                  value={formData.apiToken}
                  onChange={(e) => setFormData({ ...formData, apiToken: e.target.value })}
                />
              </Grid>
              <Grid item xs={12}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.active}
                      onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                      color="primary"
                    />
                  }
                  label="Equipamento Ativo"
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenModal(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Cadastrar
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};

export default NetworkDeviceList;
