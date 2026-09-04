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
  IconButton,
  Tooltip,
  FormControlLabel,
  Switch,
  InputAdornment
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
  Description as DocumentIcon,
  QrCodeScanner as QrIcon,
  Search as SearchIcon
} from '@mui/icons-material';
import { inventoryCustodyService, CarrierType } from '../../services/inventoryCustodyService';
import { inventoryService } from '../../services/inventoryService';
import { userService } from '../../services/userService';
import { useAuth } from '../../contexts/AuthContext';
import { toast } from 'react-toastify';

const InventoryManager = () => {
  const { user } = useAuth();
  const [currentTab, setCurrentTab] = useState(0);
  const [loading, setLoading] = useState(true);

  // Dados
  const [warehouses, setWarehouses] = useState([]);
  const [items, setItems] = useState([]);
  const [assets, setAssets] = useState([]);
  const [transfers, setTransfers] = useState([]);
  const [agreements, setAgreements] = useState([]);
  const [users, setUsers] = useState([]);

  // Filtros
  const [assetSearch, setAssetSearch] = useState('');
  const [assetCategoryFilter, setAssetCategoryFilter] = useState('ALL');
  const [assetStatusFilter, setAssetStatusFilter] = useState('ALL');

  // Modais
  const [warehouseModalOpen, setWarehouseModalOpen] = useState(false);
  const [transferModalOpen, setTransferModalOpen] = useState(false);
  const [toolModalOpen, setToolModalOpen] = useState(false);
  const [returnToolModalOpen, setReturnToolModalOpen] = useState(false);
  const [reverseLogisticsModalOpen, setReverseLogisticsModalOpen] = useState(false);

  // Seleções para Ações
  const [selectedAgreement, setSelectedAgreement] = useState(null);
  const [selectedAssetForReturn, setSelectedAssetForReturn] = useState(null);

  // Forms
  const [warehouseForm, setWarehouseForm] = useState({
    code: '',
    name: '',
    city: 'São Paulo',
    state: 'SP',
    address: ''
  });

  const [transferForm, setTransferForm] = useState({
    originWarehouseId: '',
    destinationWarehouseId: '',
    carrierUserId: '',
    carrierName: '',
    carrierDocument: '',
    carrierType: 'COLABORADOR' as CarrierType,
    notes: 'Transferência de equipamentos e insumos entre bases operacionais'
  });

  const [toolForm, setToolForm] = useState({
    holderUserId: '',
    holderName: '',
    holderCpf: '',
    isThirdParty: false,
    selectedAssetIds: [],
    totalPromissoryValue: 5000.0,
    notes: 'Empréstimo de ferramental de alto valor com termo de custódia e nota promissória executiva.'
  });

  const [returnToolForm, setReturnToolForm] = useState({
    warehouseId: '',
    isDamaged: false,
    returnPhotoUrl: '',
    notes: 'Equipamento conferido em bancada, limpo e em perfeito estado de funcionamento.'
  });

  const [reverseLogisticsForm, setReverseLogisticsForm] = useState({
    warehouseId: '',
    isDamaged: false,
    photoUrl: '',
    notes: 'Equipamento recolhido de cliente por cancelamento/mudança, aguardando triagem.'
  });

  const loadData = async () => {
    try {
      setLoading(true);
      const [wRes, iRes, aRes, tRes, gRes, uRes] = await Promise.all([
        inventoryCustodyService.getAllWarehouses(),
        inventoryService.getAllItems(),
        inventoryCustodyService.getAllAssets(),
        inventoryCustodyService.getAllTransfers(),
        inventoryCustodyService.getAllToolAgreements(),
        userService.getAllUsers().catch(() => ({ data: [] }))
      ]);

      setWarehouses(wRes.data || []);
      setItems(iRes.data || []);
      setAssets(aRes.data || []);
      setTransfers(tRes.data || []);
      setAgreements(gRes.data || []);
      setUsers(uRes.data || []);
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

  const getWarehouseName = (id) => {
    if (!id) return 'N/D';
    const w = warehouses.find((item) => item.id === id);
    return w ? w.name : 'Depósito #' + id.substring(0, 8);
  };

  const handleCreateWarehouse = async (e) => {
    e.preventDefault();
    try {
      await inventoryCustodyService.createWarehouse(warehouseForm);
      toast.success('Depósito cadastrado com sucesso!');
      setWarehouseModalOpen(false);
      setWarehouseForm({ code: '', name: '', city: 'São Paulo', state: 'SP', address: '' });
      loadData();
    } catch (err) {
      toast.error('Erro ao cadastrar depósito: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleCarrierUserChange = (userId) => {
    const selectedUser = users.find((u) => u.id === userId);
    if (selectedUser) {
      setTransferForm((prev) => ({
        ...prev,
        carrierUserId: selectedUser.id,
        carrierName: selectedUser.name || selectedUser.username,
        carrierDocument: selectedUser.cpf || '123.456.789-00'
      }));
    } else {
      setTransferForm((prev) => ({ ...prev, carrierUserId: userId }));
    }
  };

  const handleToolHolderChange = (userId) => {
    const selectedUser = users.find((u) => u.id === userId);
    if (selectedUser) {
      setToolForm((prev) => ({
        ...prev,
        holderUserId: selectedUser.id,
        holderName: selectedUser.name || selectedUser.username,
        holderCpf: selectedUser.cpf || '123.456.789-00'
      }));
    } else {
      setToolForm((prev) => ({ ...prev, holderUserId: userId }));
    }
  };

  const handleCreateTransfer = async (e) => {
    e.preventDefault();
    if (!transferForm.originWarehouseId || !transferForm.destinationWarehouseId) {
      toast.warning('Selecione os depósitos de origem e destino.');
      return;
    }
    if (transferForm.originWarehouseId === transferForm.destinationWarehouseId) {
      toast.warning('O depósito de destino deve ser diferente da origem.');
      return;
    }

    try {
      await inventoryCustodyService.createTransfer({
        originWarehouseId: transferForm.originWarehouseId,
        destinationWarehouseId: transferForm.destinationWarehouseId,
        carrierUserId: transferForm.carrierUserId || null,
        carrierName: transferForm.carrierName || 'Portador / Técnico',
        carrierDocument: transferForm.carrierDocument || '000.000.000-00',
        carrierType: transferForm.carrierType as CarrierType,
        notes: transferForm.notes
      });
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
        userId: user?.id || null,
        dispatchPhotoUrl: 'https://isperp.local/photos/dispatch-transfer.jpg'
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
        userId: user?.id || null,
        receiptPhotoUrl: 'https://isperp.local/photos/receipt-transfer.jpg'
      });
      toast.success('Recebimento confirmado! Itens creditados no destino e portador liberado.');
      loadData();
    } catch (err) {
      toast.error('Erro ao confirmar recebimento.');
    }
  };

  const handleCreateToolAgreement = async (e) => {
    e.preventDefault();
    if (!toolForm.holderName.trim() || !toolForm.holderCpf.trim()) {
      toast.warning('Informe o nome e CPF do portador responsável.');
      return;
    }
    if (toolForm.selectedAssetIds.length === 0) {
      toast.warning('Selecione pelo menos um equipamento para o termo.');
      return;
    }

    try {
      await inventoryCustodyService.checkoutToolAgreement({
        holderUserId: toolForm.holderUserId || null,
        holderName: toolForm.holderName.trim(),
        holderCpf: toolForm.holderCpf.trim(),
        isThirdParty: toolForm.isThirdParty,
        assetIds: toolForm.selectedAssetIds,
        totalPromissoryValue: Number(toolForm.totalPromissoryValue) || 0,
        notes: toolForm.notes
      });
      toast.success('Termo de Custódia e Nota Promissória emitidos com sucesso!');
      setToolModalOpen(false);
      setToolForm({
        holderUserId: '',
        holderName: '',
        holderCpf: '',
        isThirdParty: false,
        selectedAssetIds: [],
        totalPromissoryValue: 5000.0,
        notes: ''
      });
      loadData();
    } catch (err) {
      toast.error('Erro ao emitir termo: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleOpenReturnTool = (agr) => {
    setSelectedAgreement(agr);
    setReturnToolForm({
      warehouseId: warehouses[0]?.id || '',
      isDamaged: false,
      returnPhotoUrl: '',
      notes: 'Equipamento conferido em bancada, limpo e em perfeito estado de funcionamento.'
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

  const handleOpenReverseLogistics = (asset) => {
    setSelectedAssetForReturn(asset);
    setReverseLogisticsForm({
      warehouseId: warehouses[0]?.id || '',
      isDamaged: false,
      photoUrl: '',
      notes: 'ONU/Roteador recolhido em visita técnica, encaminhado para triagem.'
    });
    setReverseLogisticsModalOpen(true);
  };

  const handleConfirmReverseLogistics = async (e) => {
    e.preventDefault();
    try {
      await inventoryCustodyService.returnAssetFromWorkOrder(selectedAssetForReturn.id, reverseLogisticsForm);
      toast.success('Equipamento recebido no almoxarifado para triagem!');
      setReverseLogisticsModalOpen(false);
      loadData();
    } catch (err) {
      toast.error('Erro ao registrar logística reversa.');
    }
  };

  // Filtragem de Ativos Serializados
  const filteredAssets = assets.filter((a) => {
    const matchSearch =
      (a.serialNumber && a.serialNumber.toLowerCase().includes(assetSearch.toLowerCase())) ||
      (a.macAddress && a.macAddress.toLowerCase().includes(assetSearch.toLowerCase())) ||
      (a.brandModel && a.brandModel.toLowerCase().includes(assetSearch.toLowerCase()));
    const matchCategory = assetCategoryFilter === 'ALL' || a.category === assetCategoryFilter;
    const matchStatus = assetStatusFilter === 'ALL' || a.status === assetStatusFilter;
    return matchSearch && matchCategory && matchStatus;
  });

  const availableToolsForCheckout = assets.filter(
    (a) => a.category.startsWith('TOOL_') && a.status === 'DISPONIVEL_DEPOSITO'
  );

  return (
    <Box sx={{ p: { xs: 2, sm: 3 } }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Almoxarifado & Custódia de Ativos
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Multi-depósitos, rastreabilidade serial (MAC/Serial), transferências com handshake e termos de responsabilidade técnica.
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
                TERMOS DE CUSTÓDIA
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
                ATIVOS SERIALIZADOS
              </Typography>
              <Typography variant="h4" fontWeight="bold" sx={{ color: '#9c27b0' }}>
                {assets.length}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                ONTs, Roteadores e Ferramental
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
          <Tab label="Depósitos & Materiais" icon={<WarehouseIcon />} iconPosition="start" />
          <Tab label="Ativos Serializados (ONTs / Roteadores)" icon={<QrIcon />} iconPosition="start" />
          <Tab label="Custódia de Ferramental" icon={<ToolIcon />} iconPosition="start" />
          <Tab label="Transferências Inter-Bases" icon={<TruckIcon />} iconPosition="start" />
          <Tab label="Logística Reversa (Campo)" icon={<ReturnIcon />} iconPosition="start" />
        </Tabs>
      </Paper>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          {/* ABA 0: Depósitos & Materiais */}
          {currentTab === 0 && (
            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6" fontWeight="bold">
                  Depósitos & Almoxarifados Físicos
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
                  <Grid item xs={12} sm={6} md={4} key={w.id}>
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
                Catálogo de Insumos & Materiais
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
                    {items.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                          Nenhum insumo cadastrado.
                        </TableCell>
                      </TableRow>
                    ) : (
                      items.map((item) => {
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
                      })
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}

          {/* ABA 1: Ativos Serializados (ONTs / Roteadores) */}
          {currentTab === 1 && (
            <Box>
              <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
                <TextField
                  placeholder="Buscar por MAC, número de série ou modelo..."
                  size="small"
                  value={assetSearch}
                  onChange={(e) => setAssetSearch(e.target.value)}
                  sx={{ minWidth: 280, flexGrow: 1 }}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon />
                      </InputAdornment>
                    ),
                  }}
                />
                <TextField
                  select
                  size="small"
                  label="Categoria"
                  value={assetCategoryFilter}
                  onChange={(e) => setAssetCategoryFilter(e.target.value)}
                  sx={{ minWidth: 160 }}
                >
                  <MenuItem value="ALL">Todas Categorias</MenuItem>
                  <MenuItem value="ONU_ONT">ONU / ONT</MenuItem>
                  <MenuItem value="ROUTER_MESH">Roteador Wi-Fi Mesh</MenuItem>
                  <MenuItem value="TOOL_FUSION_MACHINE">Máquina de Fusão</MenuItem>
                  <MenuItem value="TOOL_OTDR">OTDR</MenuItem>
                  <MenuItem value="TOOL_POWER_METER">Power Meter</MenuItem>
                  <MenuItem value="TOOL_CLEAVER">Clivador</MenuItem>
                </TextField>
                <TextField
                  select
                  size="small"
                  label="Status do Ativo"
                  value={assetStatusFilter}
                  onChange={(e) => setAssetStatusFilter(e.target.value)}
                  sx={{ minWidth: 180 }}
                >
                  <MenuItem value="ALL">Todos os Status</MenuItem>
                  <MenuItem value="DISPONIVEL_DEPOSITO">Disponível em Depósito</MenuItem>
                  <MenuItem value="CUSTODIA_COLABORADOR">Em Custódia Técnico</MenuItem>
                  <MenuItem value="EM_TRANSITO">Em Trânsito</MenuItem>
                  <MenuItem value="INSTALADO_CLIENTE">Instalado no Cliente</MenuItem>
                  <MenuItem value="RETIRADO_PENDENTE_DEVOLUCAO">Retirado (Devolução)</MenuItem>
                  <MenuItem value="DEFEITO_TRIAGEM">Defeito / Triagem</MenuItem>
                </TextField>
              </Box>

              <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 2 }}>
                <Table>
                  <TableHead sx={{ bgcolor: '#f8f9fa' }}>
                    <TableRow>
                      <TableCell><strong>Número de Série</strong></TableCell>
                      <TableCell><strong>MAC Address</strong></TableCell>
                      <TableCell><strong>Marca / Modelo</strong></TableCell>
                      <TableCell><strong>Categoria</strong></TableCell>
                      <TableCell><strong>Localização / Portador</strong></TableCell>
                      <TableCell><strong>Status</strong></TableCell>
                      <TableCell align="right"><strong>Valor Reposição</strong></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {filteredAssets.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                          Nenhum ativo serializado encontrado.
                        </TableCell>
                      </TableRow>
                    ) : (
                      filteredAssets.map((a) => (
                        <TableRow key={a.id} hover>
                          <TableCell sx={{ fontWeight: 'bold', fontFamily: 'monospace' }}>
                            {a.serialNumber}
                          </TableCell>
                          <TableCell sx={{ fontFamily: 'monospace' }}>
                            {a.macAddress || '—'}
                          </TableCell>
                          <TableCell>{a.brandModel}</TableCell>
                          <TableCell>
                            <Chip label={a.category} size="small" variant="outlined" />
                          </TableCell>
                          <TableCell>
                            {a.currentWarehouseId ? (
                              <Typography variant="body2">🏢 {getWarehouseName(a.currentWarehouseId)}</Typography>
                            ) : a.currentHolderUserId ? (
                              <Typography variant="body2">👷 Técnico com custódia</Typography>
                            ) : a.currentCustomerId ? (
                              <Typography variant="body2">🏠 Instalado no cliente</Typography>
                            ) : (
                              <Typography variant="caption" color="text.secondary">Não definido</Typography>
                            )}
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={a.status}
                              size="small"
                              color={
                                a.status === 'DISPONIVEL_DEPOSITO'
                                  ? 'success'
                                  : a.status === 'INSTALADO_CLIENTE'
                                  ? 'primary'
                                  : a.status === 'CUSTODIA_COLABORADOR' || a.status === 'EM_TRANSITO'
                                  ? 'warning'
                                  : 'error'
                              }
                            />
                          </TableCell>
                          <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                            R$ {Number(a.replacementValue || 0).toFixed(2).replace('.', ',')}
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}

          {/* ABA 2: Custódia & Ferramental de Alto Valor */}
          {currentTab === 2 && (
            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 2 }}>
                <Box>
                  <Typography variant="h6" fontWeight="bold">
                    Termos de Cautela & Notas Promissórias Executivas
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Controle de máquinas de fusão, OTDRs e ferramentas cedidas a técnicos ou terceiros com valor legal.
                  </Typography>
                </Box>
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={() => setToolModalOpen(true)}
                  sx={{ borderRadius: 2 }}
                >
                  Emitir Termo de Custódia
                </Button>
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
                            R$ {Number(agr.totalPromissoryValue || 0).toFixed(2).replace('.', ',')}
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

          {/* ABA 3: Transferências Intermunicipais */}
          {currentTab === 3 && (
            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 2 }}>
                <Box>
                  <Typography variant="h6" fontWeight="bold">
                    Transferências Inter-Bases com Duplo Aceite (Handshake)
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
                  Nenhuma transferência inter-bases registrada.
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
                            <Chip label={getWarehouseName(trf.originWarehouseId)} size="small" sx={{ mr: 1 }} />
                            ➔
                            <Chip label={getWarehouseName(trf.destinationWarehouseId)} size="small" color="primary" sx={{ ml: 1 }} />
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

          {/* ABA 4: Logística Reversa */}
          {currentTab === 4 && (
            <Box>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Logística Reversa • ONUs & Roteadores Recolhidos em Campo
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Equipamentos recolhidos de clientes por cancelamento que estão sob custódia do técnico aguardando triagem em bancada.
              </Typography>

              {assets.filter((a) => a.status === 'RETIRADO_PENDENTE_DEVOLUCAO' || a.status === 'DEFEITO_TRIAGEM').length === 0 ? (
                <Alert severity="success" sx={{ borderRadius: 2 }}>
                  Nenhum equipamento pendente de triagem ou logística reversa no momento!
                </Alert>
              ) : (
                <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 2 }}>
                  <Table>
                    <TableHead sx={{ bgcolor: '#f8f9fa' }}>
                      <TableRow>
                        <TableCell><strong>Número de Série</strong></TableCell>
                        <TableCell><strong>MAC Address</strong></TableCell>
                        <TableCell><strong>Modelo</strong></TableCell>
                        <TableCell><strong>Status</strong></TableCell>
                        <TableCell align="right"><strong>Ações</strong></TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {assets
                        .filter((a) => a.status === 'RETIRADO_PENDENTE_DEVOLUCAO' || a.status === 'DEFEITO_TRIAGEM')
                        .map((asset) => (
                          <TableRow key={asset.id} hover>
                            <TableCell sx={{ fontWeight: 'bold' }}>{asset.serialNumber}</TableCell>
                            <TableCell>{asset.macAddress || '—'}</TableCell>
                            <TableCell>{asset.brandModel}</TableCell>
                            <TableCell>
                              <Chip label={asset.status} size="small" color="warning" />
                            </TableCell>
                            <TableCell align="right">
                              <Button
                                size="small"
                                variant="contained"
                                color="primary"
                                startIcon={<ReturnIcon />}
                                onClick={() => handleOpenReverseLogistics(asset)}
                              >
                                Receber no Depósito
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </Box>
          )}
        </>
      )}

      {/* Modal: Novo Depósito */}
      <Dialog open={warehouseModalOpen} onClose={() => setWarehouseModalOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleCreateWarehouse}>
          <DialogTitle sx={{ bgcolor: '#1976d2', color: '#fff', fontWeight: 'bold' }}>
            Cadastrar Novo Depósito / Almoxarifado
          </DialogTitle>
          <DialogContent sx={{ pt: 3 }}>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Código Único *"
                  required
                  placeholder="Ex: DEP-CENTRAL-01"
                  value={warehouseForm.code}
                  onChange={(e) => setWarehouseForm({ ...warehouseForm, code: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Nome do Depósito *"
                  required
                  placeholder="Ex: Almoxarifado Central"
                  value={warehouseForm.name}
                  onChange={(e) => setWarehouseForm({ ...warehouseForm, name: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Cidade *"
                  required
                  value={warehouseForm.city}
                  onChange={(e) => setWarehouseForm({ ...warehouseForm, city: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Estado *"
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
          <DialogActions sx={{ p: 2 }}>
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
          <DialogTitle sx={{ bgcolor: '#1976d2', color: '#fff', fontWeight: 'bold' }}>
            Nova Guia de Transferência Inter-Bases
          </DialogTitle>
          <DialogContent sx={{ pt: 3 }}>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  select
                  label="Depósito de Origem *"
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
                  label="Depósito de Destino *"
                  required
                  value={transferForm.destinationWarehouseId}
                  onChange={(e) => setTransferForm({ ...transferForm, destinationWarehouseId: e.target.value })}
                >
                  {warehouses.map((w) => (
                    <MenuItem key={w.id} value={w.id}>{w.name}</MenuItem>
                  ))}
                </TextField>
              </Grid>

              {users.length > 0 && (
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    select
                    label="Selecionar Colaborador / Portador (Opcional)"
                    value={transferForm.carrierUserId}
                    onChange={(e) => handleCarrierUserChange(e.target.value)}
                  >
                    <MenuItem value="">Preenchimento Manual</MenuItem>
                    {users.map((u) => (
                      <MenuItem key={u.id} value={u.id}>
                        {u.name || u.username} ({u.role})
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
              )}

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Nome do Portador *"
                  required
                  value={transferForm.carrierName}
                  onChange={(e) => setTransferForm({ ...transferForm, carrierName: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="CPF ou CNPJ do Portador *"
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
          <DialogActions sx={{ p: 2 }}>
            <Button onClick={() => setTransferModalOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Gerar Guia com Handshake
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Modal: Emitir Termo de Custódia / Cautela */}
      <Dialog open={toolModalOpen} onClose={() => setToolModalOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleCreateToolAgreement}>
          <DialogTitle sx={{ bgcolor: '#1976d2', color: '#fff', fontWeight: 'bold' }}>
            Emitir Termo de Custódia & Cautela de Ferramental
          </DialogTitle>
          <DialogContent sx={{ pt: 3 }}>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              {users.length > 0 && (
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    select
                    label="Selecionar Técnico Cadastrado (Opcional)"
                    value={toolForm.holderUserId}
                    onChange={(e) => handleToolHolderChange(e.target.value)}
                  >
                    <MenuItem value="">Preenchimento Manual</MenuItem>
                    {users.map((u) => (
                      <MenuItem key={u.id} value={u.id}>
                        {u.name || u.username} ({u.role})
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
              )}

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Nome do Responsável *"
                  required
                  value={toolForm.holderName}
                  onChange={(e) => setToolForm({ ...toolForm, holderName: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="CPF do Responsável *"
                  required
                  value={toolForm.holderCpf}
                  onChange={(e) => setToolForm({ ...toolForm, holderCpf: e.target.value })}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Valor da Nota Promissória (R$) *"
                  required
                  value={toolForm.totalPromissoryValue}
                  onChange={(e) => setToolForm({ ...toolForm, totalPromissoryValue: parseFloat(e.target.value) || 0 })}
                />
              </Grid>
              <Grid item xs={12} sm={6} sx={{ display: 'flex', alignItems: 'center' }}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={toolForm.isThirdParty}
                      onChange={(e) => setToolForm({ ...toolForm, isThirdParty: e.target.checked })}
                    />
                  }
                  label="Prestador Terceirizado (PJ)"
                />
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  select
                  SelectProps={{ multiple: true }}
                  label="Equipamentos / Ferramentas em Cautela *"
                  required
                  value={toolForm.selectedAssetIds}
                  onChange={(e) => setToolForm({ ...toolForm, selectedAssetIds: typeof e.target.value === 'string' ? e.target.value.split(',') : (e.target.value as any) })}
                  helperText="Selecione as máquinas e ferramentas disponíveis em estoque"
                >
                  {availableToolsForCheckout.length === 0 ? (
                    <MenuItem disabled value="">Nenhuma ferramenta disponível no depósito</MenuItem>
                  ) : (
                    availableToolsForCheckout.map((tool) => (
                      <MenuItem key={tool.id} value={tool.id}>
                        {tool.brandModel} - Serial: {tool.serialNumber} (R$ {Number(tool.replacementValue || 0).toFixed(2)})
                      </MenuItem>
                    ))
                  )}
                </TextField>
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={2}
                  label="Observações / Condições de Uso"
                  value={toolForm.notes}
                  onChange={(e) => setToolForm({ ...toolForm, notes: e.target.value })}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ p: 2 }}>
            <Button onClick={() => setToolModalOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Emitir Termo com Nota Promissória
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Modal: Receber Devolução de Ferramenta */}
      <Dialog open={returnToolModalOpen} onClose={() => setReturnToolModalOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleConfirmReturnTool}>
          <DialogTitle sx={{ bgcolor: '#2e7d32', color: '#fff', fontWeight: 'bold' }}>
            Conferência & Devolução de Ferramental
          </DialogTitle>
          <DialogContent sx={{ pt: 3 }}>
            <Alert severity="info" sx={{ mb: 2, mt: 0.5 }}>
              Termo: <strong>{selectedAgreement?.code}</strong> | Portador: <strong>{selectedAgreement?.holderName}</strong>
            </Alert>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  select
                  label="Depósito onde foi Devolvido *"
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
                <FormControlLabel
                  control={
                    <Switch
                      checked={returnToolForm.isDamaged}
                      onChange={(e) => setReturnToolForm({ ...returnToolForm, isDamaged: e.target.checked })}
                      color="error"
                    />
                  }
                  label="Equipamento devolvido com avaria/defeito"
                />
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
          <DialogActions sx={{ p: 2 }}>
            <Button onClick={() => setReturnToolModalOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="success">
              Baixar Responsabilidade do Portador
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Modal: Logística Reversa (Devolução de Campo) */}
      <Dialog open={reverseLogisticsModalOpen} onClose={() => setReverseLogisticsModalOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleConfirmReverseLogistics}>
          <DialogTitle sx={{ bgcolor: '#1976d2', color: '#fff', fontWeight: 'bold' }}>
            Triagem & Entrada no Almoxarifado (Logística Reversa)
          </DialogTitle>
          <DialogContent sx={{ pt: 3 }}>
            <Alert severity="info" sx={{ mb: 2, mt: 0.5 }}>
              Serial: <strong>{selectedAssetForReturn?.serialNumber}</strong> | Modelo: <strong>{selectedAssetForReturn?.brandModel}</strong>
            </Alert>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  select
                  label="Depósito de Destino *"
                  required
                  value={reverseLogisticsForm.warehouseId}
                  onChange={(e) => setReverseLogisticsForm({ ...reverseLogisticsForm, warehouseId: e.target.value })}
                >
                  {warehouses.map((w) => (
                    <MenuItem key={w.id} value={w.id}>{w.name}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={reverseLogisticsForm.isDamaged}
                      onChange={(e) => setReverseLogisticsForm({ ...reverseLogisticsForm, isDamaged: e.target.checked })}
                      color="error"
                    />
                  }
                  label="Equipamento com avaria / necessita de reparo técnico"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={2}
                  label="Notas de Triagem"
                  value={reverseLogisticsForm.notes}
                  onChange={(e) => setReverseLogisticsForm({ ...reverseLogisticsForm, notes: e.target.value })}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ p: 2 }}>
            <Button onClick={() => setReverseLogisticsModalOpen(false)}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Confirmar Entrada no Depósito
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};

export default InventoryManager;
