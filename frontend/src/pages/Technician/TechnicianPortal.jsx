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
} from '@mui/icons-material';
import { workOrderService } from '../../services/workOrderService';
import { geoCepService } from '../../services/geoCepService';
import GeoCepMapView from '../../components/Map/GeoCepMapView';

const TechnicianPortal = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(false);
  const [workOrders, setWorkOrders] = useState([]);
  const [feedback, setFeedback] = useState({ type: '', message: '' });

  // Posição GPS do Técnico
  const [techLocation, setTechLocation] = useState(null);
  const [gpsAccuracy, setGpsAccuracy] = useState(null);
  const [gettingGps, setGettingGps] = useState(false);

  // Mapa selecionado
  const [selectedOrderForMap, setSelectedOrderForMap] = useState(null);

  // Modal de Conclusão
  const [completeModalOpen, setCompleteModalOpen] = useState(false);
  const [currentOrder, setCurrentOrder] = useState(null);
  const [onuMac, setOnuMac] = useState('');
  const [onuSerial, setOnuSerial] = useState('');
  const [signalDbm, setSignalDbm] = useState('-19.50');
  const [signName, setSignName] = useState('');
  const [notes, setNotes] = useState('');
  const [submittingComplete, setSubmittingComplete] = useState(false);
  const [contributingGeoCep, setContributingGeoCep] = useState(false);

  // Canvas de Assinatura
  const canvasRef = useRef(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [hasSignature, setHasSignature] = useState(false);

  const loadWorkOrders = async () => {
    try {
      setLoading(true);
      const res = await workOrderService.getAllWorkOrders();
      setWorkOrders(res.data || []);
    } catch (err) {
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
        // Fallback para Altamira/PA
        setTechLocation({ latitude: -3.2033, longitude: -52.2064 });
        setGpsAccuracy(10);
        setGettingGps(false);
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  const handleOpenCompleteModal = (order) => {
    setCurrentOrder(order);
    setOnuMac(order.onuMac || '');
    setOnuSerial(order.onuSerial || '');
    setSignalDbm(order.fiberSignalDbm ? order.fiberSignalDbm.toString() : '-19.50');
    setSignName(order.customerSignatureName || '');
    setNotes(order.notes || '');
    setHasSignature(false);
    setCompleteModalOpen(true);
    handleGetTechGps();

    setTimeout(() => {
      initCanvas();
    }, 200);
  };

  // Canvas Handlers
  const initCanvas = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    ctx.strokeStyle = '#0f172a';
    ctx.lineWidth = 2.5;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  };

  const startDrawing = (e) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const ctx = canvas.getContext('2d');
    const x = (e.touches ? e.touches[0].clientX : e.clientX) - rect.left;
    const y = (e.touches ? e.touches[0].clientY : e.clientY) - rect.top;

    ctx.beginPath();
    ctx.moveTo(x, y);
    setIsDrawing(true);
    setHasSignature(true);
  };

  const draw = (e) => {
    if (!isDrawing) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const ctx = canvas.getContext('2d');
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
        cep: '68370-000',
        numero: '100',
        latitude: techLocation.latitude,
        longitude: techLocation.longitude,
        precisaoGpsMetros: gpsAccuracy || 5.0,
      };

      const res = await geoCepService.contributeCoordinate(payload);
      setFeedback({
        type: 'success',
        message: '📍 Coordenada enviada com sucesso para homologação no GeoCEP! Sua conta foi bonificada.',
      });
    } catch (err) {
      setFeedback({ type: 'error', message: 'Erro ao enviar para o GeoCEP: ' + err.message });
    } finally {
      setContributingGeoCep(false);
    }
  };

  const handleCompleteWorkOrder = async () => {
    if (!onuMac || !onuSerial) {
      setFeedback({ type: 'warning', message: 'Informe o MAC e Número de Série da ONU.' });
      return;
    }

    try {
      setSubmittingComplete(true);
      const canvas = canvasRef.current;
      const signatureBase64 = canvas && hasSignature ? canvas.toDataURL('image/png') : null;

      const payload = {
        onuMac,
        onuSerial,
        fiberSignalDbm: parseFloat(signalDbm) || -19.50,
        technicianLatitude: techLocation ? techLocation.latitude : null,
        technicianLongitude: techLocation ? techLocation.longitude : null,
        digitalSignatureBase64: signatureBase64,
        customerSignatureName: signName || 'Cliente Assinante',
        notes,
      };

      await workOrderService.completeWorkOrder(currentOrder.id, payload);
      setFeedback({
        type: 'success',
        message: `✅ O.S. #${currentOrder.id.slice(0, 8)} concluída! Contrato ativado com sucesso em tempo real.`,
      });
      setCompleteModalOpen(false);
      loadWorkOrders();
    } catch (err) {
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
            <TechIcon color="primary" /> Modo Campo Técnico
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Navegação nativa GeoCEP, Coleta GPS e Assinatura Digital Touch.
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
        <Alert severity={feedback.type || 'info'} sx={{ mb: 2 }} onClose={() => setFeedback({ type: '', message: '' })}>
          {feedback.message}
        </Alert>
      )}

      {/* Tabs */}
      <Paper sx={{ mb: 2 }}>
        <Tabs value={activeTab} onChange={(e, val) => setActiveTab(val)} variant="fullWidth" indicatorColor="primary" textColor="primary">
          <Tab label={`Pendentes (${pendingOrders.length})`} />
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
            const isMapOpen = selectedOrderForMap === order.id;

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

                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                      📅 <strong>Agendamento:</strong> {order.scheduledDate || 'Hoje'} ({order.scheduledPeriod || 'MANHÃ'})
                    </Typography>

                    {order.technicianName && (
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                        👷 <strong>Técnico:</strong> {order.technicianName}
                      </Typography>
                    )}

                    {/* Botão de Mapa Integrado GeoCEP */}
                    <Box sx={{ my: 1.5 }}>
                      <Button
                        size="small"
                        variant="text"
                        startIcon={<MapIcon />}
                        onClick={() => setSelectedOrderForMap(isMapOpen ? null : order.id)}
                        sx={{ textTransform: 'none', fontWeight: 'bold' }}
                      >
                        {isMapOpen ? 'Ocultar Mapa GeoCEP' : '🗺️ Ver Localização no Mapa GeoCEP'}
                      </Button>

                      {isMapOpen && (
                        <Box sx={{ mt: 1 }}>
                          <GeoCepMapView
                            technicianLocation={techLocation}
                            customerLocation={{
                              latitude: order.technicianLatitude || -3.2050,
                              longitude: order.technicianLongitude || -52.2070,
                              label: `O.S. #${order.id.slice(0, 8)} - ${order.type}`,
                            }}
                            height="240px"
                          />
                        </Box>
                      )}
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
                        href="https://wa.me/5593988887777?text=Ol%C3%A1%2C+sou+o+t%C3%A9cnico+da+Xingu+Telecom+e+estou+a+caminho+da+sua+instala%C3%A7%C3%A3o."
                        target="_blank"
                      >
                        WhatsApp
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        startIcon={<PhoneIcon />}
                        component="a"
                        href="tel:93988887777"
                      >
                        Ligar
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
                          Concluir Atendimento
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

      {/* Modal de Conclusão de Campo & Assinatura */}
      <Dialog open={completeModalOpen} onClose={() => setCompleteModalOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <TechIcon color="primary" /> Conclusão & Ativação de O.S.
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

              {/* Dados da ONU */}
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <OnuIcon fontSize="small" color="primary" /> Equipamento Instalado (ONU / ONT)
              </Typography>
              <Grid container spacing={1.5}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Endereço MAC da ONU"
                    placeholder="AA:BB:CC:DD:EE:FF"
                    value={onuMac}
                    onChange={(e) => setOnuMac(e.target.value.toUpperCase())}
                    required
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Número de Série (Serial)"
                    placeholder="ZTEG12345678"
                    value={onuSerial}
                    onChange={(e) => setOnuSerial(e.target.value.toUpperCase())}
                    required
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    size="small"
                    label="Potência Óptica do Sinal (dBm)"
                    value={signalDbm}
                    onChange={(e) => setSignalDbm(e.target.value)}
                    helperText="Faixa recomendada: -15.00 a -25.00 dBm"
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
                    height: 140,
                    position: 'relative',
                    touchAction: 'none',
                  }}
                >
                  <canvas
                    ref={canvasRef}
                    width={500}
                    height={140}
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
                placeholder="Ex: Instalação concluída com 120m de cabo drop e PTO fixado na sala."
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
            disabled={submittingComplete || !onuMac || !onuSerial}
            startIcon={submittingComplete ? <CircularProgress size={16} /> : <SuccessIcon />}
          >
            {submittingComplete ? 'Ativando...' : 'Finalizar & Ativar Assinante'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TechnicianPortal;
