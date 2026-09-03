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
  FormControlLabel,
  Grid,
  IconButton,
  MenuItem,
  Paper,
  Switch,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
  Alert,
} from '@mui/material';
import {
  Receipt as NfcomIcon,
  Business as CompanyIcon,
  VpnKey as CertificateIcon,
  Archive as ZipIcon,
  Download as DownloadIcon,
  PictureAsPdf as PdfIcon,
  Code as XmlIcon,
  Cancel as CancelIcon,
  CheckCircle as ActiveIcon,
  Warning as WarningIcon,
  Save as SaveIcon,
  Refresh as RefreshIcon,
  CloudUpload as UploadIcon,
  Send as SendIcon,
  MarkEmailRead as EmailSentIcon,
  Schedule as ScheduleIcon,
  History as HistoryIcon,
} from '@mui/icons-material';
import fiscalService from '../../services/fiscalService';
import fiscalRegimeService from '../../services/fiscalRegimeService';
import { FiscalCompany, NfcomRecord } from '../../types/fiscal';
import { FiscalRegimeTransition, FiscalRegimeTransitionRequest } from '../../types/regime';

export const FiscalDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);
  const [feedback, setFeedback] = useState<{ type: string; message: string }>({ type: '', message: '' });

  // Tab 0: Emissões NFCom
  const [records, setRecords] = useState<NfcomRecord[]>([]);
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [cancelModalOpen, setCancelModalOpen] = useState<boolean>(false);
  const [selectedRecordId, setSelectedRecordId] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState<string>('');

  // Tab 1: Empresa & Configuração Fiscal
  const [companyForm, setCompanyForm] = useState<FiscalCompany>({
    id: undefined,
    cnpj: '12.345.678/0001-95',
    razaoSocial: 'Xingu Telecom Provedor de Internet Ltda',
    nomeFantasia: 'Xingu Telecom',
    inscricaoEstadual: '15999888',
    inscricaoMunicipal: '998877',
    cnaePrincipal: '6110-8/03',
    regimeTributario: 'SIMPLES_NACIONAL',
    aliquotaIcms: 0,
    aliquotaFust: 0.65,
    aliquotaFunttel: 0.50,
    aliquotaPis: 0,
    aliquotaCofins: 0,
    logradouro: 'Av. Brigadeiro Eduardo Gomes',
    numero: '1000',
    complemento: 'Sala 102',
    bairro: 'Centro',
    cidade: 'Altamira',
    uf: 'PA',
    cep: '68370-000',
    codigoIbge: '1500602',
    telefone: '93988887777',
    emailFiscal: 'fiscal@xingubit.com.br',
    nfcomAmbiente: 'HOMOLOGACAO',
    nfcomSerie: '1',
    nfcomProximoNumero: 1,
    hasCertificate: false,
    certificateExpiresAt: undefined,
    fiscalConfirmed: false,
    fiscalConfirmedAt: undefined,
    accountingName: 'Assessoria Contábil Silva & Associados',
    accountingEmails: 'fiscal@contabilidade.com.br',
    accountingSendDay: 5,
    accountingAutoSend: true,
    accountingLastSentAt: undefined,
  });

  const [certFile, setCertFile] = useState<File | null>(null);
  const [certPassword, setCertPassword] = useState<string>('');

  // Tab 2: Mudanças de Regime Fiscal (Agendadas & Imediatas)
  const [transitions, setTransitions] = useState<FiscalRegimeTransition[]>([]);
  const [transitionForm, setTransitionForm] = useState<FiscalRegimeTransitionRequest>({
    newRegime: 'LUCRO_PRESUMIDO',
    effectiveDate: new Date().toISOString().split('T')[0],
    aliquotaIcms: 18.0,
    aliquotaPis: 0.65,
    aliquotaCofins: 3.0,
    aliquotaFust: 0.65,
    aliquotaFunttel: 0.50,
    notes: 'Virada de exercício contábil',
  });

  // Tab 3: Convênio 115 / Contabilidade
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear());
  const [selectedMonth, setSelectedMonth] = useState<number>(new Date().getMonth() + 1);

  const loadData = async () => {
    try {
      setLoading(true);
      const [recs, comp] = await Promise.all([
        fiscalService.getRecords().catch(() => []),
        fiscalService.getActiveCompany().catch(() => null),
      ]);

      if (recs) setRecords(recs);
      if (comp) {
        setCompanyForm(comp);
        loadTransitions(comp.id);
      }
    } catch (err: any) {
      setFeedback({ type: 'error', message: 'Erro ao carregar dados fiscais: ' + (err?.message || err) });
    } finally {
      setLoading(false);
    }
  };

  const loadTransitions = async (companyId?: string) => {
    try {
      const list = await fiscalRegimeService.getTransitions(companyId);
      setTransitions(list);
    } catch (e) {
      console.warn('Erro ao carregar histórico de transições:', e);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleRegimeChange = (regime: 'SIMPLES_NACIONAL' | 'LUCRO_PRESUMIDO' | 'LUCRO_REAL') => {
    let aliquotas = {
      aliquotaIcms: 0,
      aliquotaPis: 0,
      aliquotaCofins: 0,
      aliquotaFust: 0.65,
      aliquotaFunttel: 0.50,
    };
    if (regime === 'LUCRO_PRESUMIDO') {
      aliquotas = {
        aliquotaIcms: 18.0,
        aliquotaPis: 0.65,
        aliquotaCofins: 3.0,
        aliquotaFust: 0.65,
        aliquotaFunttel: 0.50,
      };
    } else if (regime === 'LUCRO_REAL') {
      aliquotas = {
        aliquotaIcms: 18.0,
        aliquotaPis: 1.65,
        aliquotaCofins: 7.6,
        aliquotaFust: 0.65,
        aliquotaFunttel: 0.50,
      };
    }
    setCompanyForm((prev) => ({
      ...prev,
      regimeTributario: regime,
      ...aliquotas,
    }));
  };

  const handleSaveCompany = async (confirmed = true, e?: React.FormEvent) => {
    if (e && e.preventDefault) e.preventDefault();
    try {
      setLoading(true);
      const payload: Partial<FiscalCompany> = {
        ...companyForm,
        fiscalConfirmed: confirmed,
      };
      const saved = await fiscalService.saveCompany(payload);
      setCompanyForm(saved);
      if (confirmed) {
        setFeedback({ type: 'success', message: 'Configurações fiscais confirmadas e salvas com sucesso!' });
      } else {
        setFeedback({ type: 'info', message: 'Dados salvos provisoriamente como pendentes de validação com o contador.' });
      }
    } catch (err: any) {
      setFeedback({ type: 'error', message: 'Falha ao salvar dados da empresa: ' + (err?.message || err) });
    } finally {
      setLoading(false);
    }
  };

  const handleUploadCertificate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!certFile || !certPassword || !companyForm.id) {
      setFeedback({ type: 'warning', message: 'Selecione o arquivo .pfx e informe a senha do certificado.' });
      return;
    }

    try {
      setLoading(true);
      const res = await fiscalService.uploadCertificate(companyForm.id, certFile, certPassword);
      if (res.success) {
        setFeedback({ type: 'success', message: 'Certificado Digital A1 instalado com sucesso no gateway!' });
        setCertFile(null);
        setCertPassword('');
        loadData();
      } else {
        setFeedback({ type: 'error', message: res.errorMessage || 'Falha no upload do certificado.' });
      }
    } catch (err: any) {
      setFeedback({ type: 'error', message: 'Erro no envio do certificado: ' + (err?.message || err) });
    } finally {
      setLoading(false);
    }
  };

  const handleScheduleTransition = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setLoading(true);
      const payload: FiscalRegimeTransitionRequest = {
        ...transitionForm,
        companyId: companyForm.id,
      };
      await fiscalRegimeService.scheduleOrApplyTransition(payload);
      setFeedback({ type: 'success', message: 'Transição de regime fiscal processada / agendada com sucesso!' });
      loadData();
    } catch (err: any) {
      setFeedback({ type: 'error', message: 'Erro ao programar transição fiscal: ' + (err?.message || err) });
    } finally {
      setLoading(false);
    }
  };

  const handleCancelTransition = async (id: string) => {
    try {
      setLoading(true);
      await fiscalRegimeService.cancelTransition(id);
      setFeedback({ type: 'success', message: 'Transição cancelada com sucesso.' });
      if (companyForm.id) loadTransitions(companyForm.id);
    } catch (err: any) {
      setFeedback({ type: 'error', message: 'Erro ao cancelar transição: ' + (err?.message || err) });
    } finally {
      setLoading(false);
    }
  };

  const handleCancelNfcom = async () => {
    if (!cancelReason || !selectedRecordId) {
      setFeedback({ type: 'warning', message: 'Informe a justificativa de cancelamento.' });
      return;
    }

    try {
      setLoading(true);
      await fiscalService.cancelNfcom(selectedRecordId, cancelReason);
      setFeedback({ type: 'success', message: 'Solicitação de cancelamento enviada à SEFAZ com sucesso!' });
      setCancelModalOpen(false);
      setCancelReason('');
      setSelectedRecordId(null);
      loadData();
    } catch (err: any) {
      setFeedback({ type: 'error', message: 'Falha no cancelamento da NFCom: ' + (err?.message || err) });
    } finally {
      setLoading(false);
    }
  };

  const handleSendAccounting = async () => {
    try {
      setLoading(true);
      const res = await fiscalService.sendAccountingReport(selectedYear, selectedMonth);
      if (res.success) {
        setFeedback({
          type: 'success',
          message: `Lote fiscal de ${selectedMonth}/${selectedYear} enviado com sucesso para a contabilidade!`,
        });
        loadData();
      } else {
        setFeedback({ type: 'error', message: res.message || 'Falha ao despachar pacote contábil.' });
      }
    } catch (err: any) {
      setFeedback({ type: 'error', message: 'Erro ao enviar para contabilidade: ' + (err?.message || err) });
    } finally {
      setLoading(false);
    }
  };

  const filteredRecords = (Array.isArray(records) ? records : []).filter(
    (r) =>
      (r.accessKey && r.accessKey.includes(searchTerm)) ||
      (r.documentNumber && r.documentNumber.toString().includes(searchTerm)) ||
      (r.status && r.status.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <NfcomIcon color="primary" sx={{ fontSize: 32 }} /> Módulo Fiscal, NFCom (Mod. 62) & Tributação
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Emissão de Notas Fiscais Fatura de Telecomunicações, Regimes Tributários, Certificado A1 e Convênio ICMS 115/03.
          </Typography>
        </Box>
        <Button variant="outlined" startIcon={<RefreshIcon />} onClick={loadData} disabled={loading}>
          Atualizar
        </Button>
      </Box>

      {/* Alerts / Feedback */}
      {feedback.message && (
        <Alert
          severity={feedback.type === 'error' ? 'error' : feedback.type === 'warning' ? 'warning' : feedback.type === 'info' ? 'info' : 'success'}
          onClose={() => setFeedback({ type: '', message: '' })}
          sx={{ mb: 3 }}
        >
          {feedback.message}
        </Alert>
      )}

      {/* Tabs */}
      <Paper sx={{ mb: 3 }}>
        <Tabs value={activeTab} onChange={(_e, val) => setActiveTab(val)} indicatorColor="primary" textColor="primary">
          <Tab icon={<NfcomIcon />} iconPosition="start" label="Emissões NFCom (Modelo 62)" />
          <Tab icon={<CompanyIcon />} iconPosition="start" label="Empresa & Certificado A1" />
          <Tab icon={<ScheduleIcon />} iconPosition="start" label="Transição de Regime Fiscal" />
          <Tab icon={<ZipIcon />} iconPosition="start" label="Convênio ICMS 115/03 & Contabilidade" />
        </Tabs>
      </Paper>

      {/* TAB 0: LISTAGEM DE NFCOM */}
      {activeTab === 0 && (
        <Box>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Grid container spacing={2} alignItems="center">
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    size="small"
                    placeholder="Pesquisar por Chave de Acesso (44 dígitos), Número ou Status..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={6} sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
                  <Chip
                    label={`Ambiente: ${companyForm.nfcomAmbiente}`}
                    color={companyForm.nfcomAmbiente === 'PRODUCAO' ? 'success' : 'warning'}
                    variant="outlined"
                  />
                  <Chip label={`Próximo Número: ${companyForm.nfcomProximoNumero}`} variant="outlined" />
                </Grid>
              </Grid>
            </CardContent>
          </Card>

          <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
            <Table>
              <TableHead sx={{ bgcolor: 'grey.100' }}>
                <TableRow>
                  <TableCell><strong>Número / Série</strong></TableCell>
                  <TableCell><strong>Chave de Acesso</strong></TableCell>
                  <TableCell><strong>Status SEFAZ</strong></TableCell>
                  <TableCell><strong>Data Autorização</strong></TableCell>
                  <TableCell align="right"><strong>Ações</strong></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredRecords.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                      Nenhuma NFCom emitida registrada até o momento.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredRecords.map((r) => (
                    <TableRow key={r.id} hover>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                          NFCom #{r.documentNumber}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          Série {r.series} • Mod. 62
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" sx={{ fontFamily: 'monospace', bgcolor: 'grey.100', p: 0.5, borderRadius: 1 }}>
                          {r.accessKey || 'Em processamento...'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={r.status}
                          size="small"
                          color={r.status === 'AUTHORIZED' ? 'success' : r.status === 'CANCELED' ? 'error' : 'default'}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption">
                          {r.issuedAt ? new Date(r.issuedAt).toLocaleString('pt-BR') : '-'}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        {r.danfePdfUrl && (
                          <Tooltip title="Visualizar / Baixar DANFE (PDF)">
                            <IconButton color="primary" size="small" component="a" href={r.danfePdfUrl} target="_blank">
                              <PdfIcon />
                            </IconButton>
                          </Tooltip>
                        )}
                        {r.xmlAuthorized && (
                          <Tooltip title="Download XML Protocolado">
                            <IconButton color="secondary" size="small" component="a" href={r.xmlAuthorized} target="_blank">
                              <XmlIcon />
                            </IconButton>
                          </Tooltip>
                        )}
                        {r.status !== 'CANCELED' && (
                          <Tooltip title="Cancelar NFCom">
                            <IconButton
                              color="error"
                              size="small"
                              onClick={() => {
                                setSelectedRecordId(r.id);
                                setCancelModalOpen(true);
                              }}
                            >
                              <CancelIcon />
                            </IconButton>
                          </Tooltip>
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      )}

      {/* TAB 1: EMPRESA & CERTIFICADO A1 */}
      {activeTab === 1 && (
        <Grid container spacing={3}>
          {/* Dados da Empresa */}
          <Grid item xs={12} md={7}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 1, mb: 2 }}>
                  <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <CompanyIcon color="primary" /> Dados Cadastrais & Fiscais do Provedor
                  </Typography>
                  <Chip
                    label={companyForm.fiscalConfirmed ? '🟢 Configuração Fiscal Validada' : '🟡 Pendente de Confirmação Contábil'}
                    color={companyForm.fiscalConfirmed ? 'success' : 'warning'}
                    variant="outlined"
                    size="small"
                  />
                </Box>

                {!companyForm.fiscalConfirmed && (
                  <Alert severity="info" sx={{ mb: 3 }}>
                    ℹ️ <strong>Status Provisório:</strong> Os parâmetros fiscais estão salvos provisoriamente. Você pode utilizar todo o sistema normalmente e validar as alíquotas com sua contabilidade a qualquer momento antes da emissão definitiva em produção.
                  </Alert>
                )}

                <form onSubmit={(e) => handleSaveCompany(true, e)}>
                  <Grid container spacing={2}>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        size="small"
                        label="CNPJ da Empresa"
                        value={companyForm.cnpj}
                        onChange={(e) => setCompanyForm({ ...companyForm, cnpj: e.target.value })}
                        required
                      />
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Inscrição Estadual (IE)"
                        value={companyForm.inscricaoEstadual}
                        onChange={(e) => setCompanyForm({ ...companyForm, inscricaoEstadual: e.target.value })}
                        required
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Razão Social"
                        value={companyForm.razaoSocial}
                        onChange={(e) => setCompanyForm({ ...companyForm, razaoSocial: e.target.value })}
                        required
                      />
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        size="small"
                        label="CNAE Principal"
                        value={companyForm.cnaePrincipal}
                        onChange={(e) => setCompanyForm({ ...companyForm, cnaePrincipal: e.target.value })}
                        helperText="6110-8/03 (Serviços de Comunicação Multimídia - SCM)"
                        required
                      />
                    </Grid>
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        size="small"
                        select
                        label="Regime Tributário"
                        value={companyForm.regimeTributario}
                        onChange={(e) => handleRegimeChange(e.target.value as any)}
                      >
                        <MenuItem value="SIMPLES_NACIONAL">Simples Nacional</MenuItem>
                        <MenuItem value="LUCRO_PRESUMIDO">Lucro Presumido</MenuItem>
                        <MenuItem value="LUCRO_REAL">Lucro Real</MenuItem>
                      </TextField>
                    </Grid>

                    {/* Explicação e Alíquotas do Assistente Fiscal */}
                    <Grid item xs={12}>
                      <Paper variant="outlined" sx={{ p: 2, bgcolor: 'background.default', borderRadius: 2 }}>
                        <Typography variant="subtitle2" fontWeight="bold" gutterBottom color="primary">
                          📊 Assistente de Alíquotas Sugeridas ({companyForm.regimeTributario}):
                        </Typography>
                        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1.5 }}>
                          {companyForm.regimeTributario === 'SIMPLES_NACIONAL' &&
                            '💡 No Simples Nacional, o ICMS, PIS e COFINS são recolhidos de forma unificada na guia DAS. O FUST (0.65%) e FUNTTEL (0.50%) são calculados conforme a Lei 9.998/2000.'}
                          {companyForm.regimeTributario === 'LUCRO_PRESUMIDO' &&
                            '💡 No Lucro Presumido, aplica-se o ICMS estadual padrão (ex: 18%), PIS cumulativo (0.65%), COFINS cumulativo (3.00%), FUST (0.65%) e FUNTTEL (0.50%).'}
                          {companyForm.regimeTributario === 'LUCRO_REAL' &&
                            '💡 No Lucro Real, aplica-se o ICMS estadual padrão (ex: 18%), PIS não-cumulativo (1.65%), COFINS não-cumulativo (7.60%), FUST (0.65%) e FUNTTEL (0.50%).'}
                        </Typography>
                        <Grid container spacing={1.5}>
                          <Grid item xs={6} sm={2.4}>
                            <TextField
                              fullWidth
                              size="small"
                              type="number"
                              label="ICMS (%)"
                              value={companyForm.aliquotaIcms}
                              onChange={(e) => setCompanyForm({ ...companyForm, aliquotaIcms: parseFloat(e.target.value) || 0 })}
                            />
                          </Grid>
                          <Grid item xs={6} sm={2.4}>
                            <TextField
                              fullWidth
                              size="small"
                              type="number"
                              label="PIS (%)"
                              value={companyForm.aliquotaPis ?? 0}
                              onChange={(e) => setCompanyForm({ ...companyForm, aliquotaPis: parseFloat(e.target.value) || 0 })}
                            />
                          </Grid>
                          <Grid item xs={6} sm={2.4}>
                            <TextField
                              fullWidth
                              size="small"
                              type="number"
                              label="COFINS (%)"
                              value={companyForm.aliquotaCofins ?? 0}
                              onChange={(e) => setCompanyForm({ ...companyForm, aliquotaCofins: parseFloat(e.target.value) || 0 })}
                            />
                          </Grid>
                          <Grid item xs={6} sm={2.4}>
                            <TextField
                              fullWidth
                              size="small"
                              type="number"
                              label="FUST (%)"
                              value={companyForm.aliquotaFust}
                              onChange={(e) => setCompanyForm({ ...companyForm, aliquotaFust: parseFloat(e.target.value) || 0 })}
                            />
                          </Grid>
                          <Grid item xs={6} sm={2.4}>
                            <TextField
                              fullWidth
                              size="small"
                              type="number"
                              label="FUNTTEL (%)"
                              value={companyForm.aliquotaFunttel}
                              onChange={(e) => setCompanyForm({ ...companyForm, aliquotaFunttel: parseFloat(e.target.value) || 0 })}
                            />
                          </Grid>
                        </Grid>
                      </Paper>
                    </Grid>

                    <Grid item xs={12} md={8}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Logradouro Fiscal"
                        value={companyForm.logradouro}
                        onChange={(e) => setCompanyForm({ ...companyForm, logradouro: e.target.value })}
                        required
                      />
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Número"
                        value={companyForm.numero}
                        onChange={(e) => setCompanyForm({ ...companyForm, numero: e.target.value })}
                        required
                      />
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Cidade"
                        value={companyForm.cidade}
                        onChange={(e) => setCompanyForm({ ...companyForm, cidade: e.target.value })}
                        required
                      />
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <TextField
                        fullWidth
                        size="small"
                        label="UF"
                        value={companyForm.uf}
                        onChange={(e) => setCompanyForm({ ...companyForm, uf: e.target.value })}
                        required
                      />
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Código IBGE"
                        value={companyForm.codigoIbge}
                        onChange={(e) => setCompanyForm({ ...companyForm, codigoIbge: e.target.value })}
                        required
                      />
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <TextField
                        fullWidth
                        size="small"
                        select
                        label="Ambiente NFCom"
                        value={companyForm.nfcomAmbiente}
                        onChange={(e) => setCompanyForm({ ...companyForm, nfcomAmbiente: e.target.value as any })}
                      >
                        <MenuItem value="HOMOLOGACAO">Homologação</MenuItem>
                        <MenuItem value="PRODUCAO">Produção</MenuItem>
                      </TextField>
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Série NFCom"
                        value={companyForm.nfcomSerie}
                        onChange={(e) => setCompanyForm({ ...companyForm, nfcomSerie: e.target.value })}
                      />
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <TextField
                        fullWidth
                        size="small"
                        type="number"
                        label="Próximo Número"
                        value={companyForm.nfcomProximoNumero}
                        onChange={(e) => setCompanyForm({ ...companyForm, nfcomProximoNumero: parseInt(e.target.value) || 1 })}
                      />
                    </Grid>
                    <Grid item xs={12} sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mt: 1 }}>
                      <Button
                        type="button"
                        variant="outlined"
                        color="warning"
                        disabled={loading}
                        onClick={() => handleSaveCompany(false)}
                      >
                        Vou confirmar com meu contador
                      </Button>
                      <Button
                        type="button"
                        variant="contained"
                        color="primary"
                        startIcon={<SaveIcon />}
                        disabled={loading}
                        onClick={() => handleSaveCompany(true)}
                      >
                        Confirmar e Salvar Dados Fiscais
                      </Button>
                    </Grid>
                  </Grid>
                </form>
              </CardContent>
            </Card>
          </Grid>

          {/* Certificado Digital A1 */}
          <Grid item xs={12} md={5}>
            <Card sx={{ mb: 3 }}>
              <CardContent>
                <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <CertificateIcon color="primary" /> Certificado Digital A1 (e-CNPJ)
                </Typography>

                <Box sx={{ p: 2, bgcolor: companyForm.hasCertificate ? 'success.50' : 'warning.50', borderRadius: 2, mb: 3, border: '1px solid', borderColor: companyForm.hasCertificate ? 'success.200' : 'warning.200' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    {companyForm.hasCertificate ? <ActiveIcon color="success" /> : <WarningIcon color="warning" />}
                    <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                      {companyForm.hasCertificate ? 'Certificado A1 Ativo e Instalado' : 'Certificado A1 Não Instalado'}
                    </Typography>
                  </Box>
                  {companyForm.certificateExpiresAt && (
                    <Typography variant="caption" color="text.secondary" display="block">
                      Expira em: {new Date(companyForm.certificateExpiresAt).toLocaleDateString('pt-BR')}
                    </Typography>
                  )}
                </Box>

                <form onSubmit={handleUploadCertificate}>
                  <Grid container spacing={2}>
                    <Grid item xs={12}>
                      <Button variant="outlined" component="label" fullWidth startIcon={<UploadIcon />}>
                        {certFile ? certFile.name : 'Selecionar Certificado (.pfx / .p12)'}
                        <input
                          type="file"
                          hidden
                          accept=".pfx,.p12"
                          onChange={(e) => e.target.files && setCertFile(e.target.files[0])}
                        />
                      </Button>
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        fullWidth
                        size="small"
                        type="password"
                        label="Senha do Certificado"
                        value={certPassword}
                        onChange={(e) => setCertPassword(e.target.value)}
                        placeholder="••••••••"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <Button type="submit" variant="contained" color="secondary" fullWidth disabled={loading || !certFile || !certPassword}>
                        Instalar Certificado A1
                      </Button>
                    </Grid>
                  </Grid>
                </form>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* TAB 2: TRANSIÇÃO DE REGIME FISCAL (AGENDADA & IMEDIATA) */}
      {activeTab === 2 && (
        <Grid container spacing={3}>
          <Grid item xs={12} md={5}>
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <ScheduleIcon color="primary" /> Programar Mudança de Regime Fiscal
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Planeje com antecedência a virada de ano fiscal ou aplique uma mudança imediata com recálculo automático de alíquotas.
                </Typography>

                <form onSubmit={handleScheduleTransition}>
                  <Grid container spacing={2}>
                    <Grid item xs={12}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Regime Atual"
                        value={companyForm.regimeTributario}
                        disabled
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        fullWidth
                        size="small"
                        select
                        label="Novo Regime Tributário"
                        value={transitionForm.newRegime}
                        onChange={(e) => {
                          const val = e.target.value as any;
                          let aliq = { aliquotaIcms: 18.0, aliquotaPis: 0.65, aliquotaCofins: 3.0, aliquotaFust: 0.65, aliquotaFunttel: 0.50 };
                          if (val === 'SIMPLES_NACIONAL') {
                            aliq = { aliquotaIcms: 0, aliquotaPis: 0, aliquotaCofins: 0, aliquotaFust: 0.65, aliquotaFunttel: 0.50 };
                          } else if (val === 'LUCRO_REAL') {
                            aliq = { aliquotaIcms: 18.0, aliquotaPis: 1.65, aliquotaCofins: 7.60, aliquotaFust: 0.65, aliquotaFunttel: 0.50 };
                          }
                          setTransitionForm({ ...transitionForm, newRegime: val, ...aliq });
                        }}
                      >
                        <MenuItem value="SIMPLES_NACIONAL">Simples Nacional</MenuItem>
                        <MenuItem value="LUCRO_PRESUMIDO">Lucro Presumido</MenuItem>
                        <MenuItem value="LUCRO_REAL">Lucro Real</MenuItem>
                      </TextField>
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        fullWidth
                        size="small"
                        type="date"
                        label="Data de Início da Vigência"
                        InputLabelProps={{ shrink: true }}
                        value={transitionForm.effectiveDate}
                        onChange={(e) => setTransitionForm({ ...transitionForm, effectiveDate: e.target.value })}
                        helperText="Se for hoje ou passada, será aplicada de imediato. Se for futura (ex: 01/01), será agendada."
                        required
                      />
                    </Grid>
                    <Grid item xs={6}>
                      <TextField
                        fullWidth
                        size="small"
                        type="number"
                        label="ICMS (%)"
                        value={transitionForm.aliquotaIcms}
                        onChange={(e) => setTransitionForm({ ...transitionForm, aliquotaIcms: parseFloat(e.target.value) || 0 })}
                      />
                    </Grid>
                    <Grid item xs={6}>
                      <TextField
                        fullWidth
                        size="small"
                        type="number"
                        label="PIS (%)"
                        value={transitionForm.aliquotaPis}
                        onChange={(e) => setTransitionForm({ ...transitionForm, aliquotaPis: parseFloat(e.target.value) || 0 })}
                      />
                    </Grid>
                    <Grid item xs={6}>
                      <TextField
                        fullWidth
                        size="small"
                        type="number"
                        label="COFINS (%)"
                        value={transitionForm.aliquotaCofins}
                        onChange={(e) => setTransitionForm({ ...transitionForm, aliquotaCofins: parseFloat(e.target.value) || 0 })}
                      />
                    </Grid>
                    <Grid item xs={6}>
                      <TextField
                        fullWidth
                        size="small"
                        type="number"
                        label="FUST (%)"
                        value={transitionForm.aliquotaFust}
                        onChange={(e) => setTransitionForm({ ...transitionForm, aliquotaFust: parseFloat(e.target.value) || 0 })}
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        fullWidth
                        size="small"
                        multiline
                        rows={2}
                        label="Motivo / Justificativa Contábil"
                        value={transitionForm.notes}
                        onChange={(e) => setTransitionForm({ ...transitionForm, notes: e.target.value })}
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <Button type="submit" variant="contained" color="primary" fullWidth startIcon={<ScheduleIcon />} disabled={loading}>
                        Efetivar / Agendar Transição
                      </Button>
                    </Grid>
                  </Grid>
                </form>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={7}>
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <HistoryIcon color="primary" /> Histórico & Agendamentos de Transições Fiscais
                </Typography>

                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead sx={{ bgcolor: 'grey.50' }}>
                      <TableRow>
                        <TableCell><strong>Vigência</strong></TableCell>
                        <TableCell><strong>De ➔ Para</strong></TableCell>
                        <TableCell><strong>Status</strong></TableCell>
                        <TableCell><strong>Alíquotas</strong></TableCell>
                        <TableCell align="right"><strong>Ação</strong></TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {transitions.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={5} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                            Nenhuma transição de regime registrada até o momento.
                          </TableCell>
                        </TableRow>
                      ) : (
                        transitions.map((t) => (
                          <TableRow key={t.id} hover>
                            <TableCell>
                              <Typography variant="body2" fontWeight="bold">
                                {new Date(t.effectiveDate + 'T00:00:00').toLocaleDateString('pt-BR')}
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                {t.notes || 'Sem observações'}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <Typography variant="caption" display="block">
                                {t.previousRegime} ➔ <strong>{t.newRegime}</strong>
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <Chip
                                label={t.status === 'APPLIED' ? 'APLICADO' : t.status === 'SCHEDULED' ? 'AGENDADO' : 'CANCELADO'}
                                color={t.status === 'APPLIED' ? 'success' : t.status === 'SCHEDULED' ? 'primary' : 'default'}
                                size="small"
                              />
                            </TableCell>
                            <TableCell>
                              <Typography variant="caption" display="block">
                                ICMS: {t.aliquotaIcms}% | PIS: {t.aliquotaPis}%
                              </Typography>
                              <Typography variant="caption" display="block" color="text.secondary">
                                COFINS: {t.aliquotaCofins}% | FUST: {t.aliquotaFust}%
                              </Typography>
                            </TableCell>
                            <TableCell align="right">
                              {t.status === 'SCHEDULED' && t.id && (
                                <Tooltip title="Cancelar Agendamento">
                                  <IconButton color="error" size="small" onClick={() => handleCancelTransition(t.id!)}>
                                    <CancelIcon fontSize="small" />
                                  </IconButton>
                                </Tooltip>
                              )}
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* TAB 3: CONVÊNIO ICMS 115/03 & CONTABILIDADE */}
      {activeTab === 3 && (
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <ZipIcon color="primary" /> Exportação de Arquivos Magnéticos (Convênio 115/03)
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Geração dos arquivos magnéticos MESTRE, ITEM e DESTINATÁRIO com assinatura digital MD5 para prestação de contas à SEFAZ estadual.
                </Typography>

                <Grid container spacing={2} alignItems="center">
                  <Grid item xs={6}>
                    <TextField
                      fullWidth
                      size="small"
                      type="number"
                      label="Ano de Competência"
                      value={selectedYear}
                      onChange={(e) => setSelectedYear(parseInt(e.target.value) || 2026)}
                    />
                  </Grid>
                  <Grid item xs={6}>
                    <TextField
                      fullWidth
                      size="small"
                      select
                      label="Mês de Competência"
                      value={selectedMonth}
                      onChange={(e) => setSelectedMonth(parseInt(e.target.value as string) || 1)}
                    >
                      {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                        <MenuItem key={m} value={m}>
                          {new Date(2026, m - 1).toLocaleString('pt-BR', { month: 'long' }).toUpperCase()}
                        </MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                  <Grid item xs={12}>
                    <Button
                      variant="contained"
                      color="primary"
                      fullWidth
                      startIcon={<DownloadIcon />}
                      component="a"
                      href={fiscalService.getConvenio115ExportUrl(selectedYear, selectedMonth)}
                      target="_blank"
                    >
                      Gerar Pacote ZIP (Convênio ICMS 115/03)
                    </Button>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <EmailSentIcon color="primary" /> Despacho Automático para Escritório Contábil
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Envio do espelho fiscal mensal, relatórios de apuração e XMLs para o escritório contábil cadastrado.
                </Typography>

                <Box sx={{ p: 2, bgcolor: 'grey.50', borderRadius: 2, mb: 3 }}>
                  <Typography variant="subtitle2" fontWeight="bold">
                    Escritório: {companyForm.accountingName || 'Não configurado'}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    E-mails: {companyForm.accountingEmails || 'Nenhum e-mail registrado'}
                  </Typography>
                  {companyForm.accountingLastSentAt && (
                    <Typography variant="caption" color="success.main" display="block" sx={{ mt: 1 }}>
                      Último envio realizado em: {new Date(companyForm.accountingLastSentAt).toLocaleString('pt-BR')}
                    </Typography>
                  )}
                </Box>

                <Button
                  variant="outlined"
                  color="secondary"
                  fullWidth
                  startIcon={<SendIcon />}
                  onClick={handleSendAccounting}
                  disabled={loading || !companyForm.accountingEmails}
                >
                  Enviar Lote Fiscal do Mês Selecionado ({selectedMonth}/{selectedYear})
                </Button>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Modal de Cancelamento de NFCom */}
      <Dialog open={cancelModalOpen} onClose={() => setCancelModalOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Cancelar NFCom (Modelo 62)</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Informe a justificativa regulatória para cancelamento desta NFCom junto à SEFAZ (mínimo 15 caracteres):
          </Typography>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Justificativa do Cancelamento"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            placeholder="Ex: Cancelamento decorrente de desistência do cliente dentro do prazo legal..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelModalOpen(false)}>Voltar</Button>
          <Button onClick={handleCancelNfcom} color="error" variant="contained" disabled={loading || cancelReason.length < 15}>
            Confirmar Cancelamento SEFAZ
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default FiscalDashboard;
