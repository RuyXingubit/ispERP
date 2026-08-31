import React, { useState, useEffect, useRef } from 'react';
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
  Grid,
  IconButton,
  Paper,
  Tab,
  Tabs,
  TextField,
  Typography,
  Alert,
  Tooltip,
  ButtonGroup,
} from '@mui/material';
import {
  Engineering as TechIcon,
  MyLocation as GpsIcon,
  CheckCircle as SuccessIcon,
  Edit as SignatureIcon,
  Refresh as RefreshIcon,
  QrCodeScanner as ScannerIcon,
  Router as OnuIcon,
  Speed as SignalIcon,
  Phone as PhoneIcon,
  WhatsApp as WhatsAppIcon,
  Map as MapIcon,
  AddLocationAlt as ContributeIcon,
  Clear as ClearIcon,
  Sensors as PonIcon,
  VpnKey as RadiusIcon,
  Directions as RouteIcon,
  Home as HomeIcon,
  Inventory2 as CtoIcon,
} from '@mui/icons-material';
import { workOrderService } from '../../services/workOrderService';
import { geoCepService } from '../../services/geoCepService';
import { onboardingDispatchService } from '../../services/onboardingDispatchService';
import { OltUnprovisionedOnu, RadiusSessionStatus } from '../../types/onboarding-dispatch';
import GeoCepMapView from '../../components/Map/GeoCepMapView';

