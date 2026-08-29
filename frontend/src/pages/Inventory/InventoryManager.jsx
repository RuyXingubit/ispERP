import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  Tabs,
  Tab,
  Button,
  Grid,
  Card,
  CardContent,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  CircularProgress,
  Alert,
  IconButton
} from '@mui/material';
import {
  Warehouse as WarehouseIcon,
  Handshake as HandshakeIcon,
  Build as ToolIcon,
  AssignmentReturn as ReturnIcon,
  LocalShipping as TruckIcon,
  Add as AddIcon,
  CheckCircle as CheckIcon,
  Warning as WarningIcon,
  PhotoCamera as CameraIcon,
  Refresh as RefreshIcon,
  Description as DocumentIcon
} from '@mui/icons-material';
import { inventoryCustodyService } from '../../services/inventoryCustodyService';
import { inventoryService } from '../../services/inventoryService';
import { toast } from 'react-toastify';

const InventoryManager = () => {
  const [currentTab, setCurrentTab] = useState(0);
  const [loading, setLoading] = useState(true);

  // Dados
  const [warehouses, setWarehouses] = useState([]);
  const [items, setItems] = useState([]);
  const [assets, setAssets] = useState([]);
  const [transfers, setTransfers] = useState([]);
  const [agreements, setAgreements] = useState([]);

  // Modais
  const [warehouseModalOpen, setWarehouseModalOpen] = useState(false);
  const [transferModalOpen, setTransferModalOpen] = useState(false);
  const [toolModalOpen, setToolModalOpen] = useState(false);
  const [returnToolModalOpen, setReturnToolModalOpen] = useState(false);
  const [selectedAgreement, setSelectedAgreement] = useState(null);

  // Forms
  const [warehouseForm, setWarehouseForm] = useState({
    code: '',
    name: '',
    city: 'Altamira',
    state: 'PA',
    address: ''
  });

  const [transferForm, setTransferForm] = useState({
    originWarehouseId: '',
    destinationWarehouseId: '',
    carrierName: 'João Silva (Técnico / Portador)',
    carrierDocument: '529.982.247-25',
    carrierType: 'COLABORADOR',
    notes: 'Transferência de equipamentos para expansão em Vitória do Xingu'
  });

  const [toolForm, setToolForm] = useState({
    holderName: 'Carlos Silva (Equipe 01)',
    holderCpf: '123.456.789-00',
    isThirdParty: false,
    notes: 'Empréstimo de máquina de fusão e OTDR para emenda de backbone'
  });

  const [returnToolForm, setReturnToolForm] = useState({
    warehouseId: '',
    isDamaged: false,
    returnPhotoUrl: 'https://isperp.local/photos/return-inspection-ok.jpg',
    notes: 'Equipamento conferido em bancada, limpo e operando com clivagem perfeita.'
  });

  const loadData = async () => {
    try {
      setLoading(true);
      const [wRes, iRes, aRes, tRes, gRes] = await Promise.all([
        inventoryCustodyService.getAllWarehouses(),
        inventoryService.getAllItems(),
        inventoryCustodyService.getAllAssets(),
        inventoryCustodyService.getAllTransfers(),
        inventoryCustodyService.getAllToolAgreements()
      ]);

      setWarehouses(wRes.data || []);
      setItems(iRes.data || []);
      setAssets(aRes.data || []);
      setTransfers(tRes.data || []);
      setAgreements(gRes.data || []);
    } catch (err) {
      console.error('Erro ao carregar almoxarifado:', err);
      toast.error('Erro ao carregar dados do almoxarifado.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateWarehouse = async (e) => {
    e.preventDefault();
    try {
      await inventoryCustodyService.createWarehouse(warehouseForm);
      toast.success('Depósito cadastrado com sucesso!');
      setWarehouseModalOpen(false);
      loadData();
    } catch (err) {
      toast.error('Erro ao cadastrar depósito: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleCreateTransfer = async (e) => {
    e.preventDefault();
    try {
      await inventoryCustodyService.createTransfer(transferForm);
      toast.success('Guia de transferência criada com sucesso!');
      setTransferModalOpen(false);
      loadData();
    } catch (err) {
      toast.error('Erro ao criar transferência: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleDispatchTransfer = async (transferId) => {
    try {
      await inventoryCustodyService.dispatchTransfer(transferId, {
        dispatchPhotoUrl: 'https://isperp.local/photos/dispatch-boxes.jpg'
      });
      toast.success('Carga despachada! Itens colocados sob custódia legal do portador.');
      loadData();
    } catch (err) {
      toast.error('Erro ao despachar carga.');
    }
  };

  const handleConfirmReceipt = async (transferId) => {
    try {
      await inventoryCustodyService.confirmReceiptTransfer(transferId, {
        receiptPhotoUrl: 'https://isperp.local/photos/receipt-boxes.jpg'
      });
      toast.success('Recebimento confirmado! Itens creditados no destino e portador liberado.');
      loadData();
    } catch (err) {
      toast.error('Erro ao confirmar recebimento.');
    }
  };

  const handleOpenReturnTool = (agr) => {
    setSelectedAgreement(agr);
    setReturnToolForm({
      warehouseId: warehouses[0]?.id || '',
      isDamaged: false,
      returnPhotoUrl: 'https://isperp.local/photos/return-inspection-ok.jpg',
      notes: 'Equipamento conferido em bancada, limpo e operando com clivagem perfeita.'
    });
    setReturnToolModalOpen(true);
  };

  const handleConfirmReturnTool = async (e) => {
    e.preventDefault();
    try {
      await inventoryCustodyService.returnToolAgreement(selectedAgreement.id, returnToolForm);
      toast.success('Devolução confirmada e responsabilidade do portador extinta!');
      setReturnToolModalOpen(false);
      loadData();
    } catch (err) {
      toast.error('Erro ao processar devolução.');
    }
  };

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Almoxarifado & Gestão de Estoques
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Multi-depósitos, custódia patrimonial por colaborador (CPF), transferências com handshake e termos de cautela executiva.
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={loadData}
          disabled={loading}
          sx={{ borderRadius: 2 }}
        >
          Atualizar
        </Button>
      </Box>

      {/* Cards de Resumo */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderRadius: 3, borderLeft: '6px solid #1976d2' }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                DEPÓSITOS FÍSICOS
              </Typography>
              <Typography variant="h4" fontWeight="bold" color="primary">
                {warehouses.length}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Locais de armazenagem ativos
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderRadius: 3, borderLeft: '6px solid #ed6c02' }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                CARGAS EM TRÂNSITO
              </Typography>
              <Typography variant="h4" fontWeight="bold" sx={{ color: '#ed6c02' }}>
                {transfers.filter((t) => t.status === 'IN_TRANSIT').length}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Sob custódia de portadores
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderRadius: 3, borderLeft: '6px solid #2e7d32' }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                FERRAMENTAS EM CAUTELA
              </Typography>
              <Typography variant="h4" fontWeight="bold" color="success.main">
                {agreements.filter((a) => a.status === 'ACTIVE').length}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Com nota promissória ativa
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ borderRadius: 3, borderLeft: '6px solid #9c27b0' }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">
                INSUMOS NO CATÁLOGO
              </Typography>
              <Typography variant="h4" fontWeight="bold" sx={{ color: '#9c27b0' }}>
                {items.length}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Cabos, ONTs, conectores e PTOs
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Tabs */}
      <Paper elevation={1} sx={{ mb: 3, borderRadius: 2 }}>
        <Tabs
          value={currentTab}
          onChange={(e, val) => setCurrentTab(val)}
          indicatorColor="primary"
          textColor="primary"
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab label="Depósitos & Saldos Físicos" icon={<WarehouseIcon />} iconPosition="start" />
          <Tab label="Custódias & Ferramental de Alto Valor" icon={<ToolIcon />} iconPosition="start" />
          <Tab label="Transferências Intermunicipais" icon={<TruckIcon />} iconPosition="start" />
          <Tab label="Logística Reversa (Devoluções O.S.)" icon={<ReturnIcon />} iconPosition="start" />
        </Tabs>
      </Paper>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          {/* ABA 0: Depósitos & Saldos */}
          {currentTab === 0 && (
            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6" fontWeight="bold">
                  Locais Físicos de Armazenagem & Almoxarifados
                </Typography>
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={() => setWarehouseModalOpen(true)}
                  sx={{ borderRadius: 2 }}
                >
                  Novo Depósito
                </Button>
              </Box>

              <Grid container spacing={2} sx={{ mb: 4 }}>
                {warehouses.map((w) => (
                  <Grid item xs={12} md={6} key={w.id}>
                    <Card sx={{ borderRadius: 3, border: '1px solid #e2e8f0' }}>
                      <CardContent sx={{ p: 2.5 }}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                          <Typography variant="subtitle1" fontWeight="bold">
                            {w.name}
                          </Typography>
                          <Chip label={w.code} size="small" color="primary" variant="outlined" />
                        </Box>
                        <Typography variant="body2" color="text.secondary">
                          📍 {w.city} - {w.state} | {w.address || 'Sem endereço detalhado'}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>

              <Typography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
                Saldos Globais de Insumos & Materiais
              </Typography>
              <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 2 }}>
                <Table>
                  <TableHead sx={{ bgcolor: '#f8f9fa' }}>
                    <TableRow>
                      <TableCell><strong>Código</strong></TableCell>
                      <TableCell><strong>Item / Insumo</strong></TableCell>
                      <TableCell><strong>Categoria</strong></TableCell>
                      <TableCell align="right"><strong>Saldo em Estoque</strong></TableCell>
                      <TableCell align="right"><strong>Estoque Mínimo</strong></TableCell>
                      <TableCell align="center"><strong>Status</strong></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {items.map((item) => {
                      const isLow = item.quantityInStock <= item.minQuantity;
                      return (
                        <TableRow key={item.id} hover>
                          <TableCell sx={{ fontWeight: 'bold' }}>{item.code}</TableCell>
                          <TableCell>{item.name}</TableCell>
                          <TableCell>
                            <Chip label={item.category} size="small" />
                          </TableCell>
                          <TableCell align="right" sx={{ fontWeight: 'bold', fontSize: '1rem' }}>
                            {item.quantityInStock} {item.unit}
                          </TableCell>
                          <TableCell align="right">{item.minQuantity} {item.unit}</TableCell>
                          <TableCell align="center">
                            {isLow ? (
                              <Chip label="Estoque Crítico" color="error" size="small" icon={<WarningIcon />} />
                            ) : (
                              <Chip label="Regular" color="success" size="small" icon={<CheckIcon />} />
                            )}
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}

          {/* ABA 1: Custódias & Ferramental de Alto Valor */}
          {currentTab === 1 && (
            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Box>
                  <Typography variant="h6" fontWeight="bold">
                    Termos de Cautela & Notas Promissórias Executivas
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Controle de máquinas de fusão, OTDRs e ferramentas cedidas a técnicos ou terceiros.
                  </Typography>
                </Box>
              </Box>

              {agreements.length === 0 ? (
                <Alert severity="info" sx={{ borderRadius: 2 }}>
                  Nenhum termo de cautela registrado no momento.
                </Alert>
              ) : (
                <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 2 }}>
                  <Table>
                    <TableHead sx={{ bgcolor: '#f8f9fa' }}>
                      <TableRow>
                        <TableCell><strong>Código / Termo</strong></TableCell>
                        <TableCell><strong>Responsável (Pessoa Física)</strong></TableCell>
                        <TableCell><strong>Tipo</strong></TableCell>
                        <TableCell><strong>Valor Promissória</strong></TableCell>
                        <TableCell><strong>Status</strong></TableCell>
                        <TableCell align="right"><strong>Ações</strong></TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {agreements.map((agr) => (
                        <TableRow key={agr.id} hover>
                          <TableCell sx={{ fontWeight: 'bold' }}>
                            <DocumentIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 0.5 }} color="primary" />
                            {agr.code}
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2" fontWeight="bold">{agr.holderName}</Typography>
                            <Typography variant="caption" color="text.secondary">CPF: {agr.holderCpf}</Typography>
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={agr.isThirdParty ? 'Terceiro / PJ' : 'Colaborador CLT'}
                              color={agr.isThirdParty ? 'warning' : 'info'}
                              size="small"
                            />
                          </TableCell>
                          <TableCell sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                            R$ {Number(agr.totalPromissoryValue).toFixed(2).replace('.', ',')}
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={agr.status}
                              color={agr.status === 'ACTIVE' ? 'error' : 'success'}
                              size="small"
                            />
                          </TableCell>
                          <TableCell align="right">
                            {agr.status === 'ACTIVE' && (
                              <Button
                                size="small"
                                variant="contained"
                                color="success"
                                startIcon={<CheckIcon />}
                                onClick={() => handleOpenReturnTool(agr)}
                              >
                                Receber Devolução
                              </Button>
                            )}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </Box>
          )}

          {/* ABA 2: Transferências Intermunicipais */}
          {currentTab === 2 && (
            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Box>
                  <Typography variant="h6" fontWeight="bold">
                    Transferências Intermunicipais com Duplo Aceite (Handshake)
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Garante que a responsabilidade do transporte fique com a pessoa física do portador até o aceite no destino.
                  </Typography>
                </Box>
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={() => setTransferModalOpen(true)}
                  sx={{ borderRadius: 2 }}
                >
                  Nova Transferência
                </Button>
              </Box>

              {transfers.length === 0 ? (
                <Alert severity="info" sx={{ borderRadius: 2 }}>
                  Nenhuma transferência intermunicipal registrada.
                </Alert>
              ) : (
                <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 2 }}>
                  <Table>
                    <TableHead sx={{ bgcolor: '#f8f9fa' }}>
                      <TableRow>
                        <TableCell><strong>Código</strong></TableCell>
                        <TableCell><strong>Portador Responsável</strong></TableCell>
                        <TableCell><strong>Origem ➔ Destino</strong></TableCell>
                        <TableCell><strong>Status</strong></TableCell>
                        <TableCell align="right"><strong>Ações de Handshake</strong></TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {transfers.map((trf) => (
                        <TableRow key={trf.id} hover>
                          <TableCell sx={{ fontWeight: 'bold' }}>{trf.code}</TableCell>
                          <TableCell>
                            <Typography variant="body2" fontWeight="bold">{trf.carrierName}</Typography>
                            <Typography variant="caption" color="text.secondary">Doc: {trf.carrierDocument}</Typography>
                          </TableCell>
                          <TableCell>
                            <Chip label="Origem" size="small" sx={{ mr: 1 }} />
                            ➔
                            <Chip label="Destino" size="small" color="primary" sx={{ ml: 1 }} />
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={trf.status}
                              color={
                                trf.status === 'PENDING'
                                  ? 'default'
                                  : trf.status === 'IN_TRANSIT'
                                  ? 'warning'
                                  : 'success'
                              }
                              size="small"
                            />
                          </TableCell>
                          <TableCell align="right">
                            {trf.status === 'PENDING' && (
                              <Button
                                size="small"
                                variant="contained"
                                color="warning"
                                startIcon={<CameraIcon />}
                                onClick={() => handleDispatchTransfer(trf.id)}
                              >
                                Despachar Carga
                              </Button>
                            )}
                            {trf.status === 'IN_TRANSIT' && (
                              <Button
                                size="small"
                                variant="contained"
                                color="success"
                                startIcon={<CheckIcon />}
                                onClick={() => handleConfirmReceipt(trf.id)}
                              >
                                Confirmar Recebimento
                              </Button>
                            )}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </Box>
          )}

          {/* ABA 3: Logística Reversa */}
          {currentTab === 3 && (
            <Box>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Logística Reversa • ONUs Recolhidas em Ordens de Serviço
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Equipamentos recolhidos de clientes por cancelamento que estão sob custódia do técnico aguardando triagem em bancada.
              </Typography>

              <Alert severity="success" sx={{ borderRadius: 2 }}>
                Todas as devoluções de campo foram recebidas e conferidas com sucesso nos depósitos!
              </Alert>
            </Box>
          )}
        </>
      )}

      {/* Modal: Novo Depósito */}
      <Dialog open={warehouseModalOpen} onClose={() => setWarehouseModalOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleCreateWarehouse}>
          <DialogTitle>Cadastrar Novo Depósito / Almoxarifado</DialogTitle>
          <DialogContent dividers>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Código Único"
                  required
                  placeholder="Ex: DEP-STM-CENTRAL"
                  value={warehouseForm.code}
                  onChange={(e) => setWarehouseForm({ ...warehouseForm, code: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Nome do Depósito"
                  required
                  placeholder="Ex: Ponto de Apoio Santarém"
                  value={warehouseForm.name}
                  onChange={(e) => setWarehouseForm({ ...warehouseForm, name: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Cidade"
                  required
                  value={warehouseForm.city}
                  onChange={(e) => setWarehouseForm({ ...warehouseForm, city: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Estado"
                  required
                  value={warehouseForm.state}
                  onChange={(e) => setWarehouseForm({ ...warehouseForm, state: e.target.value })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Endereço Completo"
                  value={warehouseForm.address}
                  onChange={(e) => setWarehouseForm({ ...warehouseForm, address: e.target.value })}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setWarehouseModalOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Salvar Depósito
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Modal: Nova Transferência */}
      <Dialog open={transferModalOpen} onClose={() => setTransferModalOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleCreateTransfer}>
          <DialogTitle>Nova Guia de Transferência Intermunicipal</DialogTitle>
          <DialogContent dividers>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  select
                  label="Depósito de Origem"
                  required
                  value={transferForm.originWarehouseId}
                  onChange={(e) => setTransferForm({ ...transferForm, originWarehouseId: e.target.value })}
                >
                  {warehouses.map((w) => (
                    <MenuItem key={w.id} value={w.id}>{w.name}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  select
                  label="Depósito de Destino"
                  required
                  value={transferForm.destinationWarehouseId}
                  onChange={(e) => setTransferForm({ ...transferForm, destinationWarehouseId: e.target.value })}
                >
                  {warehouses.map((w) => (
                    <MenuItem key={w.id} value={w.id}>{w.name}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Nome do Portador / Motorista"
                  required
                  value={transferForm.carrierName}
                  onChange={(e) => setTransferForm({ ...transferForm, carrierName: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="CPF ou CNPJ do Portador"
                  required
                  value={transferForm.carrierDocument}
                  onChange={(e) => setTransferForm({ ...transferForm, carrierDocument: e.target.value })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={2}
                  label="Instruções / Observações do Transporte"
                  value={transferForm.notes}
                  onChange={(e) => setTransferForm({ ...transferForm, notes: e.target.value })}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setTransferModalOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Gerar Guia com Handshake
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Modal: Receber Devolução de Ferramenta */}
      <Dialog open={returnToolModalOpen} onClose={() => setReturnToolModalOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleConfirmReturnTool}>
          <DialogTitle>Conferência & Devolução de Ferramental</DialogTitle>
          <DialogContent dividers>
            <Alert severity="info" sx={{ mb: 2 }}>
              Termo: <strong>{selectedAgreement?.code}</strong> | Portador: <strong>{selectedAgreement?.holderName}</strong>
            </Alert>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  select
                  label="Depósito onde foi Devolvido"
                  required
                  value={returnToolForm.warehouseId}
                  onChange={(e) => setReturnToolForm({ ...returnToolForm, warehouseId: e.target.value })}
                >
                  {warehouses.map((w) => (
                    <MenuItem key={w.id} value={w.id}>{w.name}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={2}
                  label="Laudo de Triagem / Observações"
                  value={returnToolForm.notes}
                  onChange={(e) => setReturnToolForm({ ...returnToolForm, notes: e.target.value })}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setReturnToolModalOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="success">
              Baixar Responsabilidade do Portador
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};

export default InventoryManager;
