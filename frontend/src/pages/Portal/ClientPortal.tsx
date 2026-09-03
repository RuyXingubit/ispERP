import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Button,
  Chip,
  Tabs,
  Tab,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
  CircularProgress,
  IconButton,
  Tooltip,
  Paper,
  Divider,
  Snackbar,
  MenuItem
} from '@mui/material';
import {
  Speed as SpeedIcon,
  Payment as PaymentIcon,
  CheckCircle as CheckCircleIcon,
  Warning as WarningIcon,
  ContentCopy as ContentCopyIcon,
  Upgrade as UpgradeIcon,
  Person as PersonIcon,
  Lock as LockIcon,
  QrCode as QrCodeIcon,
  SignalCellularAlt as SignalIcon,
  History as HistoryIcon,
  Key as KeyIcon,
  Receipt as ReceiptIcon,
  HeadsetMic as SupportIcon,
  SwitchAccount as SwitchAccountIcon,
  Logout as LogoutIcon,
  VpnKey as VpnKeyIcon,
  Badge as BadgeIcon,
} from '@mui/icons-material';
import clientPortalService from '../../services/clientPortalService';
import { helpdeskService } from '../../services/helpdeskService';
import { customerService } from '../../services/customerService';
import { useAuth } from '../../contexts/AuthContext';

function TabPanel(props) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );
}

