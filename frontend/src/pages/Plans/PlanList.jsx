import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  TextField,
  Typography,
  Switch,
  FormControlLabel,
  CircularProgress,
  Alert,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Speed as SpeedIcon,
  AttachMoney as MoneyIcon,
} from '@mui/icons-material';
import planService from '../../services/planService';

const PlanList = () => {
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingPlan, setEditingPlan] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    downloadSpeed: '',
    uploadSpeed: '',
    price: '',
    description: '',
    svaIncluded: '',
    active: true,
  });

  const loadPlans = async () => {
    try {
      setLoading(true);
      const res = await planService.getAllPlans();
      setPlans(res.data || []);
      setError(null);
    } catch (err) {
      setError('Erro ao carregar planos de internet.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPlans();
  }, []);

  const handleOpenDialog = (plan = null) => {
    if (plan) {
      setEditingPlan(plan);
      setFormData({
        name: plan.name,
        downloadSpeed: plan.downloadSpeed,
        uploadSpeed: plan.uploadSpeed,
        price: plan.price,
        description: plan.description || '',
        svaIncluded: plan.svaIncluded || '',
        active: plan.active,
      });
    } else {
      setEditingPlan(null);
      setFormData({
        name: '',
        downloadSpeed: '',
        uploadSpeed: '',
        price: '',
        description: '',
        svaIncluded: '',
        active: true,
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingPlan(null);
  };

  const handleSavePlan = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...formData,
        downloadSpeed: Number(formData.downloadSpeed),
        uploadSpeed: Number(formData.uploadSpeed),
        price: Number(formData.price),
      };

      if (editingPlan) {
        await planService.updatePlan(editingPlan.id, payload);
      } else {
        await planService.createPlan(payload);
      }
      handleCloseDialog();
      loadPlans();
    } catch (err) {
      alert('Erro ao salvar plano: ' + (err.response?.data || err.message));
    }
  };

  const handleDeletePlan = async (id) => {
    if (window.confirm('Tem certeza que deseja excluir este plano?')) {
      try {
        await planService.deletePlan(id);
        loadPlans();
      } catch (err) {
        alert('Erro ao excluir plano.');
      }
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <div>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Planos de Internet
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Gerencie o catálogo de planos, velocidades e serviços de valor agregado (SVA).
          </Typography>
        </div>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Novo Plano
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 5 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Grid container spacing={3}>
          {plans.map((plan) => (
            <Grid item xs={12} sm={6} md={4} key={plan.id}>
              <Card
                sx={{
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  borderRadius: 2,
                  boxShadow: 2,
                  border: plan.active ? '1px solid #e0e0e0' : '1px dashed #bdbdbd',
                  opacity: plan.active ? 1 : 0.7,
                }}
              >
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                    <Typography variant="h6" fontWeight="bold">
                      {plan.name}
                    </Typography>
                    <Chip
                      label={plan.active ? 'Ativo' : 'Inativo'}
                      color={plan.active ? 'success' : 'default'}
                      size="small"
                    />
                  </Box>

                  <Box sx={{ display: 'flex', alignItems: 'baseline', my: 2 }}>
                    <Typography variant="h4" color="primary" fontWeight="bold">
                      R$ {Number(plan.price).toFixed(2).replace('.', ',')}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                      / mês
                    </Typography>
                  </Box>

                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, my: 1 }}>
                    <SpeedIcon color="action" fontSize="small" />
                    <Typography variant="body2">
                      <strong>{plan.downloadSpeed} Mbps</strong> download / <strong>{plan.uploadSpeed} Mbps</strong> upload
                    </Typography>
                  </Box>

                  {plan.svaIncluded && (
                    <Box sx={{ mt: 1.5 }}>
                      <Typography variant="caption" color="text.secondary" display="block">
                        SVA Incluído:
                      </Typography>
                      <Chip label={plan.svaIncluded} size="small" variant="outlined" color="primary" />
                    </Box>
                  )}

                  {plan.description && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1.5 }}>
                      {plan.description}
                    </Typography>
                  )}
                </CardContent>

                <Box sx={{ display: 'flex', justifyContent: 'flex-end', p: 1.5, borderTop: '1px solid #f0f0f0' }}>
                  <IconButton size="small" color="primary" onClick={() => handleOpenDialog(plan)}>
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton size="small" color="error" onClick={() => handleDeletePlan(plan.id)}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Box>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Dialog de Criação / Edição */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <form onSubmit={handleSavePlan}>
          <DialogTitle>{editingPlan ? 'Editar Plano' : 'Novo Plano de Internet'}</DialogTitle>
          <DialogContent dividers>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Nome do Plano"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Ex: Fibra 500 Mega Gamer"
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Download (Mbps)"
                  required
                  value={formData.downloadSpeed}
                  onChange={(e) => setFormData({ ...formData, downloadSpeed: e.target.value })}
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Upload (Mbps)"
                  required
                  value={formData.uploadSpeed}
                  onChange={(e) => setFormData({ ...formData, uploadSpeed: e.target.value })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  type="number"
                  inputProps={{ step: '0.01' }}
                  label="Mensalidade (R$)"
                  required
                  value={formData.price}
                  onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="SVA / Benefícios Inclusos"
                  value={formData.svaIncluded}
                  onChange={(e) => setFormData({ ...formData, svaIncluded: e.target.value })}
                  placeholder="Ex: Paramount+, Noggin, Wi-Fi 6"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  label="Descrição Comercial"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
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
                  label="Plano Ativo para Vendas"
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseDialog}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Salvar Plano
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};

export default PlanList;
