import React, { useState, useEffect } from 'react';
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
  Snackbar
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
  Key as KeyIcon
} from '@mui/icons-material';
import clientPortalService from '../../services/clientPortalService';

function TabPanel(props) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );
}

const ClientPortal = () => {
  const [loading, setLoading] = useState(true);
  const [dashboard, setDashboard] = useState(null);
  const [activeTab, setActiveTab] = useState(0);
  const [invoiceSubTab, setInvoiceSubTab] = useState(0);

  // Modals
  const [pixModalOpen, setPixModalOpen] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [upgradeModalOpen, setUpgradeModalOpen] = useState(false);
  const [selectedUpgradePlan, setSelectedUpgradePlan] = useState(null);

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

  const loadDashboard = async () => {
    try {
      setLoading(true);
      const data = await clientPortalService.getDashboard();
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
    } catch (err) {
      console.error('Erro ao carregar Central do Assinante:', err);
      setToast({
        open: true,
        message: 'Erro ao carregar dados da Central do Assinante',
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

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
      await clientPortalService.upgradePlan(dashboard.contract.id, selectedUpgradePlan.id);
      setToast({
        open: true,
        message: `Upgrade para o plano ${selectedUpgradePlan.name} realizado com sucesso!`,
        severity: 'success'
      });
      setUpgradeModalOpen(false);
      loadDashboard();
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
      await clientPortalService.requestTrustUnblock(dashboard.contract.id);
      setToast({
        open: true,
        message: 'Desbloqueio em Confiança concedido por 48 horas! Sua conexão foi reativada.',
        severity: 'success'
      });
      loadDashboard();
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
      await clientPortalService.updateProfile(profileForm);
      setToast({
        open: true,
        message: 'Dados cadastrais atualizados com sucesso!',
        severity: 'success'
      });
      loadDashboard();
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
      });
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
            <Chip
              icon={isConnectionBlocked ? <WarningIcon /> : <CheckCircleIcon />}
              label={isConnectionBlocked ? 'Conexão Suspensa' : 'Conexão Ativa'}
              color={isConnectionBlocked ? 'error' : 'success'}
              sx={{ fontWeight: 'bold', fontSize: '0.9rem', py: 2, px: 1 }}
            />
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
          {connectionStatusMessage}
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
                  {currentPlan?.name || 'Plano Básico'}
                </Typography>

                <Grid container spacing={2} sx={{ my: 2 }}>
                  <Grid item xs={6}>
                    <Paper variant="outlined" sx={{ p: 2, textAlign: 'center', borderRadius: 2 }}>
                      <Typography variant="body2" color="text.secondary">
                        Velocidade Download
                      </Typography>
                      <Typography variant="h5" fontWeight="bold" color="primary.main">
                        {currentPlan?.downloadSpeed || 0} Mbps
                      </Typography>
                    </Paper>
                  </Grid>
                  <Grid item xs={6}>
                    <Paper variant="outlined" sx={{ p: 2, textAlign: 'center', borderRadius: 2 }}>
                      <Typography variant="body2" color="text.secondary">
                        Velocidade Upload
                      </Typography>
                      <Typography variant="h5" fontWeight="bold" color="secondary.main">
                        {currentPlan?.uploadSpeed || 0} Mbps
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
                      R$ {contract?.monthlyFee ? Number(contract.monthlyFee).toFixed(2).replace('.', ',') : '0,00'}
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
                      {contract?.installationAddress}, {contract?.city} - {contract?.state}
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
                      ONT/ONU sincronizada na OLT
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
                  Parabéns! Você não possui nenhuma fatura em aberto no momento.
                </Alert>
              </Grid>
            ) : (
              pendingInvoices.map((inv) => (
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
              overdueInvoices.map((inv) => (
                <Grid item xs={12} md={6} key={inv.id}>
                  <Card sx={{ borderRadius: 3, borderLeft: '6px solid #d32f2f' }}>
                    <CardContent sx={{ p: 3 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                        <Typography variant="subtitle1" fontWeight="bold" color="error">
                          Fatura Vencida
                        </Typography>
                        <Chip label="Atrasada" color="error" size="small" />
                      </Box>

                      <Typography variant="h4" fontWeight="bold" color="error" sx={{ my: 1 }}>
                        R$ {Number(inv.amount).toFixed(2).replace('.', ',')}
                      </Typography>

                      <Typography variant="body2" color="error">
                        Venceu em: <strong>{new Date(inv.dueDate).toLocaleDateString('pt-BR')}</strong>
                      </Typography>

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
              paidInvoices.map((inv) => (
                <Grid item xs={12} md={6} key={inv.id}>
                  <Card sx={{ borderRadius: 3, borderLeft: '6px solid #2e7d32' }}>
                    <CardContent sx={{ p: 3 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                        <Typography variant="subtitle1" fontWeight="bold">
                          Fatura Liquidada
                        </Typography>
                        <Chip icon={<CheckCircleIcon />} label="Pago" color="success" size="small" />
                      </Box>

                      <Typography variant="h5" fontWeight="bold" color="success.main" sx={{ my: 1 }}>
                        R$ {Number(inv.amount).toFixed(2).replace('.', ',')}
                      </Typography>

                      <Typography variant="body2" color="text.secondary">
                        Vencimento: {new Date(inv.dueDate).toLocaleDateString('pt-BR')}
                      </Typography>
                      {inv.paidAt && (
                        <Typography variant="caption" color="text.secondary" display="block">
                          Pago em: {new Date(inv.paidAt).toLocaleDateString('pt-BR')}
                        </Typography>
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
                      bgcolor: isSelected ? 'rgba(25, 118, 210, 0.05)' : 'inherit',
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

      {/* Snackbar Toast */}
      <Snackbar
        open={toast.open}
        autoHideDuration={4000}
        onClose={() => setToast({ ...toast, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert severity={toast.severity} sx={{ borderRadius: 2, fontWeight: 'bold' }}>
          {toast.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ClientPortal;
