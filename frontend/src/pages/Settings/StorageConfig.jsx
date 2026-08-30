import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  CardHeader,
  Grid,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  CircularProgress,
  Alert,
  Snackbar,
  Switch,
  FormControlLabel,
  Divider,
  Chip,
  IconButton,
  InputAdornment,
  Paper,
  Tooltip
} from '@mui/material';
import {
  CloudQueue as CloudIcon,
  Storage as StorageIcon,
  CheckCircle as CheckCircleIcon,
  ErrorOutline as ErrorIcon,
  Visibility,
  VisibilityOff,
  Speed as SpeedIcon,
  Save as SaveIcon,
  Bolt as BoltIcon,
  Folder as FolderIcon
} from '@mui/icons-material';
import storageConfigService from '../../services/storageConfigService';

const PRESETS = {
  SEAWEEDFS_LOCAL: {
    name: 'SeaweedFS Local (Padrão Docker / On-Premise)',
    storageType: 'S3',
    endpointUrl: 'http://localhost:8333',
    bucketName: 'isperp-files',
    region: 'us-east-1',
    pathStyleAccess: true,
    description: 'Armazenamento S3 local leve e ultrarrápido rodando diretamente no Docker Compose.'
  },
  AWS_S3: {
    name: 'Amazon Web Services (AWS S3)',
    storageType: 'S3',
    endpointUrl: 'https://s3.amazonaws.com',
    bucketName: 'isperp-files',
    region: 'us-east-1',
    pathStyleAccess: false,
    description: 'Armazenamento em nuvem com alta durabilidade e criptografia gerenciada pela AWS.'
  },
  CLOUDFLARE_R2: {
    name: 'Cloudflare R2 Storage',
    storageType: 'S3',
    endpointUrl: 'https://<ACCOUNT_ID>.r2.cloudflarestorage.com',
    bucketName: 'isperp-files',
    region: 'auto',
    pathStyleAccess: true,
    description: 'Compatível com S3 com zero taxa de transferência/egress.'
  },
  CUSTOM_S3: {
    name: 'Servidor S3 Customizado / Outro S3',
    storageType: 'S3',
    endpointUrl: 'http://meu-storage:9000',
    bucketName: 'isperp-files',
    region: 'us-east-1',
    pathStyleAccess: true,
    description: 'Conexão flexível com qualquer storage compatível com a API S3 (Wasabi, Garage, etc.).'
  },
  LOCAL_DISK: {
    name: 'Disco Local do Servidor (Filesystem)',
    storageType: 'LOCAL',
    endpointUrl: '',
    bucketName: 'local',
    region: 'local',
    pathStyleAccess: true,
    description: 'Armazena arquivos diretamente na pasta /uploads do sistema de arquivos local.'
  }
};

