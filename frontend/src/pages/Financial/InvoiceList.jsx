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
  Divider,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Alert,
} from '@mui/material';
import {
  QrCode2 as QrCodeIcon,
  CheckCircle as PaidIcon,
  HourglassEmpty as PendingIcon,
  ErrorOutline as OverdueIcon,
  Cancel as CancelIcon,
  ContentCopy as CopyIcon,
  Autorenew as SyncIcon,
  AttachMoney as MoneyIcon,
} from '@mui/icons-material';
import invoiceService from '../../services/invoiceService';

const statusConfig = {
  PENDING: { label: 'Aguardando Pagamento', color: 'warning', icon: <PendingIcon fontSize="small" /> },
  PAID: { label: 'Pago', color: 'success', icon: <PaidIcon fontSize="small" /> },
  OVERDUE: { label: 'Vencido', color: 'error', icon: <OverdueIcon fontSize="small" /> },
  CANCELED: { label: 'Cancelado', color: 'default', icon: <CancelIcon fontSize="small" /> },
};

const InvoiceList = () => {
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filterStatus, setFilterStatus] = useState('ALL');

  // Modal Pix
  const [pixModalOpen, setPixModalOpen] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [copied, setCopied] = useState(false);

  const loadInvoices = async () => {
    try {
      setLoading(true);
      const res = await invoiceService.getAllInvoices();
      setInvoices(res.data || []);
      setError(null);
    } catch (err) {
      setError('Erro ao carregar faturas de cobrança.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInvoices();
  }, []);

  const handleOpenPix = (invoice) => {
    setSelectedInvoice(invoice);
    setCopied(false);
    setPixModalOpen(true);
  };

  const handleCopyPix = () => {
    if (selectedInvoice?.pixCopiaECola) {
      navigator.clipboard.writeText(selectedInvoice.pixCopiaECola);
      setCopied(true);
      setTimeout(() => setCopied(false), 3000);
    }
  };

  const handleMarkAsPaid = async (id) => {
    if (window.confirm('Deseja confirmar o recebimento desta fatura manualmente?')) {
      try {
        await invoiceService.markAsPaid(id);
        loadInvoices();
      } catch (err) {
        alert('Erro ao dar baixa na fatura.');
      }
    }
  };

  const handleTriggerRecurring = async () => {
    try {
      setLoading(true);
      const res = await invoiceService.triggerRecurringBilling();
      alert(res.data || 'Rotina de faturamento recorrente executada com sucesso!');
      loadInvoices();
    } catch (err) {
      alert('Erro ao executar faturamento.');
    } finally {
      setLoading(false);
    }
  };

  const filteredInvoices = filterStatus === 'ALL'
    ? invoices
    : invoices.filter((i) => i.status === filterStatus);

  const totalPending = invoices
    .filter((i) => i.status === 'PENDING')
    .reduce((acc, curr) => acc + Number(curr.amount || 0), 0);

  const totalPaid = invoices
    .filter((i) => i.status === 'PAID')
    .reduce((acc, curr) => acc + Number(curr.paidAmount || curr.amount || 0), 0);

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <div>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Gestão Financeira & Cobrança
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Emissão de faturas, Pix COB/COBV via Xingubit Pay e conciliação bancária automática.
          </Typography>
        </div>

        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="outlined"
            color="primary"
            startIcon={<SyncIcon />}
            onClick={handleTriggerRecurring}
          >
            Faturamento Recorrente
          </Button>

          <FormControl sx={{ minWidth: 200 }} size="small">
            <InputLabel id="filter-status-label">Filtrar por Status</InputLabel>
            <Select
              labelId="filter-status-label"
              value={filterStatus}
              label="Filtrar por Status"
              onChange={(e) => setFilterStatus(e.target.value)}
            >
              <MenuItem value="ALL">Todas as Faturas</MenuItem>
              <MenuItem value="PENDING">Aguardando Pagamento</MenuItem>
              <MenuItem value="PAID">Pagas</MenuItem>
              <MenuItem value="OVERDUE">Vencidas</MenuItem>
              <MenuItem value="CANCELED">Canceladas</MenuItem>
            </Select>
          </FormControl>
        </Box>
      </Box>

      {/* Cards de Métricas Financeiras */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card sx={{ borderRadius: 2, bgcolor: '#fff8e1' }}>
            <CardContent>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                TOTAL A RECEBER (PENDENTE)
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="warning.main" sx={{ mt: 1 }}>
                R$ {totalPending.toFixed(2).replace('.', ',')}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card sx={{ borderRadius: 2, bgcolor: '#e8f5e9' }}>
            <CardContent>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                TOTAL RECEBIDO (LIQUIDADO)
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="success.main" sx={{ mt: 1 }}>
                R$ {totalPaid.toFixed(2).replace('.', ',')}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card sx={{ borderRadius: 2, bgcolor: '#e3f2fd' }}>
            <CardContent>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                TOTAL DE FATURAS EMITIDAS
              </Typography>
              <Typography variant="h5" fontWeight="bold" color="primary.main" sx={{ mt: 1 }}>
                {invoices.length} cobranças
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
                <TableCell><strong>Identificador / Gateway</strong></TableCell>
                <TableCell><strong>Status</strong></TableCell>
                <TableCell><strong>Vencimento</strong></TableCell>
                <TableCell><strong>Valor</strong></TableCell>
                <TableCell><strong>Pagamento</strong></TableCell>
                <TableCell align="right"><strong>Ações</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredInvoices.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                    Nenhuma fatura encontrada.
                  </TableCell>
                </TableRow>
              ) : (
                filteredInvoices.map((inv) => {
                  const conf = statusConfig[inv.status] || { label: inv.status, color: 'default' };
                  return (
                    <TableRow key={inv.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <MoneyIcon color="primary" />
                          <div>
                            <Typography variant="body2" fontWeight="bold">
                              {inv.externalTransactionId || inv.id?.substring(0, 8)}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              Gateway: {inv.gatewayType}
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
                        {inv.paidByCrossCreditId && (
                          <Chip label="Compensação" size="small" color="primary" variant="outlined" sx={{ ml: 0.5 }} />
                        )}
                        {inv.protectedAgainstSuspension && (
                          <Chip label="Protegido" size="small" color="info" sx={{ ml: 0.5 }} />
                        )}
                        {inv.rebalanceNotice && (
                          <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5, fontStyle: 'italic' }}>
                            ℹ️ {inv.rebalanceNotice}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight="500">
                          {inv.dueDate}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight="600" color="primary">
                          R$ {Number(inv.amount).toFixed(2).replace('.', ',')}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {inv.paidAt ? (
                          <>
                            <Typography variant="caption" color="success.main" display="block" fontWeight="bold">
                              Pago em {inv.paidAt.substring(0, 10)}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              Via {inv.paymentMethod}
                            </Typography>
                          </>
                        ) : (
                          <Typography variant="caption" color="text.secondary">
                            Aguardando compensação
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell align="right">
                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                          {inv.pixCopiaECola && (
                            <Button
                              size="small"
                              variant="outlined"
                              color="primary"
                              startIcon={<QrCodeIcon />}
                              onClick={() => handleOpenPix(inv)}
                            >
                              Pix QR
                            </Button>
                          )}
                          {inv.status === 'PENDING' && (
                            <Button
                              size="small"
                              variant="contained"
                              color="success"
                              onClick={() => handleMarkAsPaid(inv.id)}
                            >
                              Baixa Manual
                            </Button>
                          )}
                        </Box>
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Modal Pix Copia e Cola / QR Code */}
      <Dialog open={pixModalOpen} onClose={() => setPixModalOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Pagamento via Pix (Xingubit Pay)</DialogTitle>
        <DialogContent dividers sx={{ textAlign: 'center' }}>
          {selectedInvoice && (
            <Box sx={{ py: 2 }}>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Valor: R$ {Number(selectedInvoice.amount).toFixed(2).replace('.', ',')}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Vencimento: {selectedInvoice.dueDate} | TxID: {selectedInvoice.externalTransactionId}
              </Typography>

              <Box sx={{ my: 3, p: 2, bgcolor: '#f5f5f5', borderRadius: 2 }}>
                <QrCodeIcon sx={{ fontSize: 120, color: 'primary.main' }} />
                <Typography variant="caption" color="text.secondary" display="block">
                  Aponte a câmera do aplicativo do seu banco para o QR Code Pix acima
                </Typography>
              </Box>

              <TextField
                fullWidth
                multiline
                rows={3}
                label="Código Pix Copia e Cola"
                value={selectedInvoice.pixCopiaECola || ''}
                InputProps={{ readOnly: true }}
                sx={{ mt: 1 }}
              />

              {copied && <Alert severity="success" sx={{ mt: 2 }}>Código Pix copiado para a área de transferência!</Alert>}
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setPixModalOpen(false)}>Fechar</Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<CopyIcon />}
            onClick={handleCopyPix}
          >
            Copiar Código Pix
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default InvoiceList;
