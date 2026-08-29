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
} from '@mui/icons-material';
import fiscalService from '../../services/fiscalService';

const FiscalDashboard = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState({ type: '', message: '' });

  // Tab 0: Emissões NFCom
  const [records, setRecords] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [selectedRecordId, setSelectedRecordId] = useState(null);
  const [cancelReason, setCancelReason] = useState('');

  // Tab 1: Empresa & Configuração Fiscal
  const [companyForm, setCompanyForm] = useState({
    id: null,
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
    certificateExpiresAt: null,
    accountingName: 'Assessoria Contábil Silva & Associados',
    accountingEmails: 'fiscal@contabilidade.com.br',
    accountingSendDay: 5,
    accountingAutoSend: true,
    accountingLastSentAt: null,
  });

  const [gatewayConfig, setGatewayConfig] = useState({
    gatewayType: 'XINGUBIT_PAY',
    environment: 'HOMOLOGACAO',
    baseUrl: 'https://pay.xingubit.com.br',
    clientId: '',
    clientSecret: '',
  });

  // Upload Certificado
  const [certFile, setCertFile] = useState(null);
  const [certPassword, setCertPassword] = useState('');

  // Tab 2: Convênio 115/03
  const [convenioYear, setConvenioYear] = useState(new Date().getFullYear());
  const [convenioMonth, setConvenioMonth] = useState(new Date().getMonth() + 1);
  const [sendingAccounting, setSendingAccounting] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const [compRes, confRes, recRes] = await Promise.all([
        fiscalService.getActiveCompany().catch(() => null),
        fiscalService.getActiveConfig().catch(() => null),
        fiscalService.getRecords(0, 50).catch(() => ({ content: [] })),
      ]);

      if (compRes) setCompanyForm(compRes);
      if (confRes) setGatewayConfig(confRes);
      setRecords(recRes.content || []);
    } catch (err) {
      setFeedback({ type: 'error', message: 'Erro ao carregar dados fiscais: ' + err.message });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSaveCompany = async (e) => {
    if (e && e.preventDefault) e.preventDefault();
    try {
      setLoading(true);
      const saved = await fiscalService.saveCompany(companyForm);
      setCompanyForm(saved);
      setFeedback({ type: 'success', message: 'Dados cadastrais e fiscais da empresa salvos com sucesso!' });
    } catch (err) {
      setFeedback({ type: 'error', message: 'Falha ao salvar dados da empresa: ' + err.message });
    } finally {
      setLoading(false);
    }
  };

  const handleUploadCertificate = async (e) => {
    e.preventDefault();
    if (!certFile || !certPassword) {
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
    } catch (err) {
      setFeedback({ type: 'error', message: 'Erro no envio do certificado: ' + err.message });
    } finally {
      setLoading(false);
    }
  };

  const handleCancelNfcom = async () => {
    if (!cancelReason) {
      setFeedback({ type: 'warning', message: 'Informe a justificativa de cancelamento.' });
      return;
    }
    try {
      setLoading(true);
      await fiscalService.cancelNfcom(selectedRecordId, cancelReason);
      setCancelModalOpen(false);
      setCancelReason('');
      setFeedback({ type: 'success', message: 'NFCom cancelada com sucesso!' });
      loadData();
    } catch (err) {
      setFeedback({ type: 'error', message: 'Erro ao cancelar NFCom: ' + err.message });
    } finally {
      setLoading(false);
    }
  };

  const handleSendAccountingEmail = async () => {
    if (!companyForm.accountingEmails) {
      setFeedback({ type: 'warning', message: 'Cadastre ao menos um e-mail de contabilidade na aba Empresa.' });
      return;
    }

    try {
      setSendingAccounting(true);
      const res = await fiscalService.sendAccountingReport(convenioYear, convenioMonth);
      if (res.success) {
        setFeedback({ type: 'success', message: 'Lote do Convênio 115/03 enviado com sucesso via e-mail para a contabilidade!' });
        loadData();
      } else {
        setFeedback({ type: 'error', message: res.message || 'Falha ao enviar e-mail para contabilidade.' });
      }
    } catch (err) {
      setFeedback({ type: 'error', message: 'Erro ao disparar e-mail: ' + err.message });
    } finally {
      setSendingAccounting(false);
    }
  };

  const filteredRecords = records.filter((r) => {
    const term = searchTerm.toLowerCase();
    return (
      (r.chaveAcesso && r.chaveAcesso.toLowerCase().includes(term)) ||
      (r.numero && r.numero.toString().includes(term)) ||
      (r.status && r.status.toLowerCase().includes(term))
    );
  });

  return (
    <Box sx={{ p: 3, maxWidth: 1400, margin: '0 auto' }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <NfcomIcon color="primary" fontSize="large" /> Módulo Fiscal & NFCom (Modelo 62)
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Emissão de Notas Fiscais Eletrônicas de Telecomunicações (SCM), Certificados Digitais A1, Automação FreeMarker e Convênio ICMS 115/03.
          </Typography>
        </Box>
        <Button variant="outlined" startIcon={<RefreshIcon />} onClick={loadData} disabled={loading}>
          Atualizar
        </Button>
      </Box>

      {feedback.message && (
        <Alert severity={feedback.type || 'info'} sx={{ mb: 3 }} onClose={() => setFeedback({ type: '', message: '' })}>
          {feedback.message}
        </Alert>
      )}

      {/* Tabs */}
      <Paper sx={{ mb: 3 }}>
        <Tabs value={activeTab} onChange={(e, val) => setActiveTab(val)} indicatorColor="primary" textColor="primary">
          <Tab icon={<NfcomIcon />} iconPosition="start" label="Emissões NFCom (Modelo 62)" />
          <Tab icon={<CompanyIcon />} iconPosition="start" label="Empresa & Certificado A1" />
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
                  <TableCell><strong>Valor Total</strong></TableCell>
                  <TableCell><strong>Status SEFAZ</strong></TableCell>
                  <TableCell><strong>Data Autorização</strong></TableCell>
                  <TableCell align="right"><strong>Ações</strong></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredRecords.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                      Nenhuma NFCom emitida registrada até o momento.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredRecords.map((r) => (
                    <TableRow key={r.id} hover>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                          NFCom #{r.numero}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          Série {r.serie} • Mod. 62
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" sx={{ fontFamily: 'monospace', bgcolor: 'grey.100', p: 0.5, borderRadius: 1 }}>
                          {r.chaveAcesso || 'Em processamento...'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                          R$ {Number(r.valorTotal || 0).toFixed(2)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={r.status}
                          size="small"
                          color={r.status === 'AUTORIZADA' || r.status === 'EMITIDA' ? 'success' : r.status === 'CANCELADA' ? 'error' : 'default'}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption">
                          {r.dataAutorizacao ? new Date(r.dataAutorizacao).toLocaleString('pt-BR') : '-'}
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
                        {r.xmlAutorizado && (
                          <Tooltip title="Download XML Protocolado">
                            <IconButton color="secondary" size="small" component="a" href={r.xmlAutorizado} target="_blank">
                              <XmlIcon />
                            </IconButton>
                          </Tooltip>
                        )}
                        {r.status !== 'CANCELADA' && (
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
                <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <CompanyIcon color="primary" /> Dados Cadastrais & Fiscais do Provedor
                </Typography>
                <form onSubmit={handleSaveCompany}>
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
                        onChange={(e) => setCompanyForm({ ...companyForm, regimeTributario: e.target.value })}
                      >
                        <MenuItem value="SIMPLES_NACIONAL">Simples Nacional</MenuItem>
                        <MenuItem value="LUCRO_PRESUMIDO">Lucro Presumido</MenuItem>
                        <MenuItem value="LUCRO_REAL">Lucro Real</MenuItem>
                      </TextField>
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
                        onChange={(e) => setCompanyForm({ ...companyForm, nfcomAmbiente: e.target.value })}
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
                    <Grid item xs={12}>
                      <Button type="submit" variant="contained" startIcon={<SaveIcon />} disabled={loading}>
                        Salvar Dados Fiscais
                      </Button>
                    </Grid>
                  </Grid>
                </form>
              </CardContent>
            </Card>
          </Grid>

          {/* Certificado Digital A1 & Gateway */}
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
                  <Typography variant="caption" color="text.secondary">
                    {companyForm.hasCertificate && companyForm.certificateExpiresAt
                      ? `Válido até: ${new Date(companyForm.certificateExpiresAt).toLocaleDateString('pt-BR')}`
                      : 'Faça o upload do arquivo .pfx com a respectiva senha para habilitar a emissão de NFCom na SEFAZ.'}
                  </Typography>
                </Box>

                <form onSubmit={handleUploadCertificate}>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <Button variant="outlined" component="label" startIcon={<UploadIcon />}>
                      {certFile ? certFile.name : 'Selecionar Arquivo .pfx'}
                      <input
                        type="file"
                        hidden
                        accept=".pfx,.p12"
                        onChange={(e) => setCertFile(e.target.files[0])}
                      />
                    </Button>
                    <TextField
                      fullWidth
                      size="small"
                      type="password"
                      label="Senha do Certificado"
                      value={certPassword}
                      onChange={(e) => setCertPassword(e.target.value)}
                      placeholder="••••••••"
                    />
                    <Button
                      type="submit"
                      variant="contained"
                      color="secondary"
                      disabled={loading || !certFile || !certPassword}
                    >
                      Enviar Certificado ao Cofre
                    </Button>
                  </Box>
                </form>
              </CardContent>
            </Card>

            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ mb: 2 }}>
                  Gateway Fiscal Conectado
                </Typography>
                <Chip label="Xingubit Pay Fiscal (Nativo)" color="primary" sx={{ mb: 2 }} />
                <Typography variant="body2" color="text.secondary">
                  <strong>Base URL:</strong> {gatewayConfig.baseUrl}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  <strong>Protocolo:</strong> REST / OAuth2 (Tokens com expiração de 1h e guarda criptografada de chaves).
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* TAB 2: CONVÊNIO ICMS 115/03 & CONTABILIDADE */}
      {activeTab === 2 && (
        <Grid container spacing={3}>
          {/* Exportação Manual */}
          <Grid item xs={12} md={7}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" sx={{ mb: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <ZipIcon color="primary" /> Arquivos Magnéticos do Convênio ICMS 115/03
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Geração dos 4 arquivos oficiais padronizados pela SEFAZ: Arquivo Mestre (M), Itens (I), Destinatário (D) e Controle/Validação (C) com cálculo automático dos hashes MD5 cruzados.
                </Typography>

                <Grid container spacing={2} alignItems="center" sx={{ mb: 3 }}>
                  <Grid item xs={12} md={4}>
                    <TextField
                      fullWidth
                      size="small"
                      type="number"
                      label="Ano Fiscal"
                      value={convenioYear}
                      onChange={(e) => setConvenioYear(parseInt(e.target.value))}
                    />
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <TextField
                      fullWidth
                      size="small"
                      select
                      label="Mês de Referência"
                      value={convenioMonth}
                      onChange={(e) => setConvenioMonth(parseInt(e.target.value))}
                    >
                      {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                        <MenuItem key={m} value={m}>
                          {String(m).padStart(2, '0')} - {new Date(2026, m - 1, 1).toLocaleString('pt-BR', { month: 'long' })}
                        </MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <Button
                      fullWidth
                      variant="outlined"
                      color="primary"
                      startIcon={<DownloadIcon />}
                      component="a"
                      href={fiscalService.getConvenio115ExportUrl(convenioYear, convenioMonth)}
                      download
                    >
                      Baixar ZIP
                    </Button>
                  </Grid>
                </Grid>

                <Box sx={{ p: 2, bgcolor: 'grey.50', borderRadius: 2, border: '1px solid', borderColor: 'grey.200' }}>
                  <Typography variant="body2" sx={{ fontWeight: 'bold', mb: 1 }}>
                    📋 Pacote Magnético (.zip) Gerado:
                  </Typography>
                  <Typography variant="caption" component="div" sx={{ fontFamily: 'monospace', color: 'text.secondary' }}>
                    • PA{companyForm.cnpj.replace(/\D/g, '')}62001{String(convenioYear).slice(2)}{String(convenioMonth).padStart(2, '0')}N01.M (Mestre de Faturas)
                  </Typography>
                  <Typography variant="caption" component="div" sx={{ fontFamily: 'monospace', color: 'text.secondary' }}>
                    • PA{companyForm.cnpj.replace(/\D/g, '')}62001{String(convenioYear).slice(2)}{String(convenioMonth).padStart(2, '0')}N01.I (Itens Faturados)
                  </Typography>
                  <Typography variant="caption" component="div" sx={{ fontFamily: 'monospace', color: 'text.secondary' }}>
                    • PA{companyForm.cnpj.replace(/\D/g, '')}62001{String(convenioYear).slice(2)}{String(convenioMonth).padStart(2, '0')}N01.D (Destinatários)
                  </Typography>
                  <Typography variant="caption" component="div" sx={{ fontFamily: 'monospace', color: 'text.secondary' }}>
                    • PA{companyForm.cnpj.replace(/\D/g, '')}62001{String(convenioYear).slice(2)}{String(convenioMonth).padStart(2, '0')}N01.C (Controle com Hashes MD5)
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          {/* Automação & Assessoria Contábil */}
          <Grid item xs={12} md={5}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <EmailSentIcon color="primary" /> Envio Automático para Contabilidade
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  Configure o agendamento para o sistema enviar automaticamente o lote mensal compactado via template FreeMarker para sua assessoria contábil.
                </Typography>

                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      size="small"
                      label="Nome da Assessoria / Contador"
                      value={companyForm.accountingName || ''}
                      onChange={(e) => setCompanyForm({ ...companyForm, accountingName: e.target.value })}
                      placeholder="Ex: Contabilidade Silva & Associados"
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      size="small"
                      label="E-mails da Contabilidade (separados por vírgula)"
                      value={companyForm.accountingEmails || ''}
                      onChange={(e) => setCompanyForm({ ...companyForm, accountingEmails: e.target.value })}
                      placeholder="fiscal@contabilidade.com.br, socio@contabilidade.com.br"
                    />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField
                      fullWidth
                      size="small"
                      type="number"
                      label="Dia do Mês (Envio)"
                      value={companyForm.accountingSendDay || 5}
                      onChange={(e) => setCompanyForm({ ...companyForm, accountingSendDay: parseInt(e.target.value) || 5 })}
                      helperText="Ex: Todo dia 05 às 08:00"
                    />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={Boolean(companyForm.accountingAutoSend)}
                          onChange={(e) => setCompanyForm({ ...companyForm, accountingAutoSend: e.target.checked })}
                          color="primary"
                        />
                      }
                      label="Envio Automático Ativo"
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <Button
                      fullWidth
                      variant="outlined"
                      startIcon={<SaveIcon />}
                      onClick={handleSaveCompany}
                      disabled={loading}
                      sx={{ mb: 2 }}
                    >
                      Salvar Configurações da Contabilidade
                    </Button>
                  </Grid>
                </Grid>

                <Divider sx={{ my: 2 }} />

                <Box sx={{ p: 2, bgcolor: 'primary.50', borderRadius: 2, border: '1px solid', borderColor: 'primary.200' }}>
                  <Typography variant="caption" color="text.secondary" component="div" sx={{ mb: 1 }}>
                    Último envio registrado: {companyForm.accountingLastSentAt ? new Date(companyForm.accountingLastSentAt).toLocaleString('pt-BR') : 'Nenhum envio registrado'}
                  </Typography>
                  <Button
                    fullWidth
                    variant="contained"
                    color="primary"
                    startIcon={<SendIcon />}
                    onClick={handleSendAccountingEmail}
                    disabled={sendingAccounting || !companyForm.accountingEmails}
                  >
                    {sendingAccounting ? 'Transmitindo E-mail...' : '⚡ Disparar Fechamento por E-mail Agora'}
                  </Button>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Modal Cancelamento NFCom */}
      <Dialog open={cancelModalOpen} onClose={() => setCancelModalOpen(false)}>
        <DialogTitle>Cancelar NFCom Autorizada</DialogTitle>
        <DialogContent sx={{ pt: 1 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Esta ação registrará o evento de cancelamento oficial perante a SEFAZ. Informe a justificativa (mínimo 15 caracteres).
          </Typography>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Justificativa do Cancelamento"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            placeholder="Ex: Cancelamento por erro no valor faturado acordado com o cliente."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelModalOpen(false)}>Voltar</Button>
          <Button onClick={handleCancelNfcom} color="error" variant="contained" disabled={loading || cancelReason.length < 10}>
            Confirmar Cancelamento
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default FiscalDashboard;