const StorageConfig = () => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [showSecretKey, setShowSecretKey] = useState(false);

  const [formData, setFormData] = useState({
    provider: 'SEAWEEDFS_LOCAL',
    storageType: 'S3',
    endpointUrl: 'http://localhost:8333',
    bucketName: 'isperp-files',
    region: 'us-east-1',
    accessKey: '',
    secretKey: '',
    maskedSecretKey: '',
    pathStyleAccess: true,
    isActive: true
  });

  const [testResult, setTestResult] = useState(null);
  const [toast, setToast] = useState({ open: false, message: '', severity: 'success' });

  useEffect(() => {
    loadConfig();
  }, []);

  const loadConfig = async () => {
    try {
      setLoading(true);
      const data = await storageConfigService.getActiveConfig();
      if (data) {
        setFormData({
          provider: data.provider || 'SEAWEEDFS_LOCAL',
          storageType: data.storageType || 'S3',
          endpointUrl: data.endpointUrl || 'http://localhost:8333',
          bucketName: data.bucketName || 'isperp-files',
          region: data.region || 'us-east-1',
          accessKey: data.accessKey || '',
          secretKey: '',
          maskedSecretKey: data.maskedSecretKey || '',
          pathStyleAccess: data.pathStyleAccess !== undefined ? data.pathStyleAccess : true,
          isActive: data.isActive !== undefined ? data.isActive : true
        });
      }
    } catch (err) {
      console.error('Erro ao carregar configurações de storage:', err);
      setToast({
        open: true,
        message: 'Erro ao carregar configurações de armazenamento.',
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleProviderChange = (e) => {
    const selectedProvider = e.target.value;
    const preset = PRESETS[selectedProvider];
    if (preset) {
      setFormData(prev => ({
        ...prev,
        provider: selectedProvider,
        storageType: preset.storageType,
        endpointUrl: preset.endpointUrl,
        bucketName: preset.bucketName,
        region: preset.region,
        pathStyleAccess: preset.pathStyleAccess
      }));
    } else {
      setFormData(prev => ({ ...prev, provider: selectedProvider }));
    }
    setTestResult(null);
  };

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleTestConnection = async () => {
    try {
      setTesting(true);
      setTestResult(null);
      const result = await storageConfigService.testConnection(formData);
      setTestResult(result);
      if (result.success) {
        setToast({
          open: true,
          message: `Conexão bem-sucedida (${result.latencyMs} ms)!`,
          severity: 'success'
        });
      } else {
        setToast({
          open: true,
          message: result.message || 'Falha no teste de conexão S3.',
          severity: 'error'
        });
      }
    } catch (err) {
      console.error('Erro no teste de conexão:', err);
      setTestResult({
        success: false,
        message: err.response?.data?.userMessage || err.message || 'Erro de comunicação.',
        latencyMs: 0
      });
      setToast({
        open: true,
        message: 'Falha ao executar teste de conexão.',
        severity: 'error'
      });
    } finally {
      setTesting(false);
    }
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      const updated = await storageConfigService.saveConfig(formData);
      setFormData(prev => ({
        ...prev,
        maskedSecretKey: updated.maskedSecretKey,
        secretKey: ''
      }));
      setToast({
        open: true,
        message: 'Configurações de armazenamento salvas com sucesso!',
        severity: 'success'
      });
    } catch (err) {
      console.error('Erro ao salvar configuração:', err);
      setToast({
        open: true,
        message: err.response?.data?.userMessage || 'Erro ao salvar configuração.',
        severity: 'error'
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <CircularProgress />
      </Box>
    );
  }

  const isLocalDisk = formData.provider === 'LOCAL_DISK' || formData.storageType === 'LOCAL';

  return (
    <Box sx={{ p: { xs: 2, md: 4 }, maxWidth: 1200, margin: '0 auto' }}>
      {/* Header */}
      <Box sx={{ mb: 4, display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 0.5 }}>
            <CloudIcon color="primary" sx={{ fontSize: 36 }} />
            <Typography variant="h4" component="h1" fontWeight="bold">
              Armazenamento & S3
            </Typography>
          </Box>
          <Typography variant="body1" color="text.secondary">
            Gerenciamento do storage para fotos de instalação, comprovantes, termos e documentos fiscais (NFCom).
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="outlined"
            color="primary"
            startIcon={testing ? <CircularProgress size={20} /> : <BoltIcon />}
            onClick={handleTestConnection}
            disabled={testing || saving}
          >
            {testing ? 'Testando...' : 'Testar Conexão'}
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={saving ? <CircularProgress size={20} color="inherit" /> : <SaveIcon />}
            onClick={handleSave}
            disabled={saving || testing}
          >
            {saving ? 'Salvando...' : 'Salvar Configuração'}
          </Button>
        </Box>
      </Box>

      {/* Test Feedback Banner */}
      {testResult && (
        <Alert
          severity={testResult.success ? 'success' : 'error'}
          icon={testResult.success ? <CheckCircleIcon /> : <ErrorIcon />}
          sx={{ mb: 3 }}
          action={
            testResult.latencyMs > 0 && (
              <Chip
                icon={<SpeedIcon />}
                label={`${testResult.latencyMs} ms`}
                color={testResult.success ? 'success' : 'error'}
                size="small"
                variant="outlined"
              />
            )
          }
        >
          <Typography variant="subtitle2" fontWeight="bold">
            {testResult.message}
          </Typography>
          {testResult.details && (
            <Typography variant="caption" display="block">
              {testResult.details}
            </Typography>
          )}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Provedor de Armazenamento */}
        <Grid item xs={12} md={5}>
          <Card elevation={2} sx={{ height: '100%' }}>
            <CardHeader
              avatar={<StorageIcon color="primary" />}
              title="Provedor de Armazenamento"
              subheader="Escolha entre o SeaweedFS local ou serviços de nuvem"
            />
            <Divider />
            <CardContent>
              <FormControl fullWidth sx={{ mb: 3 }}>
                <InputLabel>Selecione o Provedor</InputLabel>
                <Select
                  value={formData.provider}
                  label="Selecione o Provedor"
                  onChange={handleProviderChange}
                >
                  <MenuItem value="SEAWEEDFS_LOCAL">
                    🐳 SeaweedFS Local (Padrão Docker / On-Premise)
                  </MenuItem>
                  <MenuItem value="AWS_S3">
                    ☁️ Amazon Web Services (AWS S3)
                  </MenuItem>
                  <MenuItem value="CLOUDFLARE_R2">
                    ⚡ Cloudflare R2 Storage
                  </MenuItem>
                  <MenuItem value="CUSTOM_S3">
                    🛠️ Servidor S3 Customizado (Wasabi / MinIO / Garage)
                  </MenuItem>
                  <MenuItem value="LOCAL_DISK">
                    💾 Disco Local do Servidor (Filesystem)
                  </MenuItem>
                </Select>
              </FormControl>

              <Paper variant="outlined" sx={{ p: 2, bgcolor: 'background.default', borderRadius: 2 }}>
                <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
                  Sobre este Provedor:
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {PRESETS[formData.provider]?.description || 'Provedor customizado.'}
                </Typography>
              </Paper>

              <Box sx={{ mt: 3 }}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.isActive}
                      onChange={(e) => handleChange('isActive', e.target.checked)}
                      color="primary"
                    />
                  }
                  label="Armazenamento Ativo"
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Parâmetros de Conexão S3 */}
        <Grid item xs={12} md={7}>
          <Card elevation={2}>
            <CardHeader
              avatar={isLocalDisk ? <FolderIcon color="primary" /> : <CloudIcon color="primary" />}
              title="Parâmetros de Conexão"
              subheader={isLocalDisk ? 'Configurações de diretório local' : 'Configurações da API S3'}
            />
            <Divider />
            <CardContent>
              {isLocalDisk ? (
                <Alert severity="info" sx={{ mb: 2 }}>
                  O armazenamento está operando diretamente no disco do servidor. Não são necessárias chaves de autenticação S3.
                </Alert>
              ) : (
                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      label="Endpoint URL"
                      value={formData.endpointUrl}
                      onChange={(e) => handleChange('endpointUrl', e.target.value)}
                      placeholder="http://localhost:8333 ou https://s3.amazonaws.com"
                      helperText="URL base do endpoint S3 (para SeaweedFS local: http://localhost:8333 ou http://seaweedfs:8333 no Docker)"
                    />
                  </Grid>

                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      label="Nome do Bucket"
                      value={formData.bucketName}
                      onChange={(e) => handleChange('bucketName', e.target.value)}
                      placeholder="isperp-files"
                      helperText="O bucket é criado automaticamente se não existir."
                    />
                  </Grid>

                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      label="Região (Region)"
                      value={formData.region}
                      onChange={(e) => handleChange('region', e.target.value)}
                      placeholder="us-east-1 ou auto"
                    />
                  </Grid>

                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      label="Access Key (Chave de Acesso)"
                      value={formData.accessKey}
                      onChange={(e) => handleChange('accessKey', e.target.value)}
                      placeholder="Deixe em branco para SeaweedFS sem auth"
                    />
                  </Grid>

                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      label="Secret Key (Chave Secreta)"
                      type={showSecretKey ? 'text' : 'password'}
                      value={formData.secretKey}
                      onChange={(e) => handleChange('secretKey', e.target.value)}
                      placeholder={formData.maskedSecretKey ? `Chave salva: ${formData.maskedSecretKey} (deixe em branco para manter)` : 'Digite a Secret Key'}
                      helperText={formData.maskedSecretKey ? `Chave atual no servidor: ${formData.maskedSecretKey}` : ''}
                      InputProps={{
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton
                              onClick={() => setShowSecretKey(!showSecretKey)}
                              edge="end"
                            >
                              {showSecretKey ? <VisibilityOff /> : <Visibility />}
                            </IconButton>
                          </InputAdornment>
                        )
                      }}
                    />
                  </Grid>

                  <Grid item xs={12}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={formData.pathStyleAccess}
                          onChange={(e) => handleChange('pathStyleAccess', e.target.checked)}
                          color="primary"
                        />
                      }
                      label={
                        <Box>
                          <Typography variant="body2" fontWeight="medium">
                            Path-Style Access
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            (Habilitado para SeaweedFS, Cloudflare R2 e MinIO; desabilitado para AWS S3 padrão)
                          </Typography>
                        </Box>
                      }
                    />
                  </Grid>
                </Grid>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Toast Feedback */}
      <Snackbar
        open={toast.open}
        autoHideDuration={5000}
        onClose={() => setToast(prev => ({ ...prev, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert
          onClose={() => setToast(prev => ({ ...prev, open: false }))}
          severity={toast.severity}
          sx={{ width: '100%' }}
        >
          {toast.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default StorageConfig;