const ClientPortal = () => {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryCustomerId = searchParams.get('customerId');

  // Sessão persistente do cliente no navegador
  const [clientSession, setClientSession] = useState<any>(() => {
    try {
      const saved = sessionStorage.getItem('isperp_client_portal_session');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const isOperator = user?.role === 'ADMIN' || user?.role === 'SUPPORT_ANALYST' || user?.role === 'ATTENDANT';
  const effectiveCustomerId = (isOperator && queryCustomerId) ? queryCustomerId : clientSession?.customerId;

  // Estados do formulário de autenticação do cliente
  const [authStep, setAuthStep] = useState<'DOCUMENT' | 'PIN'>('DOCUMENT');
  const [loginDoc, setLoginDoc] = useState('');
  const [loginPin, setLoginPin] = useState('');
  const [loginLoading, setLoginLoading] = useState(false);
  const [loginError, setLoginError] = useState('');
  const [loginCustomerInfo, setLoginCustomerInfo] = useState<any>(null);

  // Estados do Modal de PIN de 4 dígitos
  const [pinModalOpen, setPinModalOpen] = useState(false);
  const [currentPin, setCurrentPin] = useState('');
  const [newPin, setNewPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [pinSaving, setPinSaving] = useState(false);

  const [loading, setLoading] = useState(false);
  const [dashboard, setDashboard] = useState<any>(null);
  const [activeTab, setActiveTab] = useState(0);
  const [invoiceSubTab, setInvoiceSubTab] = useState(0);

  // Lista de clientes para simulação (somente se operador do ERP logado)
  const [allCustomers, setAllCustomers] = useState<any[]>([]);

  // Modals
  const [pixModalOpen, setPixModalOpen] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState<any>(null);
  const [upgradeModalOpen, setUpgradeModalOpen] = useState(false);
  const [selectedUpgradePlan, setSelectedUpgradePlan] = useState<any>(null);

  // Tickets State
  const [myTickets, setMyTickets] = useState<any[]>([]);
  const [ticketModalOpen, setTicketModalOpen] = useState(false);
  const [newTicketCategory, setNewTicketCategory] = useState('SLOW_SPEED');
  const [newTicketSubject, setNewTicketSubject] = useState('');
  const [newTicketDesc, setNewTicketDesc] = useState('');
  const [ticketSubmitting, setTicketSubmitting] = useState(false);

  // Profile & Password State
  const [profileForm, setProfileForm] = useState({
    name: '',
    email: '',
    phone: '',
    address: '',
    city: '',
    state: '',
    zipCode: ''
  });
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  // Snackbars & Feedback
  const [toast, setToast] = useState({ open: false, message: '', severity: 'success' });

  const loadDashboard = async (targetCustomerId = effectiveCustomerId) => {
    if (!targetCustomerId) {
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      const data = await clientPortalService.getDashboard(targetCustomerId);
      setDashboard(data);
      if (data?.customer) {
        setProfileForm({
          name: data.customer.name || '',
          email: data.customer.email || '',
          phone: data.customer.phone || '',
          address: data.customer.address || '',
          city: data.customer.city || '',
          state: data.customer.state || '',
          zipCode: data.customer.zipCode || ''
        });
      }
    } catch (err: any) {
      console.error('Erro ao carregar Central do Assinante:', err);
      // Se não autorizado, limpa sessão
      if (err.response?.status === 401 || err.response?.status === 403) {
        handleClientLogout();
      }
      setToast({
        open: true,
        message: 'Erro ao carregar dados da Central do Assinante',
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };

  const loadAllCustomers = async () => {
    if (!isOperator) return;
    try {
      const list: any = await customerService.getAll();
      setAllCustomers(list || []);
    } catch (err) {
      console.error('Erro ao listar clientes:', err);
    }
  };

  const loadMyTickets = async (customerId: string) => {
    if (!customerId) return;
    try {
      const res: any = await helpdeskService.getTicketsByCustomer(customerId);
      setMyTickets(Array.isArray(res) ? res : (res?.data || []));
    } catch (err) {
      console.error('Erro ao carregar chamados do cliente:', err);
    }
  };

  // Carrega ao mudar o ID efetivo
  useEffect(() => {
    if (effectiveCustomerId) {
      loadDashboard(effectiveCustomerId);
      if (isOperator) {
        loadAllCustomers();
      }
    }
  }, [effectiveCustomerId]);

  useEffect(() => {
    if (dashboard?.customer?.id) {
      loadMyTickets(dashboard.customer.id);
    }
  }, [dashboard]);

  // Handler de login do cliente por CPF/CNPJ e PIN
  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError('');

    if (!loginDoc.trim()) {
      setLoginError('Informe o seu CPF ou CNPJ.');
      return;
    }

    try {
      setLoginLoading(true);
      const res: any = await clientPortalService.authenticate(
        loginDoc.trim(),
        authStep === 'PIN' ? loginPin.trim() : undefined
      );

      if (res.status === 'PIN_REQUIRED') {
        setLoginCustomerInfo(res);
        setAuthStep('PIN');
        setLoginLoading(false);
        return;
      }

      if (res.status === 'AUTHENTICATED') {
        const session = {
          customerId: res.customerId,
          customerName: res.customerName,
          hasPin: res.hasPin,
          document: res.maskedDocument
        };
        sessionStorage.setItem('isperp_client_portal_session', JSON.stringify(session));
        setClientSession(session);
        setToast({
          open: true,
          message: `Bem-vindo(a), ${res.customerName}!`,
          severity: 'success'
        });
      }
    } catch (err: any) {
      setLoginError(err.response?.data?.message || err.message || 'Erro ao validar documento.');
    } finally {
      setLoginLoading(false);
    }
  };

  const handleClientLogout = () => {
    sessionStorage.removeItem('isperp_client_portal_session');
    setClientSession(null);
    setDashboard(null);
    setAuthStep('DOCUMENT');
    setLoginDoc('');
    setLoginPin('');
    setLoginError('');
    if (queryCustomerId && !isOperator) {
      setSearchParams({});
    }
  };

  const handleSavePin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPin || newPin.length !== 4 || !/^\d{4}$/.test(newPin)) {
      setToast({ open: true, message: 'O PIN deve conter exatamente 4 dígitos numéricos.', severity: 'warning' });
      return;
    }
    if (newPin !== confirmPin) {
      setToast({ open: true, message: 'A confirmação do novo PIN não confere.', severity: 'warning' });
      return;
    }

    try {
      setPinSaving(true);
      await clientPortalService.setPin(effectiveCustomerId, newPin, currentPin || undefined);
      setToast({ open: true, message: 'PIN de 4 dígitos configurado com sucesso!', severity: 'success' });
      setPinModalOpen(false);
      setNewPin('');
      setConfirmPin('');
      setCurrentPin('');
      if (clientSession) {
        const updated = { ...clientSession, hasPin: true };
        sessionStorage.setItem('isperp_client_portal_session', JSON.stringify(updated));
        setClientSession(updated);
      }
    } catch (err: any) {
      setToast({ open: true, message: err.response?.data?.message || 'Erro ao configurar PIN.', severity: 'error' });
    } finally {
      setPinSaving(false);
    }
  };

  const handleCustomerSwitch = (newCustId) => {
    if (newCustId) {
      setSearchParams({ customerId: newCustId });
    } else {
      setSearchParams({});
    }
  };

  const handleCreateMyTicket = async () => {
    if (!newTicketSubject.trim() || !newTicketDesc.trim()) {
      setToast({ open: true, message: 'Preencha o assunto e a descrição do chamado.', severity: 'warning' });
      return;
    }
    try {
      setTicketSubmitting(true);
      const res = await helpdeskService.createTicket({
        customerId: dashboard?.customer?.id,
        contractId: dashboard?.contract?.id || null,
        category: newTicketCategory,
        channel: 'PORTAL',
        subject: newTicketSubject.trim(),
        description: newTicketDesc.trim(),
      });
      setToast({ open: true, message: `Chamado aberto com sucesso! Protocolo ANATEL: ${res?.protocol || 'Registrado'}`, severity: 'success' });
      setTicketModalOpen(false);
      setNewTicketSubject('');
      setNewTicketDesc('');
      loadMyTickets(dashboard?.customer?.id);
    } catch (err) {
      setToast({ open: true, message: 'Erro ao abrir chamado.', severity: 'error' });
    } finally {
      setTicketSubmitting(false);
    }
  };

  const handleCopyPix = (pixCode) => {
    if (pixCode) {
      navigator.clipboard.writeText(pixCode);
      setToast({
        open: true,
        message: 'Código Pix Copia e Cola copiado com sucesso!',
        severity: 'success'
      });
    }
  };

  const handleOpenPixModal = (invoice) => {
    setSelectedInvoice(invoice);
    setPixModalOpen(true);
  };

  const handleExecuteUpgrade = async () => {
    if (!selectedUpgradePlan || !dashboard?.contract) return;
    try {
      await clientPortalService.upgradePlan(dashboard.contract.id, selectedUpgradePlan.id, dashboard?.customer?.id);
      setToast({
        open: true,
        message: `Upgrade para o plano ${selectedUpgradePlan.name} realizado com sucesso!`,
        severity: 'success'
      });
      setUpgradeModalOpen(false);
      loadDashboard(dashboard?.customer?.id);
    } catch (err) {
      setToast({
        open: true,
        message: 'Erro ao solicitar upgrade de plano: ' + (err.response?.data?.message || err.message),
        severity: 'error'
      });
    }
  };

  const handleTrustUnblock = async () => {
    if (!dashboard?.contract) return;
    try {
      await clientPortalService.requestTrustUnblock(dashboard.contract.id, dashboard?.customer?.id);
      setToast({
        open: true,
        message: 'Desbloqueio em Confiança concedido por 48 horas! Sua conexão foi reativada.',
        severity: 'success'
      });
      loadDashboard(dashboard?.customer?.id);
    } catch (err) {
      setToast({
        open: true,
        message: 'Erro ao solicitar desbloqueio em confiança: ' + (err.response?.data?.message || err.message),
        severity: 'error'
      });
    }
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    try {
      await clientPortalService.updateProfile(profileForm, dashboard?.customer?.id);
      setToast({
        open: true,
        message: 'Dados cadastrais atualizados com sucesso!',
        severity: 'success'
      });
      loadDashboard(dashboard?.customer?.id);
    } catch (err) {
      setToast({
        open: true,
        message: 'Erro ao atualizar dados cadastrais',
        severity: 'error'
      });
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setToast({
        open: true,
        message: 'A nova senha e a confirmação não conferem',
        severity: 'warning'
      });
      return;
    }
    try {
      await clientPortalService.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword
      }, dashboard?.customer?.id);
      setToast({
        open: true,
        message: 'Senha alterada com sucesso!',
        severity: 'success'
      });
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setToast({
        open: true,
        message: 'Erro ao alterar senha: ' + (err.response?.data?.message || 'Verifique a senha atual'),
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

  // Se o cliente não estiver autenticado (sem sessão e sem impersonate de operador), exibe tela de login do assinante
  if (!effectiveCustomerId) {
    return (
      <Box
        sx={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: '#f4f6f8',
          p: 2,
        }}
      >
        <Card sx={{ maxWidth: 460, width: '100%', borderRadius: 3, boxShadow: 4, overflow: 'hidden' }}>
          <Box
            sx={{
              p: 3,
              background: 'linear-gradient(135deg, #0d47a1 0%, #1976d2 100%)',
              color: '#fff',
              textAlign: 'center',
            }}
          >
            <Typography variant="h5" fontWeight="bold">
              Central do Assinante
            </Typography>
            <Typography variant="body2" sx={{ opacity: 0.9, mt: 0.5 }}>
              Nexus Fibra Telecom • Autoatendimento
            </Typography>
          </Box>

          <CardContent sx={{ p: { xs: 3, sm: 4 } }}>
            {loginError && (
              <Alert severity="error" sx={{ mb: 3 }}>
                {loginError}
              </Alert>
            )}

            <form onSubmit={handleLoginSubmit}>
              {authStep === 'DOCUMENT' ? (
                <Box>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Informe o <strong>CPF</strong> ou <strong>CNPJ</strong> do titular para consultar faturas, código Pix e serviços.
                  </Typography>

                  <TextField
                    fullWidth
                    label="CPF ou CNPJ do Titular"
                    variant="outlined"
                    value={loginDoc}
                    onChange={(e) => setLoginDoc(e.target.value)}
                    placeholder="000.000.000-00 ou 00.000.000/0000-00"
                    autoFocus
                    required
                    sx={{ mb: 3 }}
                  />

                  <Button
                    fullWidth
                    type="submit"
                    variant="contained"
                    size="large"
                    disabled={loginLoading}
                    sx={{
                      py: 1.5,
                      fontWeight: 'bold',
                      borderRadius: 2,
                      background: 'linear-gradient(135deg, #0d47a1 0%, #1976d2 100%)',
                    }}
                  >
                    {loginLoading ? <CircularProgress size={24} color="inherit" /> : 'Acessar Central'}
                  </Button>
                </Box>
              ) : (
                <Box>
                  <Alert severity="info" sx={{ mb: 2 }}>
                    Olá, <strong>{loginCustomerInfo?.customerName}</strong>! ({loginCustomerInfo?.maskedDocument})
                  </Alert>

                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Digite seu <strong>PIN de 4 dígitos</strong> para desbloquear o acesso às suas faturas:
                  </Typography>

                  <TextField
                    fullWidth
                    label="PIN de 4 Dígitos"
                    type="password"
                    inputProps={{ maxLength: 4, inputMode: 'numeric', pattern: '[0-9]*' }}
                    value={loginPin}
                    onChange={(e) => setLoginPin(e.target.value.replace(/\D/g, ''))}
                    placeholder="••••"
                    autoFocus
                    required
                    sx={{ mb: 3, input: { letterSpacing: 8, fontSize: '1.5rem', textAlign: 'center' } }}
                  />

                  <Button
                    fullWidth
                    type="submit"
                    variant="contained"
                    size="large"
                    disabled={loginLoading}
                    sx={{
                      py: 1.5,
                      fontWeight: 'bold',
                      borderRadius: 2,
                      background: 'linear-gradient(135deg, #0d47a1 0%, #1976d2 100%)',
                      mb: 1.5,
                    }}
                  >
                    {loginLoading ? <CircularProgress size={24} color="inherit" /> : 'Confirmar e Entrar'}
                  </Button>

                  <Button
                    fullWidth
                    variant="text"
                    color="inherit"
                    onClick={() => {
                      setAuthStep('DOCUMENT');
                      setLoginPin('');
                      setLoginError('');
                    }}
                    sx={{ textTransform: 'none' }}
                  >
                    Trocar CPF/CNPJ
                  </Button>
                </Box>
              )}
            </form>

            <Divider sx={{ my: 3 }} />

            <Box sx={{ textAlign: 'center' }}>
              <Typography variant="caption" color="text.secondary" display="block">
                Precisa de ajuda ou esqueceu seu PIN?
              </Typography>
              <Button
                size="small"
                variant="outlined"
                color="success"
                startIcon={<SupportIcon />}
                href="https://wa.me/559335152000"
                target="_blank"
                sx={{ mt: 1, textTransform: 'none', borderRadius: 2 }}
              >
                Falar com Atendente no WhatsApp
              </Button>
            </Box>
          </CardContent>
        </Card>
      </Box>
    );
  }

  const {
    customer,
    contract,
    currentPlan,
    availableUpgradePlans,
    pendingInvoices,
    paidInvoices,
    overdueInvoices,
    isConnectionBlocked,
    canRequestTrustUnblock,
    connectionStatusMessage
  } = dashboard || {};

  return (
    <Box sx={{ p: { xs: 2, md: 4 }, maxWidth: 1200, mx: 'auto' }}>
      {/* Barra de Simulação de Atendente / Operador */}
      {isOperator && allCustomers.length > 0 && (
        <Paper
          elevation={1}
          sx={{
            p: 1.5,
            mb: 2,
            borderRadius: 2,
            bgcolor: '#fff3e0',
            border: '1px solid #ffe082',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 1.5
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <SwitchAccountIcon color="warning" />
            <Typography variant="body2" fontWeight="bold">
              Visão do Atendente • Simulando Portal de:
            </Typography>
          </Box>
          <TextField
            select
            size="small"
            value={customer?.id || ''}
            onChange={(e) => handleCustomerSwitch(e.target.value)}
            sx={{ minWidth: 320, bgcolor: '#fff', borderRadius: 1 }}
          >
            {allCustomers.map((c) => (
              <MenuItem key={c.id} value={c.id}>
                {c.name} - CPF/CNPJ: {c.cpf}
              </MenuItem>
            ))}
          </TextField>
        </Paper>
      )}

      {/* Header Central do Assinante */}
      <Paper
        elevation={0}
        sx={{
          p: 3,
          mb: 3,
          borderRadius: 3,
          background: 'linear-gradient(135deg, #0d47a1 0%, #1976d2 100%)',
          color: '#fff'
        }}
      >
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={8}>
            <Typography variant="overline" sx={{ letterSpacing: 1.5, opacity: 0.9 }}>
              Central do Assinante • Autoatendimento
            </Typography>
            <Typography variant="h4" fontWeight="bold">
              Olá, {customer?.name || 'Assinante'} 👋
            </Typography>
            <Typography variant="body2" sx={{ opacity: 0.9, mt: 0.5 }}>
              Contrato: <strong>{contract?.contractNumber || 'N/A'}</strong> | CPF: {customer?.cpf}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={4} sx={{ textAlign: { xs: 'left', sm: 'right' } }}>
            <Box sx={{ display: 'flex', gap: 1, justifyContent: { xs: 'flex-start', sm: 'flex-end' }, alignItems: 'center', flexWrap: 'wrap' }}>
              <Chip
                icon={isConnectionBlocked ? <WarningIcon /> : <CheckCircleIcon />}
                label={isConnectionBlocked ? 'Conexão Suspensa' : 'Conexão Ativa'}
                color={isConnectionBlocked ? 'error' : 'success'}
                sx={{ fontWeight: 'bold', fontSize: '0.9rem', py: 2, px: 1 }}
              />
              <Tooltip title={clientSession?.hasPin ? "Alterar PIN de 4 dígitos" : "Cadastrar PIN de 4 dígitos"}>
                <IconButton 
                  color="inherit" 
                  onClick={() => setPinModalOpen(true)}
                  sx={{ bgcolor: 'rgba(255,255,255,0.15)', '&:hover': { bgcolor: 'rgba(255,255,255,0.25)' } }}
                >
                  <VpnKeyIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="Encerrar Sessão">
                <IconButton 
                  color="inherit" 
                  onClick={handleClientLogout}
                  sx={{ bgcolor: 'rgba(255,255,255,0.15)', '&:hover': { bgcolor: 'rgba(255,255,255,0.25)' } }}
                >
                  <LogoutIcon />
                </IconButton>
              </Tooltip>
            </Box>
          </Grid>
        </Grid>
      </Paper>

      {/* Banner de Desbloqueio em Confiança (Promessa de Pagamento 48h) */}
      {isConnectionBlocked && (
        <Alert
          severity="error"
          variant="filled"
          sx={{ mb: 3, borderRadius: 2 }}
          action={
            canRequestTrustUnblock ? (
              <Button
                color="inherit"
                size="small"
                variant="outlined"
                onClick={handleTrustUnblock}
                sx={{ fontWeight: 'bold', bgcolor: 'rgba(255,255,255,0.2)' }}
              >
                Desbloqueio em Confiança (48h)
              </Button>
            ) : null
          }
        >
          {connectionStatusMessage || 'Sua conexão está suspensa por pendência financeira.'}
          {canRequestTrustUnblock && ' Você pode solicitar o restabelecimento do sinal por 48h enquanto realiza o pagamento.'}
        </Alert>
      )}

      {/* Navegação por Abas Principais */}
      <Paper elevation={1} sx={{ borderRadius: 3, mb: 3 }}>
        <Tabs
          value={activeTab}
          onChange={(e, val) => setActiveTab(val)}
          variant="fullWidth"
          indicatorColor="primary"
          textColor="primary"
        >
          <Tab icon={<SpeedIcon />} label="Meu Plano & Conexão" iconPosition="start" />
          <Tab
            icon={<PaymentIcon />}
            label={`Faturas & Pix (${(pendingInvoices?.length || 0) + (overdueInvoices?.length || 0)})`}
            iconPosition="start"
          />
          <Tab icon={<PersonIcon />} label="Meus Dados & Segurança" iconPosition="start" />
          <Tab icon={<SupportIcon />} label="Suporte & Chamados (ANATEL)" iconPosition="start" />
        </Tabs>
      </Paper>

      {/* ABA 1: Meu Plano & Conexão */}
      <TabPanel value={activeTab} index={0}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={7}>
            <Card sx={{ borderRadius: 3, height: '100%' }}>
              <CardContent sx={{ p: 3 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6" fontWeight="bold">
                    Plano Contratado
                  </Typography>
                  <Chip label="Fibra Óptica" color="primary" variant="outlined" size="small" />
                </Box>

                <Typography variant="h3" fontWeight="bold" color="primary.main" gutterBottom>
                  {currentPlan?.name || 'Plano Fibra Turbo'}
                </Typography>

                <Grid container spacing={2} sx={{ my: 2 }}>
                  <Grid item xs={6}>
                    <Paper variant="outlined" sx={{ p: 2, textAlign: 'center', borderRadius: 2 }}>
                      <Typography variant="body2" color="text.secondary">
                        Velocidade Download
                      </Typography>
                      <Typography variant="h5" fontWeight="bold" color="primary.main">
                        {currentPlan?.downloadSpeed || 500} Mbps
                      </Typography>
                    </Paper>
                  </Grid>
                  <Grid item xs={6}>
                    <Paper variant="outlined" sx={{ p: 2, textAlign: 'center', borderRadius: 2 }}>
                      <Typography variant="body2" color="text.secondary">
                        Velocidade Upload
                      </Typography>
                      <Typography variant="h5" fontWeight="bold" color="secondary.main">
                        {currentPlan?.uploadSpeed || 250} Mbps
                      </Typography>
                    </Paper>
                  </Grid>
                </Grid>

                <Divider sx={{ my: 2 }} />

                <Grid container spacing={1} sx={{ mt: 1 }}>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">
                      Valor Mensalidade:
                    </Typography>
                    <Typography variant="subtitle1" fontWeight="bold">
                      R$ {contract?.monthlyFee ? Number(contract.monthlyFee).toFixed(2).replace('.', ',') : (currentPlan?.price ? Number(currentPlan.price).toFixed(2).replace('.', ',') : '99,90')}
                    </Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">
                      Dia de Vencimento:
                    </Typography>
                    <Typography variant="subtitle1" fontWeight="bold">
                      Todo dia {contract?.dueDay || 10}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} sx={{ mt: 1 }}>
                    <Typography variant="body2" color="text.secondary">
                      Endereço de Instalação:
                    </Typography>
                    <Typography variant="body2">
                      {contract?.installationAddress || customer?.address || 'Endereço principal'}, {contract?.city || customer?.city || 'São Paulo'} - {contract?.state || customer?.state || 'SP'}
                    </Typography>
                  </Grid>
                </Grid>

                <Button
                  variant="contained"
                  color="secondary"
                  fullWidth
                  size="large"
                  startIcon={<UpgradeIcon />}
                  onClick={() => setUpgradeModalOpen(true)}
                  sx={{ mt: 3, borderRadius: 2, py: 1.5, fontWeight: 'bold' }}
                >
                  Fazer Upgrade de Velocidade
                </Button>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={5}>
            <Card sx={{ borderRadius: 3, height: '100%' }}>
              <CardContent sx={{ p: 3 }}>
                <Typography variant="h6" fontWeight="bold" gutterBottom>
                  Status da Conexão em Tempo Real
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, my: 3 }}>
                  <SignalIcon color="success" sx={{ fontSize: 48 }} />
                  <Box>
                    <Typography variant="subtitle1" fontWeight="bold">
                      Sinal Óptico Excelente
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      ONT/ONU sincronizada na OLT (-19.4 dBm)
                    </Typography>
                  </Box>
                </Box>

                <Paper sx={{ p: 2, bgcolor: 'background.default', borderRadius: 2, mb: 2 }}>
                  <Typography variant="caption" color="text.secondary" display="block">
                    IP PÚBLICO / CGNAT
                  </Typography>
                  <Typography variant="body2" fontWeight="bold">
                    177.136.240.58
                  </Typography>
                  <Divider sx={{ my: 1 }} />
                  <Typography variant="caption" color="text.secondary" display="block">
                    DNS PRIMÁRIO
                  </Typography>
                  <Typography variant="body2" fontWeight="bold">
                    1.1.1.1 (Cloudflare Ultra-fast)
                  </Typography>
                </Paper>

                <Alert severity="info" sx={{ borderRadius: 2 }}>
                  Dúvidas com seu sinal? Reinicie seu roteador Wi-Fi ou contate nosso suporte via WhatsApp.
                </Alert>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ABA 2: Faturas & Pix */}
      <TabPanel value={activeTab} index={1}>
        <Paper elevation={0} variant="outlined" sx={{ p: 1, borderRadius: 3, mb: 3 }}>
          <Tabs
            value={invoiceSubTab}
            onChange={(e, val) => setInvoiceSubTab(val)}
            indicatorColor="primary"
            textColor="primary"
          >
            <Tab label={`Abertas / A Vencer (${pendingInvoices?.length || 0})`} />
            <Tab label={`Vencidas (${overdueInvoices?.length || 0})`} />
            <Tab label={`Histórico de Pagas (${paidInvoices?.length || 0})`} />
          </Tabs>
        </Paper>

        {/* Sub-aba 0: Faturas Abertas */}
        {invoiceSubTab === 0 && (
          <Grid container spacing={2}>
            {pendingInvoices?.length === 0 ? (
              <Grid item xs={12}>
                <Alert severity="success" sx={{ borderRadius: 2 }}>
                  Parabéns! Você não possui nenhuma fatura a vencer no momento.
                </Alert>
              </Grid>
            ) : (
              pendingInvoices?.map((inv) => (
                <Grid item xs={12} md={6} key={inv.id}>
                  <Card sx={{ borderRadius: 3, borderLeft: '6px solid #1976d2' }}>
                    <CardContent sx={{ p: 3 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                        <Typography variant="subtitle1" fontWeight="bold">
                          Fatura Mensalidade
                        </Typography>
                        <Chip label="A Vencer" color="primary" size="small" />
                      </Box>

                      <Typography variant="h4" fontWeight="bold" color="primary.main" sx={{ my: 1 }}>
                        R$ {Number(inv.amount).toFixed(2).replace('.', ',')}
                      </Typography>

                      <Typography variant="body2" color="text.secondary">
                        Vencimento: <strong>{new Date(inv.dueDate).toLocaleDateString('pt-BR')}</strong>
                      </Typography>

                      {inv.rebalanceNotice && (
                        <Alert severity="info" sx={{ mt: 1.5, borderRadius: 2, fontSize: '0.8rem' }}>
                          ℹ️ {inv.rebalanceNotice}
                        </Alert>
                      )}

                      <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
                        <Button
                          variant="contained"
                          color="success"
                          fullWidth
                          startIcon={<QrCodeIcon />}
                          onClick={() => handleOpenPixModal(inv)}
                          sx={{ borderRadius: 2, fontWeight: 'bold' }}
                        >
                          Pagar com Pix
                        </Button>
                        <Tooltip title="Copiar código Pix Copia e Cola">
                          <IconButton
                            color="primary"
                            onClick={() => handleCopyPix(inv.pixCopiaECola || '00020126580014br.gov.bcb.pix...')}
                          >
                            <ContentCopyIcon />
                          </IconButton>
                        </Tooltip>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              ))
            )}
          </Grid>
        )}

        {/* Sub-aba 1: Faturas Vencidas */}
        {invoiceSubTab === 1 && (
          <Grid container spacing={2}>
            {overdueInvoices?.length === 0 ? (
              <Grid item xs={12}>
                <Alert severity="success" sx={{ borderRadius: 2 }}>
                  Você não possui nenhuma fatura em atraso.
                </Alert>
              </Grid>
            ) : (
              overdueInvoices?.map((inv) => (
                <Grid item xs={12} md={6} key={inv.id}>
                  <Card sx={{ borderRadius: 3, borderLeft: '6px solid #d32f2f' }}>
                    <CardContent sx={{ p: 3 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                        <Typography variant="subtitle1" fontWeight="bold" color="error">
                          Fatura Vencida
                        </Typography>
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          {inv.protectedAgainstSuspension && (
                            <Chip label="Protegido contra Corte" color="info" size="small" />
                          )}
                          <Chip label="Atrasada" color="error" size="small" />
                        </Box>
                      </Box>

                      <Typography variant="h4" fontWeight="bold" color="error" sx={{ my: 1 }}>
                        R$ {Number(inv.amount).toFixed(2).replace('.', ',')}
                      </Typography>

                      <Typography variant="body2" color="error">
                        Venceu em: <strong>{new Date(inv.dueDate).toLocaleDateString('pt-BR')}</strong>
                      </Typography>

                      {inv.protectedAgainstSuspension && (
                        <Alert severity="success" sx={{ mt: 1.5, borderRadius: 2, fontSize: '0.8rem' }}>
                          🛡️ <strong>Pagamento Identificado:</strong> Sua conexão está protegida contra bloqueios enquanto realizamos a compensação.
                        </Alert>
                      )}

                      <Button
                        variant="contained"
                        color="error"
                        fullWidth
                        startIcon={<QrCodeIcon />}
                        onClick={() => handleOpenPixModal(inv)}
                        sx={{ mt: 2, borderRadius: 2, fontWeight: 'bold' }}
                      >
                        Regularizar com Pix Instantâneo
                      </Button>
                    </CardContent>
                  </Card>
                </Grid>
              ))
            )}
          </Grid>
        )}

        {/* Sub-aba 2: Faturas Pagas */}
        {invoiceSubTab === 2 && (
          <Grid container spacing={2}>
            {paidInvoices?.length === 0 ? (
              <Grid item xs={12}>
                <Typography variant="body2" color="text.secondary">
                  Nenhum comprovante de pagamento registrado ainda.
                </Typography>
              </Grid>
            ) : (
              paidInvoices?.map((inv) => (
                <Grid item xs={12} md={6} key={inv.id}>
                  <Card sx={{ borderRadius: 3, borderLeft: '6px solid #2e7d32' }}>
                    <CardContent sx={{ p: 3 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                        <Typography variant="subtitle1" fontWeight="bold">
                          Fatura Liquidada
                        </Typography>
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          {inv.paidByCrossCreditId && (
                            <Chip label="Compensação Cruzada" color="primary" size="small" variant="outlined" />
                          )}
                          <Chip icon={<CheckCircleIcon />} label="Pago" color="success" size="small" />
                        </Box>
                      </Box>

                      <Typography variant="h5" fontWeight="bold" color="success.main" sx={{ my: 1 }}>
                        R$ {Number(inv.amount).toFixed(2).replace('.', ',')}
                      </Typography>

                      <Typography variant="body2" color="text.secondary">
                        Vencimento: {new Date(inv.dueDate).toLocaleDateString('pt-BR')}
                      </Typography>

                      {inv.rebalanceNotice && (
                        <Alert severity="success" sx={{ mt: 1.5, borderRadius: 2, fontSize: '0.8rem' }}>
                          🟢 {inv.rebalanceNotice}
                        </Alert>
                      )}
                      {inv.paidAt && (
                        <Typography variant="caption" color="text.secondary" display="block">
                          Pago em: {new Date(inv.paidAt).toLocaleDateString('pt-BR')}
                        </Typography>
                      )}

                      {inv.nfcomKey && (
                        <Box sx={{ mt: 2, pt: 1.5, borderTop: '1px dashed #e0e0e0' }}>
                          <Typography variant="caption" color="text.secondary" display="block">
                            Chave NFCom: <strong>{inv.nfcomKey}</strong>
                          </Typography>
                          <Button
                            variant="outlined"
                            color="primary"
                            size="small"
                            startIcon={<ReceiptIcon />}
                            href={inv.nfcomPdfUrl || `https://pay.xingubit.com.br/v1/nfcom/${inv.nfcomKey}/danfe-pdf`}
                            target="_blank"
                            sx={{ mt: 1, borderRadius: 1.5, fontWeight: 'bold' }}
                          >
                            Baixar NFCom (Modelo 62)
                          </Button>
                        </Box>
                      )}
                    </CardContent>
                  </Card>
                </Grid>
              ))
            )}
          </Grid>
        )}
      </TabPanel>

      {/* ABA 3: Meus Dados Cadastrais & Senha */}
      <TabPanel value={activeTab} index={2}>
        <Grid container spacing={3}>
          {/* Atualização Cadastral */}
          <Grid item xs={12} md={7}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent sx={{ p: 3 }}>
                <Typography variant="h6" fontWeight="bold" gutterBottom>
                  Atualizar Dados de Contato
                </Typography>
                <Box component="form" onSubmit={handleSaveProfile} sx={{ mt: 2 }}>
                  <Grid container spacing={2}>
                    <Grid item xs={12}>
                      <TextField
                        label="Nome Completo"
                        fullWidth
                        value={profileForm.name}
                        onChange={(e) => setProfileForm({ ...profileForm, name: e.target.value })}
                        required
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        label="E-mail"
                        fullWidth
                        type="email"
                        value={profileForm.email}
                        onChange={(e) => setProfileForm({ ...profileForm, email: e.target.value })}
                        required
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        label="Telefone / WhatsApp"
                        fullWidth
                        value={profileForm.phone}
                        onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                        required
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        label="Endereço Completo"
                        fullWidth
                        value={profileForm.address}
                        onChange={(e) => setProfileForm({ ...profileForm, address: e.target.value })}
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        label="Cidade"
                        fullWidth
                        value={profileForm.city}
                        onChange={(e) => setProfileForm({ ...profileForm, city: e.target.value })}
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        label="Estado (UF)"
                        fullWidth
                        value={profileForm.state}
                        onChange={(e) => setProfileForm({ ...profileForm, state: e.target.value })}
                      />
                    </Grid>
                  </Grid>

                  <Button
                    type="submit"
                    variant="contained"
                    color="primary"
                    sx={{ mt: 3, borderRadius: 2, px: 4, py: 1.2, fontWeight: 'bold' }}
                  >
                    Salvar Alterações
                  </Button>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          {/* Troca de Senha */}
          <Grid item xs={12} md={5}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent sx={{ p: 3 }}>
                <Typography variant="h6" fontWeight="bold" gutterBottom>
                  Alterar Senha de Acesso
                </Typography>
                <Box component="form" onSubmit={handleChangePassword} sx={{ mt: 2 }}>
                  <TextField
                    label="Senha Atual"
                    fullWidth
                    type="password"
                    value={passwordForm.currentPassword}
                    onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
                    required
                    sx={{ mb: 2 }}
                  />
                  <TextField
                    label="Nova Senha"
                    fullWidth
                    type="password"
                    value={passwordForm.newPassword}
                    onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                    required
                    sx={{ mb: 2 }}
                  />
                  <TextField
                    label="Confirmar Nova Senha"
                    fullWidth
                    type="password"
                    value={passwordForm.confirmPassword}
                    onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                    required
                    sx={{ mb: 2 }}
                  />
                  <Button
                    type="submit"
                    variant="contained"
                    color="secondary"
                    fullWidth
                    startIcon={<KeyIcon />}
                    sx={{ borderRadius: 2, py: 1.2, fontWeight: 'bold' }}
                  >
                    Atualizar Senha
                  </Button>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      {/* ABA 4: Suporte & Chamados (ANATEL) */}
      <TabPanel value={activeTab} index={3}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
          <Box>
            <Typography variant="h6" fontWeight="bold">
              Meus Atendimentos & Protocolos ANATEL
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Acompanhe suas solicitações de suporte e histórico de chamados com prazos regulatórios.
            </Typography>
          </Box>
          <Button
            variant="contained"
            color="primary"
            startIcon={<SupportIcon />}
            onClick={() => setTicketModalOpen(true)}
            sx={{ borderRadius: 2, fontWeight: 'bold' }}
          >
            Abrir Chamado
          </Button>
        </Box>

        <Grid container spacing={2}>
          {myTickets.length === 0 ? (
            <Grid item xs={12}>
              <Alert severity="info" sx={{ borderRadius: 2 }}>
                Você não possui nenhum chamado de atendimento registrado no momento.
              </Alert>
            </Grid>
          ) : (
            myTickets.map((t) => (
              <Grid item xs={12} md={6} key={t.id}>
                <Card sx={{ borderRadius: 3, borderLeft: '6px solid #1976d2' }}>
                  <CardContent sx={{ p: 2.5 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                      <Typography variant="subtitle1" fontWeight="bold" color="primary.main" sx={{ fontFamily: 'monospace' }}>
                        Protocolo: {t.protocol}
                      </Typography>
                      <Chip
                        label={t.status}
                        color={t.status === 'RESOLVED' || t.status === 'CLOSED' ? 'success' : 'primary'}
                        size="small"
                      />
                    </Box>
                    <Typography variant="subtitle2" fontWeight="bold">
                      {t.subject}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ my: 1 }}>
                      {t.description}
                    </Typography>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 1.5, pt: 1, borderTop: '1px solid #f0f0f0' }}>
                      <Typography variant="caption" color="text.secondary">
                        ⏱️ Prazo SLA: {new Date(t.slaDeadline).toLocaleString('pt-BR')}
                      </Typography>
                      {t.anatelSatisfactionRating && (
                        <Typography variant="caption" color="warning.main" fontWeight="bold">
                          ⭐ {t.anatelSatisfactionRating} / 5
                        </Typography>
                      )}
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            ))
          )}
        </Grid>
      </TabPanel>

      {/* MODAL NOVO CHAMADO CENTRAL DO ASSINANTE */}
      <Dialog
        open={ticketModalOpen}
        onClose={() => setTicketModalOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { borderRadius: 3, p: 2 } }}
      >
        <DialogTitle sx={{ fontWeight: 'bold' }}>
          Abrir Chamado de Suporte (Protocolo ANATEL)
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Sua solicitação gerará um número oficial de protocolo ANATEL com atendimento prioritário.
          </Typography>
          <TextField
            select
            fullWidth
            label="Tipo de Solicitação"
            value={newTicketCategory}
            onChange={(e) => setNewTicketCategory(e.target.value)}
            sx={{ mb: 2 }}
          >
            <MenuItem value="SLOW_SPEED">Lentidão na Conexão (SLA 48h)</MenuItem>
            <MenuItem value="CONNECTION_OUTAGE">Sem Sinal / Sem Internet (SLA 24h)</MenuItem>
            <MenuItem value="ROUTER_CONFIG">Configuração de Wi-Fi / Roteador (SLA 48h)</MenuItem>
            <MenuItem value="FINANCIAL">Dúvidas Financeiras / Pagamento (SLA 24h)</MenuItem>
            <MenuItem value="ADDRESS_CHANGE">Mudança de Endereço (SLA 72h)</MenuItem>
            <MenuItem value="OTHER">Outros Assuntos (SLA 48h)</MenuItem>
          </TextField>
          <TextField
            fullWidth
            label="Assunto Resumido"
            placeholder="Ex: Wi-Fi desconectando no quarto"
            value={newTicketSubject}
            onChange={(e) => setNewTicketSubject(e.target.value)}
            sx={{ mb: 2 }}
          />
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Descrição Detalhada do Problema"
            placeholder="Descreva o que está acontecendo com sua conexão..."
            value={newTicketDesc}
            onChange={(e) => setNewTicketDesc(e.target.value)}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setTicketModalOpen(false)} color="inherit">
            Cancelar
          </Button>
          <Button
            variant="contained"
            color="primary"
            disabled={ticketSubmitting || !newTicketSubject.trim()}
            onClick={handleCreateMyTicket}
            sx={{ fontWeight: 'bold', borderRadius: 2 }}
          >
            {ticketSubmitting ? 'Gerando Protocolo...' : 'Enviar Chamado'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* MODAL PIX COPIA E COLA & QR CODE (XINGUBIT PAY) */}
      <Dialog
        open={pixModalOpen}
        onClose={() => setPixModalOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { borderRadius: 3, p: 2 } }}
      >
        <DialogTitle sx={{ textAlign: 'center', fontWeight: 'bold', fontSize: '1.4rem' }}>
          Pagamento Instantâneo com Pix
        </DialogTitle>
        <DialogContent sx={{ textAlign: 'center' }}>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Escaneie o QR Code abaixo com o app do seu banco para confirmação em tempo real.
          </Typography>

          <Box
            sx={{
              display: 'flex',
              justifyContent: 'center',
              my: 2,
              p: 2,
              bgcolor: '#fff',
              borderRadius: 2,
              border: '1px solid #e0e0e0',
              maxWidth: 240,
              mx: 'auto'
            }}
          >
            <img
              src={`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(
                selectedInvoice?.pixCopiaECola || '00020126580014br.gov.bcb.pix...'
              )}`}
              alt="QR Code Pix"
              style={{ width: '100%', height: 'auto' }}
            />
          </Box>

          <Typography variant="h5" fontWeight="bold" color="success.main" sx={{ my: 1 }}>
            R$ {selectedInvoice?.amount ? Number(selectedInvoice.amount).toFixed(2).replace('.', ',') : '0,00'}
          </Typography>

          <Paper variant="outlined" sx={{ p: 2, mt: 2, borderRadius: 2, bgcolor: 'background.default' }}>
            <Typography variant="caption" color="text.secondary" display="block" align="left">
              Código Pix Copia e Cola:
            </Typography>
            <Typography
              variant="body2"
              sx={{
                wordBreak: 'break-all',
                fontFamily: 'monospace',
                bgcolor: 'rgba(0,0,0,0.04)',
                p: 1,
                borderRadius: 1,
                my: 1
              }}
            >
              {selectedInvoice?.pixCopiaECola || '00020126580014br.gov.bcb.pix2536pay.xingubit.com.br...'}
            </Typography>

            <Button
              variant="contained"
              color="primary"
              fullWidth
              startIcon={<ContentCopyIcon />}
              onClick={() => handleCopyPix(selectedInvoice?.pixCopiaECola || '00020126580014br.gov.bcb.pix...')}
              sx={{ fontWeight: 'bold', borderRadius: 2, py: 1 }}
            >
              Copiar Código Pix
            </Button>
          </Paper>
        </DialogContent>
        <DialogActions sx={{ justifyContent: 'center', pb: 2 }}>
          <Button onClick={() => setPixModalOpen(false)} color="inherit">
            Fechar
          </Button>
        </DialogActions>
      </Dialog>

      {/* MODAL UPGRADE DE PLANO */}
      <Dialog
        open={upgradeModalOpen}
        onClose={() => setUpgradeModalOpen(false)}
        maxWidth="md"
        fullWidth
        PaperProps={{ sx: { borderRadius: 3, p: 2 } }}
      >
        <DialogTitle sx={{ fontWeight: 'bold' }}>
          Escolha seu Novo Plano de Internet
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Selecione o plano desejado. A velocidade será atualizada automaticamente na sua conexão!
          </Typography>

          <Grid container spacing={2}>
            {availableUpgradePlans?.map((p) => {
              const isSelected = selectedUpgradePlan?.id === p.id;
              return (
                <Grid item xs={12} sm={6} md={4} key={p.id}>
                  <Card
                    onClick={() => setSelectedUpgradePlan(p)}
                    sx={{
                      borderRadius: 3,
                      cursor: 'pointer',
                      border: isSelected ? '2px solid #1976d2' : '1px solid #e0e0e0',
                      bgcolor: isSelected ? 'rgba(255, 118, 210, 0.05)' : 'inherit',
                      transition: '0.2s',
                      '&:hover': { transform: 'scale(1.02)' }
                    }}
                  >
                    <CardContent sx={{ p: 2.5, textAlign: 'center' }}>
                      <Typography variant="h6" fontWeight="bold">
                        {p.name}
                      </Typography>
                      <Typography variant="h4" fontWeight="bold" color="primary.main" sx={{ my: 1.5 }}>
                        {p.downloadSpeed} Mega
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        Upload: {p.uploadSpeed} Mbps
                      </Typography>
                      <Typography variant="h6" fontWeight="bold" sx={{ mt: 2 }}>
                        R$ {Number(p.price).toFixed(2).replace('.', ',')} /mês
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
              );
            })}
          </Grid>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setUpgradeModalOpen(false)} color="inherit">
            Cancelar
          </Button>
          <Button
            variant="contained"
            color="primary"
            disabled={!selectedUpgradePlan}
            onClick={handleExecuteUpgrade}
            sx={{ fontWeight: 'bold', borderRadius: 2, px: 3 }}
          >
            Confirmar Upgrade de Plano
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal de Configuração de PIN de 4 dígitos */}
      <Dialog open={pinModalOpen} onClose={() => setPinModalOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ fontWeight: 'bold' }}>
          {clientSession?.hasPin ? 'Alterar PIN de Segurança' : 'Cadastrar PIN de 4 Dígitos'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            O PIN numérico de 4 dígitos protege o acesso às suas faturas e histórico de chamados.
          </Typography>

          {clientSession?.hasPin && (
            <TextField
              fullWidth
              label="PIN Atual"
              type="password"
              inputProps={{ maxLength: 4, inputMode: 'numeric' }}
              value={currentPin}
              onChange={(e) => setCurrentPin(e.target.value.replace(/\D/g, ''))}
              placeholder="••••"
              sx={{ mb: 2 }}
            />
          )}

          <TextField
            fullWidth
            label="Novo PIN (4 dígitos)"
            type="password"
            inputProps={{ maxLength: 4, inputMode: 'numeric' }}
            value={newPin}
            onChange={(e) => setNewPin(e.target.value.replace(/\D/g, ''))}
            placeholder="••••"
            sx={{ mb: 2 }}
          />

          <TextField
            fullWidth
            label="Confirmar Novo PIN"
            type="password"
            inputProps={{ maxLength: 4, inputMode: 'numeric' }}
            value={confirmPin}
            onChange={(e) => setConfirmPin(e.target.value.replace(/\D/g, ''))}
            placeholder="••••"
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setPinModalOpen(false)} color="inherit">
            Cancelar
          </Button>
          <Button onClick={handleSavePin} variant="contained" disabled={pinSaving || newPin.length !== 4}>
            {pinSaving ? <CircularProgress size={20} /> : 'Salvar PIN'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar Toast */}
      <Snackbar
        open={toast.open}
        autoHideDuration={4000}
        onClose={() => setToast({ ...toast, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert severity={toast.severity as any} sx={{ borderRadius: 2, fontWeight: 'bold' }}>
          {toast.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ClientPortal;
