import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Alert,
  Paper,
  Button,
} from '@mui/material';
import {
  Description as ContractIcon,
  CheckCircle as ActiveIcon,
  Schedule as PendingIcon,
  Block as SuspendedIcon,
} from '@mui/icons-material';
import contractService from '../../services/contractService';

const statusConfig = {
  PENDING_INSTALLATION: { label: 'Pendente de Instalação', color: 'warning', icon: <PendingIcon fontSize="small" /> },
  ACTIVE: { label: 'Ativo', color: 'success', icon: <ActiveIcon fontSize="small" /> },
  SUSPENDED: { label: 'Suspenso', color: 'error', icon: <SuspendedIcon fontSize="small" /> },
  CANCELED: { label: 'Cancelado', color: 'default', icon: null },
};

const ContractList = () => {
  const [contracts, setContracts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filterStatus, setFilterStatus] = useState('ALL');

  const loadContracts = async () => {
    try {
      setLoading(true);
      const res = await contractService.getAllContracts();
      setContracts(res.data || []);
      setError(null);
    } catch (err) {
      setError('Erro ao carregar contratos.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadContracts();
  }, []);

  const handleUpdateStatus = async (id, newStatus) => {
    try {
      await contractService.updateStatus(id, newStatus);
      loadContracts();
    } catch (err) {
      alert('Erro ao atualizar status do contrato.');
    }
  };

  const filteredContracts = filterStatus === 'ALL'
    ? contracts
    : contracts.filter((c) => c.status === filterStatus);

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <div>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Contratos de Clientes
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Acompanhe a ativação, endereços de instalação e ciclo de faturamento dos clientes.
          </Typography>
        </div>

        <FormControl sx={{ minWidth: 220 }} size="small">
          <InputLabel id="filter-status-label">Filtrar por Status</InputLabel>
          <Select
            labelId="filter-status-label"
            value={filterStatus}
            label="Filtrar por Status"
            onChange={(e) => setFilterStatus(e.target.value)}
          >
            <MenuItem value="ALL">Todos os Status</MenuItem>
            <MenuItem value="PENDING_INSTALLATION">Pendentes de Instalação</MenuItem>
            <MenuItem value="ACTIVE">Ativos</MenuItem>
            <MenuItem value="SUSPENDED">Suspensos</MenuItem>
            <MenuItem value="CANCELED">Cancelados</MenuItem>
          </Select>
        </FormControl>
      </Box>

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
                <TableCell><strong>Contrato</strong></TableCell>
                <TableCell><strong>Status</strong></TableCell>
                <TableCell><strong>Endereço de Instalação</strong></TableCell>
                <TableCell><strong>Mensalidade</strong></TableCell>
                <TableCell><strong>Vencimento</strong></TableCell>
                <TableCell align="right"><strong>Ações</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredContracts.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                    Nenhum contrato encontrado.
                  </TableCell>
                </TableRow>
              ) : (
                filteredContracts.map((c) => {
                  const conf = statusConfig[c.status] || { label: c.status, color: 'default' };
                  return (
                    <TableRow key={c.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <ContractIcon color="action" />
                          <div>
                            <Typography variant="body2" fontWeight="bold">
                              {c.contractNumber}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              ID Cliente: {c.customerId?.substring(0, 8)}...
                            </Typography>
                          </div>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={conf.label}
                          color={conf.color}
                          size="small"
                          icon={conf.icon}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{c.installationAddress}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {c.city} - {c.state} ({c.zipCode})
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight="600" color="primary">
                          R$ {Number(c.monthlyFee).toFixed(2).replace('.', ',')}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        Dia {c.dueDay}
                      </TableCell>
                      <TableCell align="right">
                        {c.status === 'PENDING_INSTALLATION' && (
                          <Button
                            size="small"
                            variant="outlined"
                            color="success"
                            onClick={() => handleUpdateStatus(c.id, 'ACTIVE')}
                          >
                            Ativar
                          </Button>
                        )}
                        {c.status === 'ACTIVE' && (
                          <Button
                            size="small"
                            variant="outlined"
                            color="warning"
                            onClick={() => handleUpdateStatus(c.id, 'SUSPENDED')}
                          >
                            Suspender
                          </Button>
                        )}
                        {c.status === 'SUSPENDED' && (
                          <Button
                            size="small"
                            variant="outlined"
                            color="success"
                            onClick={() => handleUpdateStatus(c.id, 'ACTIVE')}
                          >
                            Reativar
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
};

export default ContractList;