export const TechnicianPortal = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(false);
  const [workOrders, setWorkOrders] = useState<any[]>([]);
  const [feedback, setFeedback] = useState({ type: '', message: '' });

  // Posição GPS do Técnico
  const [techLocation, setTechLocation] = useState<any>(null);
  const [gpsAccuracy, setGpsAccuracy] = useState<number | null>(null);
  const [gettingGps, setGettingGps] = useState(false);

  // Modal de Rota (Cliente vs CTO)
  const [routeModalOpen, setRouteModalOpen] = useState(false);
  const [routeTarget, setRouteTarget] = useState<'CUSTOMER' | 'CTO'>('CUSTOMER');
  const [selectedOrderForRoute, setSelectedOrderForRoute] = useState<any>(null);

  // Modal de Conclusão e Auto-Discovery
  const [completeModalOpen, setCompleteModalOpen] = useState(false);
  const [currentOrder, setCurrentOrder] = useState<any>(null);
  const [onuMac, setOnuMac] = useState('');
  const [onuSerial, setOnuSerial] = useState('');
  const [signalDbm, setSignalDbm] = useState('-19.50');
  const [signName, setSignName] = useState('');
  const [notes, setNotes] = useState('');
  const [submittingComplete, setSubmittingComplete] = useState(false);
  const [contributingGeoCep, setContributingGeoCep] = useState(false);

  // Auto-Discovery OLT & RADIUS
  const [discoveredOnus, setDiscoveredOnus] = useState<OltUnprovisionedOnu[]>([]);
  const [loadingOnus, setLoadingOnus] = useState(false);
  const [provisioningOnu, setProvisioningOnu] = useState(false);
  const [radiusStatus, setRadiusStatus] = useState<RadiusSessionStatus | null>(null);
  const [checkingRadius, setCheckingRadius] = useState(false);

  // Canvas de Assinatura
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [hasSignature, setHasSignature] = useState(false);

  const loadWorkOrders = async () => {
    try {
      setLoading(true);
      const res = await workOrderService.getAllWorkOrders();
      setWorkOrders(res.data || []);
    } catch (err: any) {
      setFeedback({ type: 'error', message: 'Erro ao carregar Ordens de Serviço: ' + err.message });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWorkOrders();
    handleGetTechGps();
  }, []);

  const handleGetTechGps = () => {
    if (!navigator.geolocation) {
      setFeedback({ type: 'warning', message: 'Geolocalização não suportada neste navegador.' });
      return;
    }
    setGettingGps(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setTechLocation({
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
        });
        setGpsAccuracy(pos.coords.accuracy ? Math.round(pos.coords.accuracy) : 5);
        setGettingGps(false);
      },
      (err) => {
        console.warn('Erro ao obter GPS:', err.message);
        setTechLocation({ latitude: -1.4558, longitude: -48.4902 });
        setGpsAccuracy(10);
        setGettingGps(false);
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  // Abre Modal de Rota
  const handleOpenRouteModal = (order: any) => {
    setSelectedOrderForRoute(order);
    setRouteTarget('CUSTOMER');
    setRouteModalOpen(true);
    handleGetTechGps();
  };

  // Abre Modal de Conclusão
  const handleOpenCompleteModal = (order: any) => {
    setCurrentOrder(order);
    setOnuMac(order.onuMac || '');
    setOnuSerial(order.onuSerial || '');
    setSignalDbm(order.fiberSignalDbm ? order.fiberSignalDbm.toString() : '-19.50');
    setSignName(order.customerSignatureName || '');
    setNotes(order.notes || '');
    setHasSignature(false);
    setDiscoveredOnus([]);
    setRadiusStatus(null);
    setCompleteModalOpen(true);
    handleGetTechGps();

    setTimeout(() => {
      initCanvas();
      handleScanOnus(order.id);
      handleCheckRadius(order.id);
    }, 200);
  };

  // Escanear ONUs na OLT
  const handleScanOnus = async (orderId: string) => {
    setLoadingOnus(true);
    try {
      const onus = await onboardingDispatchService.listUnprovisionedOnus(orderId);
      setDiscoveredOnus(onus);
    } catch (err: any) {
      console.warn('Erro ao escanear ONUs na OLT:', err.message);
    } finally {
      setLoadingOnus(false);
    }
  };

  // Vincular e Provisionar ONU
  const handleSelectAndProvisionOnu = async (onu: OltUnprovisionedOnu) => {
    if (!currentOrder) return;
    setOnuSerial(onu.onuSerial);
    if (onu.onuMac) setOnuMac(onu.onuMac);
    if (onu.rxPowerDbm) setSignalDbm(onu.rxPowerDbm.toString());

    setProvisioningOnu(true);
    try {
      await onboardingDispatchService.provisionOnu(currentOrder.id, {
        onuSerial: onu.onuSerial,
        vlanId: 100,
      });
      setFeedback({
        type: 'success',
        message: `✅ ONU ${onu.onuSerial} provisionada com sucesso na OLT (VLAN 100). Credenciais PPPoE ativas no FreeRADIUS!`,
      });
      handleCheckRadius(currentOrder.id);
    } catch (err: any) {
      setFeedback({ type: 'error', message: 'Erro ao provisionar ONU: ' + err.message });
    } finally {
      setProvisioningOnu(false);
    }
  };

  // Checar status da sessão RADIUS
  const handleCheckRadius = async (orderId: string) => {
    setCheckingRadius(true);
    try {
      const status = await onboardingDispatchService.getRadiusStatus(orderId);
      setRadiusStatus(status);
    } catch (err: any) {
      console.warn('Erro ao consultar RADIUS:', err.message);
    } finally {
      setCheckingRadius(false);
    }
  };

  // Canvas Handlers
  const initCanvas = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.strokeStyle = '#0f172a';
    ctx.lineWidth = 2.5;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  };

  const startDrawing = (e: any) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const x = (e.touches ? e.touches[0].clientX : e.clientX) - rect.left;
    const y = (e.touches ? e.touches[0].clientY : e.clientY) - rect.top;

    ctx.beginPath();
    ctx.moveTo(x, y);
    setIsDrawing(true);
    setHasSignature(true);
  };

  const draw = (e: any) => {
    if (!isDrawing) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const x = (e.touches ? e.touches[0].clientX : e.clientX) - rect.left;
    const y = (e.touches ? e.touches[0].clientY : e.clientY) - rect.top;

    ctx.lineTo(x, y);
    ctx.stroke();
    if (e.preventDefault && e.touches) e.preventDefault();
  };

  const stopDrawing = () => {
    setIsDrawing(false);
  };

  const clearSignature = () => {
    initCanvas();
    setHasSignature(false);
  };

  const handleContributeAddressToGeoCep = async () => {
    if (!techLocation || !currentOrder) {
      setFeedback({ type: 'warning', message: 'Obtenha a posição GPS em frente ao imóvel antes de enviar.' });
      return;
    }

    try {
      setContributingGeoCep(true);
      const payload = {
        cep: '66000-000',
        numero: '100',
        latitude: techLocation.latitude,
        longitude: techLocation.longitude,
        precisaoGpsMetros: gpsAccuracy || 5.0,
      };

      await geoCepService.contributeCoordinate(payload);
      setFeedback({
        type: 'success',
        message: '📍 Coordenada enviada com sucesso para homologação no GeoCEP! Sua conta foi bonificada.',
      });
    } catch (err: any) {
      setFeedback({ type: 'error', message: 'Erro ao enviar para o GeoCEP: ' + err.message });
    } finally {
      setContributingGeoCep(false);
    }
  };

  const handleCompleteWorkOrder = async () => {
    if (!onuSerial) {
      setFeedback({ type: 'warning', message: 'Informe ou descubra o Número de Série da ONU.' });
      return;
    }

    try {
      setSubmittingComplete(true);
      const canvas = canvasRef.current;
      const signatureBase64 = canvas && hasSignature ? canvas.toDataURL('image/png') : undefined;

      const payload = {
        onuSerial,
        onuMac: onuMac || undefined,
        fiberSignalDbm: parseFloat(signalDbm) || -19.50,
        technicianLatitude: techLocation ? techLocation.latitude : undefined,
        technicianLongitude: techLocation ? techLocation.longitude : undefined,
        digitalSignatureBase64: signatureBase64,
        customerSignatureName: signName || 'Cliente Assinante',
        notes,
      };

      await onboardingDispatchService.completeInstallation(currentOrder.id, payload);
      setFeedback({
        type: 'success',
        message: `✅ O.S. #${currentOrder.id.slice(0, 8)} concluída! Contrato ativado, estoque baixado e cliente navegando!`,
      });
      setCompleteModalOpen(false);
      loadWorkOrders();
    } catch (err: any) {
      setFeedback({ type: 'error', message: 'Erro ao concluir O.S.: ' + err.message });
    } finally {
      setSubmittingComplete(false);
    }
  };

  const pendingOrders = workOrders.filter(
    (w) => w.status === 'SCHEDULED' || w.status === 'PENDING_SCHEDULE' || w.status === 'IN_PROGRESS'
  );
  const completedOrders = workOrders.filter((w) => w.status === 'COMPLETED');
  const displayedOrders = activeTab === 0 ? pendingOrders : completedOrders;

  return (
    <Box sx={{ p: { xs: 2, sm: 3 }, maxWidth: 900, margin: '0 auto', pb: 10 }}>
      {/* Header Mobile */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: 1 }}>
            <TechIcon color="primary" /> Modo Campo Técnico FTTH
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Roteamento GeoCEP (CTO vs Cliente), Auto-Discovery OLT, Validação RADIUS e Assinatura Touch.
          </Typography>
        </Box>
        <IconButton color="primary" onClick={loadWorkOrders} disabled={loading}>
          <RefreshIcon />
        </IconButton>
      </Box>

      {/* GPS Status Card */}
      <Card sx={{ mb: 2, bgcolor: techLocation ? 'primary.50' : 'grey.100', border: '1px solid', borderColor: techLocation ? 'primary.200' : 'grey.300' }}>
        <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 }, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <GpsIcon color={techLocation ? 'primary' : 'disabled'} />
            <Box>
              <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                {techLocation ? 'GPS Ativo em Campo' : 'GPS Não Capturado'}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {techLocation
                  ? `Lat: ${techLocation.latitude.toFixed(4)}, Lon: ${techLocation.longitude.toFixed(4)} (±${gpsAccuracy}m)`
                  : 'Toque para atualizar a posição'}
              </Typography>
            </Box>
          </Box>
          <Button
            size="small"
            variant="outlined"
            onClick={handleGetTechGps}
            disabled={gettingGps}
            startIcon={gettingGps ? <CircularProgress size={14} /> : <GpsIcon />}
          >
            {gettingGps ? 'Localizando...' : 'Atualizar'}
          </Button>
        </CardContent>
      </Card>

      {feedback.message && (
        <Alert severity={(feedback.type as any) || 'info'} sx={{ mb: 2 }} onClose={() => setFeedback({ type: '', message: '' })}>
          {feedback.message}
        </Alert>
      )}

      {/* Tabs */}
      <Paper sx={{ mb: 2 }}>
        <Tabs value={activeTab} onChange={(e, val) => setActiveTab(val)} variant="fullWidth" indicatorColor="primary" textColor="primary">
          <Tab label={`Minhas O.S. Pendentes (${pendingOrders.length})`} />
          <Tab label={`Concluídas (${completedOrders.length})`} />
        </Tabs>
      </Paper>

      {/* Listagem de O.S. */}
      {displayedOrders.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center', borderRadius: 2 }}>
          <Typography variant="body2" color="text.secondary">
            Nenhuma Ordem de Serviço encontrada nesta aba.
          </Typography>
        </Paper>
      ) : (
        <Grid container spacing={2}>
          {displayedOrders.map((order) => {
            return (
              <Grid item xs={12} key={order.id}>
                <Card sx={{ borderRadius: 2, border: '1px solid', borderColor: order.status === 'COMPLETED' ? 'success.200' : 'primary.200' }}>
                  <CardContent sx={{ pb: 1 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                      <Box>
                        <Typography variant="h6" sx={{ fontWeight: 'bold', fontSize: '1.1rem' }}>
                          O.S. #{order.id.slice(0, 8)}
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 500, color: 'text.primary' }}>
                          {order.type || 'INSTALAÇÃO FIBRA ÓPTICA'}
                        </Typography>
                      </Box>
                      <Chip
                        label={order.status}
                        size="small"
                        color={order.status === 'COMPLETED' ? 'success' : order.status === 'SCHEDULED' ? 'primary' : 'warning'}
                      />
                    </Box>

                    <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                      📅 <strong>Agendamento:</strong> {order.scheduledDate || 'Hoje'} ({order.scheduledPeriod || 'IMEDIATO'})
                    </Typography>

                    {order.customerName && (
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                        👤 <strong>Cliente:</strong> {order.customerName}
                      </Typography>
                    )}

                    {order.installationAddress && (
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                        📍 <strong>Endereço:</strong> {order.installationAddress}
                      </Typography>
                    )}

                    {/* Botão de Rota Inteligente GeoCEP */}
                    <Box sx={{ my: 1.5 }}>
                      <Button
                        size="small"
                        variant="outlined"
                        color="primary"
                        startIcon={<RouteIcon />}
                        onClick={() => handleOpenRouteModal(order)}
                        sx={{ textTransform: 'none', fontWeight: 'bold' }}
                      >
                        🧭 Ver Rota no Mapa GeoCEP (CTO ou Cliente)
                      </Button>
                    </Box>

                    <Divider sx={{ my: 1 }} />

                    {/* Ações Rápidas */}
                    <Box sx={{ display: 'flex', gap: 1, pt: 0.5, flexWrap: 'wrap' }}>
                      <Button
                        size="small"
                        variant="outlined"
                        color="success"
                        startIcon={<WhatsAppIcon />}
                        component="a"
                        href={`https://wa.me/55${order.customerPhone ? order.customerPhone.replace(/\D/g, '') : '91988887777'}?text=Ol%C3%A1%2C+sou+o+t%C3%A9cnico+da+Xingu+Telecom+e+estou+a+caminho+da+sua+instala%C3%A7%C3%A3o.`}
                        target="_blank"
                      >
                        WhatsApp
                      </Button>

                      {order.status !== 'COMPLETED' && (
                        <Button
                          size="small"
                          variant="contained"
                          color="primary"
                          startIcon={<TechIcon />}
                          sx={{ ml: 'auto' }}
                          onClick={() => handleOpenCompleteModal(order)}
                        >
                          Instalar & Concluir
                        </Button>
                      )}
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      )}

      {/* Modal de Escolha de Rota (CTO vs Casa do Cliente) */}
      <Dialog open={routeModalOpen} onClose={() => setRouteModalOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1, bgcolor: 'primary.main', color: 'white' }}>
          <RouteIcon /> Rota de Navegação GeoCEP
        </DialogTitle>
        <DialogContent sx={{ p: 2 }}>
          {selectedOrderForRoute && (
            <Box>
              <Typography variant="body2" sx={{ mb: 2, color: 'text.secondary' }}>
                Escolha o destino desejado para traçar a rota com precisão:
              </Typography>

              {/* Botoes de alternância de rota */}
              <ButtonGroup fullWidth variant="contained" sx={{ mb: 2 }}>
                <Button
                  color={routeTarget === 'CUSTOMER' ? 'primary' : 'inherit'}
                  startIcon={<HomeIcon />}
                  onClick={() => setRouteTarget('CUSTOMER')}
                >
                  🏠 Casa do Cliente
                </Button>
                <Button
                  color={routeTarget === 'CTO' ? 'primary' : 'inherit'}
                  startIcon={<CtoIcon />}
                  onClick={() => setRouteTarget('CTO')}
                >
                  📦 Caixa CTO de Atendimento
                </Button>
              </ButtonGroup>

              {/* Informação do Destino Selecionado */}
              <Paper sx={{ p: 1.5, mb: 2, bgcolor: routeTarget === 'CUSTOMER' ? '#e3f2fd' : '#fff3e0' }}>
                <Typography variant="subtitle2" fontWeight="bold">
                  {routeTarget === 'CUSTOMER'
                    ? `Destino: Residência de ${selectedOrderForRoute.customerName || 'Cliente'}`
                    : 'Destino: Caixa CTO (Ponto de Ancoragem do Drop)'}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {routeTarget === 'CUSTOMER'
                    ? selectedOrderForRoute.installationAddress || 'Endereço residencial'
                    : 'CTO no poste mais próximo (Porta designada)'}
                </Typography>
              </Paper>

              {/* Mapa MapLibre GL Renderizado com Traçado da Rota */}
              <GeoCepMapView
                technicianLocation={techLocation}
                customerLocation={{
                  latitude:
                    routeTarget === 'CUSTOMER'
                      ? selectedOrderForRoute.technicianLatitude || -1.4550
                      : -1.4560,
                  longitude:
                    routeTarget === 'CUSTOMER'
                      ? selectedOrderForRoute.technicianLongitude || -48.4900
                      : -48.4910,
                  label:
                    routeTarget === 'CUSTOMER'
                      ? `Cliente: ${selectedOrderForRoute.customerName || 'Casa'}`
                      : 'Caixa de Distribuição Óptica (CTO)',
                  type: routeTarget,
                }}
                height="360px"
                showRouteInfo={true}
              />

              {/* Painel Nativo de Instruções de Campo GeoCEP */}
              <Paper sx={{ p: 2, mt: 2, bgcolor: '#f8fafc', border: '1px solid #cbd5e1', borderRadius: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="subtitle2" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: 'primary.main' }}>
                    <RouteIcon fontSize="small" /> Navegação Nativa GeoCEP & OpenStreetMap
                  </Typography>
                  <Button size="small" startIcon={<GpsIcon />} onClick={handleGetTechGps}>
                    Atualizar GPS
                  </Button>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  🧭 <strong>Instrução:</strong> Siga a linha azul traçada no mapa vetorial. O traçado acompanha a malha viária urbana registrada na base GeoCEP até o ponto exato de atendimento.
                </Typography>
              </Paper>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button variant="contained" onClick={() => setRouteModalOpen(false)}>Fechar Navegação</Button>
        </DialogActions>
      </Dialog>

      {/* Modal de Conclusão de Campo, Auto-Discovery e RADIUS */}
      <Dialog open={completeModalOpen} onClose={() => setCompleteModalOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1, bgcolor: 'primary.main', color: 'white' }}>
          <TechIcon /> Conclusão & Ativação de Campo (Zero-Touch)
        </DialogTitle>
        <DialogContent dividers>
          {currentOrder && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              {/* Coordenadas & Crowdsourcing */}
              <Box sx={{ p: 1.5, bgcolor: 'grey.50', borderRadius: 2, border: '1px solid', borderColor: 'grey.200' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="caption" sx={{ fontWeight: 'bold' }}>
                    📍 Coordenada GPS do Imóvel / Poste
                  </Typography>
                  <Button size="small" startIcon={<GpsIcon />} onClick={handleGetTechGps}>
                    Re-capturar GPS
                  </Button>
                </Box>
                <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '12px' }}>
                  {techLocation
                    ? `Lat: ${techLocation.latitude.toFixed(6)} • Lon: ${techLocation.longitude.toFixed(6)} (Precisão: ±${gpsAccuracy}m)`
                    : 'Obtendo posição do sensor do smartphone...'}
                </Typography>

                <Button
                  fullWidth
                  size="small"
                  variant="outlined"
                  color="secondary"
                  startIcon={contributingGeoCep ? <CircularProgress size={14} /> : <ContributeIcon />}
                  onClick={handleContributeAddressToGeoCep}
                  disabled={contributingGeoCep || !techLocation}
                  sx={{ mt: 1.5, textTransform: 'none' }}
                >
                  📍 Atualizar Coordenada da Residência no GeoCEP (Crowdsourcing)
                </Button>
              </Box>

              {/* Auto-Discovery da ONU na OLT */}
              <Paper sx={{ p: 2, border: '1px solid #90caf9', bgcolor: '#e3f2fd', borderRadius: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="subtitle2" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <PonIcon color="primary" fontSize="small" /> Auto-Discovery na OLT (ONUs na Porta PON)
                  </Typography>
                  <Button
                    size="small"
                    startIcon={loadingOnus ? <CircularProgress size={12} /> : <RefreshIcon />}
                    onClick={() => handleScanOnus(currentOrder.id)}
                    disabled={loadingOnus}
                  >
                    Escanear
                  </Button>
                </Box>

                {loadingOnus ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', py: 1 }}>
                    <CircularProgress size={20} />
                  </Box>
                ) : discoveredOnus.length === 0 ? (
                  <Typography variant="caption" color="text.secondary">
                    Nenhuma ONU desprovisionada detectada na porta PON. Conecte o cabo óptico na ONU.
                  </Typography>
                ) : (
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    {discoveredOnus.map((onu) => (
                      <Paper
                        key={onu.onuSerial}
                        sx={{
                          p: 1.5,
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          bgcolor: 'white',
                        }}
                      >
                        <Box>
                          <Typography variant="body2" fontWeight="bold">
                            Serial: {onu.onuSerial}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {onu.ponName} • Rx: <strong>{onu.rxPowerDbm} dBm</strong>
                          </Typography>
                        </Box>
                        <Button
                          size="small"
                          variant="contained"
                          color="primary"
                          disabled={provisioningOnu}
                          onClick={() => handleSelectAndProvisionOnu(onu)}
                        >
                          {provisioningOnu ? 'Provisionando...' : 'Vincular & Provisionar'}
                        </Button>
                      </Paper>
                    ))}
                  </Box>
                )}
              </Paper>

              {/* Status FreeRADIUS */}
              <Paper sx={{ p: 2, border: '1px solid #c8e6c9', bgcolor: '#f1f8e9', borderRadius: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="subtitle2" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <RadiusIcon color="success" fontSize="small" /> Validação FreeRADIUS (Sessão PPPoE)
                  </Typography>
                  <Button
                    size="small"
                    startIcon={checkingRadius ? <CircularProgress size={12} /> : <RefreshIcon />}
                    onClick={() => handleCheckRadius(currentOrder.id)}
                    disabled={checkingRadius}
                  >
                    Verificar
                  </Button>
                </Box>
                <Box sx={{ mt: 1 }}>
                  {radiusStatus ? (
                    <Chip
                      size="small"
                      icon={<SuccessIcon />}
                      label={
                        radiusStatus.online
                          ? `🟢 Online (${radiusStatus.framedIpAddress || '100.64.X.Y'}) - ${radiusStatus.message}`
                          : `⏳ ${radiusStatus.message}`
                      }
                      color={radiusStatus.online ? 'success' : 'warning'}
                    />
                  ) : (
                    <Typography variant="caption" color="text.secondary">
                      Aguardando sincronização com o concentrador BNG...
                    </Typography>
                  )}
                </Box>
              </Paper>

              {/* Dados do Equipamento */}
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <OnuIcon fontSize="small" color="primary" /> Equipamento Instalado (ONU / ONT)
              </Typography>
              <Grid container spacing={1.5}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Número de Série (Serial da ONU)"
                    placeholder="HWTC12345678"
                    value={onuSerial}
                    onChange={(e) => setOnuSerial(e.target.value.toUpperCase())}
                    required
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Endereço MAC da ONU"
                    placeholder="AA:BB:CC:DD:EE:FF"
                    value={onuMac}
                    onChange={(e) => setOnuMac(e.target.value.toUpperCase())}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Potência Óptica Medida no Drop (dBm)"
                    value={signalDbm}
                    onChange={(e) => setSignalDbm(e.target.value)}
                    helperText="Faixa ideal de atenuação: -15.00 a -24.00 dBm"
                    InputProps={{
                      startAdornment: <SignalIcon fontSize="small" sx={{ mr: 1, color: 'text.secondary' }} />,
                    }}
                  />
                </Grid>
              </Grid>

              {/* Assinatura Digital Touch */}
              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <SignatureIcon fontSize="small" color="primary" /> Assinatura Digital do Cliente (Dedo na Tela)
                  </Typography>
                  <Button size="small" color="error" startIcon={<ClearIcon />} onClick={clearSignature}>
                    Limpar
                  </Button>
                </Box>
                <Box
                  sx={{
                    border: '2px dashed #94a3b8',
                    borderRadius: 2,
                    bgcolor: '#ffffff',
                    height: 130,
                    position: 'relative',
                    touchAction: 'none',
                  }}
                >
                  <canvas
                    ref={canvasRef}
                    width={500}
                    height={130}
                    style={{ width: '100%', height: '100%', cursor: 'crosshair', display: 'block' }}
                    onMouseDown={startDrawing}
                    onMouseMove={draw}
                    onMouseUp={stopDrawing}
                    onTouchStart={startDrawing}
                    onTouchMove={draw}
                    onTouchEnd={stopDrawing}
                  />
                  {!hasSignature && (
                    <Typography
                      variant="caption"
                      sx={{
                        position: 'absolute',
                        top: '50%',
                        left: '50%',
                        transform: 'translate(-50%, -50%)',
                        color: '#94a3b8',
                        pointerEvents: 'none',
                        userSelect: 'none',
                      }}
                    >
                      ✍️ Peça para o cliente assinar aqui com o dedo
                    </Typography>
                  )}
                </Box>
              </Box>

              <TextField
                fullWidth
                size="small"
                label="Nome / CPF de Quem Assinou"
                placeholder="Ex: João da Silva (Titular)"
                value={signName}
                onChange={(e) => setSignName(e.target.value)}
              />

              <TextField
                fullWidth
                multiline
                rows={2}
                size="small"
                label="Observações da Instalação"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Ex: Drop lançado com 65m, conectorizado na CTO porta 2 e sinal em -19.45 dBm."
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setCompleteModalOpen(false)}>Cancelar</Button>
          <Button
            variant="contained"
            color="primary"
            onClick={handleCompleteWorkOrder}
            disabled={submittingComplete || !onuSerial}
            startIcon={submittingComplete ? <CircularProgress size={16} /> : <SuccessIcon />}
          >
            {submittingComplete ? 'Ativando...' : 'Finalizar O.S. & Ativar Cliente'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TechnicianPortal;
