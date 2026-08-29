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
  Grid,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Alert,
} from '@mui/material';
import {
  Router as RouterIcon,
  Sensors as SignalIcon,
  Lock as BlockIcon,
  LockOpen as UnblockIcon,
  Autorenew as SyncIcon,
  CheckCircle as ActiveIcon,
  NetworkCheck as DiagnoseIcon,
} from '@mui/icons-material';
import networkService from '../../services/networkService';

const getSignalColor = (dbm) => {
  if (!dbm) return 'default';
  const val = Number(dbm);
  if (val >= -23.0) return 'success'; // Excelente
  if (val >= -27.0) return 'warning'; // Atenção
  return 'error'; // Crítico
};

const OnuList = () => {
  const [onus, setOnus] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal Diagnóstico
  const [diagModalOpen, setDiagModalOpen] = useState(false);
  const [diagResult, setDiagResult] = useState(null);
  const [diagLoading, setDiagLoading] = useState(false);

  const loadOnus = async () => {
    try {
      setLoading(true);
      const res = await networkService.getAllOnus();
      setOnus(res.data || []);
      setError(null);
    } catch (err) {
      setError('Erro ao carregar provisionamentos de ONU.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOnus();
  }, []);

  const handleDiagnose = async (onu) => {
    try {
      setDiagLoading(true);
      setDiagModalOpen(true);
      const res = await networkService.diagnoseOnu(onu.id);
      setDiagResult(res.data);
      loadOnus();
    } catch (err) {
      alert('Erro ao realizar diagnóstico na OLT.');
      setDiagModalOpen(false);
    } finally {
      setDiagLoading(false);
    }
  };

  const handleToggleBlock = async (onu) => {
    const isBlocked = onu.status === 'BLOCKED';
    const msg = isBlocked
      ? 'Deseja desbloquear o acesso de internet desta ONU?'
      : 'Deseja bloquear o tráfego desta ONU por inadimplência/manutenção?';

    if (window.confirm(msg)) {
      try {
        if (isBlocked) {
          await networkService.unblockOnu(onu.contractId);
        } else {
          await networkService.blockOnu(onu.contractId, 'Bloqueio Administrativo');
        }
        loadOnus();
      } catch (err) {
        alert('Erro ao alterar status da ONU.');
      }
    }
  };

  const totalProvisioned = onus.filter((o) => o.status === 'PROVISIONED').length;
  const totalBlocked = onus.filter((o) => o.status === 'BLOCKED').length;

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <div>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            NOC - Provisionamento de ONUs & Fibra
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Monitoramento de potência óptica (dBm), autorização em OLTs e controle de acesso.
          </Typography>
        </div>

        <Button
          variant="outlined"
          startIcon={<SyncIcon />}
          onClick={loadOnus}
        >
          Atualizar Lista
        </Button>
      </Box>

      {/* Cards de Métricas NOC */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card sx={{ borderRadius: 2, bgcolor: '#e8f5e9' }}>
            <CardContent>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                ONUS ATIVAS / PROVISIONADAS
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="success.main" sx={{ mt: 1 }}>
                {totalProvisioned} modems online
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card sx={{ borderRadius: 2, bgcolor: '#ffebee' }}>
            <CardContent>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                ONUS BLOQUEADAS (INADIMPLÊNCIA)
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="error.main" sx={{ mt: 1 }}>
                {totalBlocked} bloqueadas
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card sx={{ borderRadius: 2, bgcolor: '#e3f2fd' }}>
            <CardContent>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                TOTAL DE EQUIPAMENTOS EM CAMPO
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="primary.main" sx={{ mt: 1 }}>
                {onus.length} ONUs
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 5 }}>
          <CircularProgress />
        </Box>
      ) : (
        <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 2 }}>
          <Table>
            <TableHead sx={{ backgroundColor: '#f8f9fa' }}>
              <TableRow>
                <TableCell><strong>Equipamento (MAC / Serial)</strong></TableCell>
                <TableCell><strong>Status</strong></TableCell>
                <TableCell><strong>Potência Óptica (RX Power)</strong></TableCell>
                <TableCell><strong>Plano de Velocidade</strong></TableCell>
                <TableCell><strong>PPPoE / Usuário</strong></TableCell>
                <TableCell align="right"><strong>Comandos NOC</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {onus.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                    Nenhuma ONU provisionada na rede até o momento.
                  </TableCell>
                </TableRow>
              ) : (
                onus.map((onu) => (
                  <TableRow key={onu.id} hover>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <RouterIcon color="primary" />
                        <div>
                          <Typography variant="body2" fontWeight="bold">
                            MAC: {onu.onuMac}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            SN: {onu.onuSerial} | VLAN: {onu.vlanId}
                          </Typography>
                        </div>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={onu.status === 'PROVISIONED' ? 'Online / Ativo' : 'Bloqueado'}
                        color={onu.status === 'PROVISIONED' ? 'success' : 'error'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={onu.rxPowerDbm ? `${onu.rxPowerDbm} dBm` : 'Sem leitura'}
                        color={getSignalColor(onu.rxPowerDbm)}
                        size="small"
                        icon={<SignalIcon fontSize="small" />}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight="500">
                        {onu.downloadSpeed}M / {onu.uploadSpeed}M
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {onu.pppoeUser || 'Não configurado'}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                        <Button
                          size="small"
                          variant="outlined"
                          color="info"
                          startIcon={<DiagnoseIcon />}
                          onClick={() => handleDiagnose(onu)}
                        >
                          Diagnosticar
                        </Button>
                        <Button
                          size="small"
                          variant="contained"
                          color={onu.status === 'PROVISIONED' ? 'warning' : 'success'}
                          startIcon={onu.status === 'PROVISIONED' ? <BlockIcon /> : <UnblockIcon />}
                          onClick={() => handleToggleBlock(onu)}
                        >
                          {onu.status === 'PROVISIONED' ? 'Bloquear' : 'Desbloquear'}
                        </Button>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Modal de Diagnóstico em Tempo Real */}
      <Dialog open={diagModalOpen} onClose={() => setDiagModalOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Diagnóstico de Sinal na OLT</DialogTitle>
        <DialogContent dividers sx={{ textAlign: 'center' }}>
          {diagLoading ? (
            <Box sx={{ py: 4 }}>
              <CircularProgress />
              <Typography variant="body2" sx={{ mt: 2 }} color="text.secondary">
                Consultando telemetria da OLT em tempo real...
              </Typography>
            </Box>
          ) : diagResult ? (
            <Box sx={{ py: 2 }}>
              <SignalIcon sx={{ fontSize: 64, color: getSignalColor(diagResult.rxPowerDbm) + '.main', mb: 1 }} />
              <Typography variant="h5" fontWeight="bold">
                {diagResult.rxPowerDbm} dBm
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                {diagResult.details}
              </Typography>
              <Typography variant="caption" display="block" color="text.secondary" sx={{ mt: 2 }}>
                OLT: {diagResult.oltName} | Status: {diagResult.status}
              </Typography>
            </Box>
          ) : (
            <Typography>Nenhum dado retornado.</Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDiagModalOpen(false)}>Fechar</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default OnuList;
