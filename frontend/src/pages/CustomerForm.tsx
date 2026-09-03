import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Container,
  Typography,
  TextField,
  Button,
  Grid,
  Card,
  CardContent,
  Box,
  AppBar,
  Toolbar,
  IconButton,
  Alert,
  FormControlLabel,
  Switch,
  CircularProgress,
  MenuItem,
} from '@mui/material';
import {
  Save as SaveIcon,
  Cancel as CancelIcon,
  ArrowBack as ArrowBackIcon,
} from '@mui/icons-material';
import { useAuth } from '../contexts/AuthContext';
import { customerService } from '../services/customerService';
import { toast } from 'react-toastify';
import api from '../services/api';
import { formatCPF, validateCPF, cleanCPF } from '../utils/cpfValidator';

const CustomerForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const isEditing = Boolean(id);

  const [formData, setFormData] = useState({
    name: '',
    cpf: '',
    email: '',
    phone: '',
    address: '',
    city: '',
    state: '',
    zipCode: '',
    active: true
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const brazilianStates = [
    { value: 'AC', label: 'Acre' },
    { value: 'AL', label: 'Alagoas' },
    { value: 'AP', label: 'Amapá' },
    { value: 'AM', label: 'Amazonas' },
    { value: 'BA', label: 'Bahia' },
    { value: 'CE', label: 'Ceará' },
    { value: 'DF', label: 'Distrito Federal' },
    { value: 'ES', label: 'Espírito Santo' },
    { value: 'GO', label: 'Goiás' },
    { value: 'MA', label: 'Maranhão' },
    { value: 'MT', label: 'Mato Grosso' },
    { value: 'MS', label: 'Mato Grosso do Sul' },
    { value: 'MG', label: 'Minas Gerais' },
    { value: 'PA', label: 'Pará' },
    { value: 'PB', label: 'Paraíba' },
    { value: 'PR', label: 'Paraná' },
    { value: 'PE', label: 'Pernambuco' },
    { value: 'PI', label: 'Piauí' },
    { value: 'RJ', label: 'Rio de Janeiro' },
    { value: 'RN', label: 'Rio Grande do Norte' },
    { value: 'RS', label: 'Rio Grande do Sul' },
    { value: 'RO', label: 'Rondônia' },
    { value: 'RR', label: 'Roraima' },
    { value: 'SC', label: 'Santa Catarina' },
    { value: 'SP', label: 'São Paulo' },
    { value: 'SE', label: 'Sergipe' },
    { value: 'TO', label: 'Tocantins' }
  ];

  useEffect(() => {
    if (isEditing) {
      fetchCustomer();
    }
  }, [id, isEditing]);

  const fetchCustomer = async () => {
    try {
      setLoading(true);
      const response = await api.get(`/customers/${id}`);
      setFormData(response.data);
      setError('');
    } catch (error) {
      console.error('Erro ao buscar cliente:', error);
      setError('Erro ao carregar dados do cliente');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    
    let formattedValue = value;
    
    // Formatação automática para campos específicos
    if (name === 'cpf') {
      formattedValue = formatCPF(value);
    } else if (name === 'phone') {
      formattedValue = formatPhoneInput(value);
    } else if (name === 'zipCode') {
      formattedValue = formatZipCodeInput(value);
    }
    
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : formattedValue
    }));
  };

  const formatPhoneInput = (value) => {
    // Remove tudo que não é dígito
    const numbers = value.replace(/\D/g, '');
    
    // Limita a 11 dígitos
    const limited = numbers.slice(0, 11);
    
    // Aplica a máscara
    if (limited.length <= 10) {
      return limited.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
    } else {
      return limited.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
    }
  };

  const formatZipCodeInput = (value) => {
    // Remove tudo que não é dígito
    const numbers = value.replace(/\D/g, '');
    
    // Limita a 8 dígitos
    const limited = numbers.slice(0, 8);
    
    // Aplica a máscara
    return limited.replace(/(\d{5})(\d{3})/, '$1-$2');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validações
    if (!formData.name.trim()) {
      setError('Nome é obrigatório');
      return;
    }
    
    if (!formData.cpf.trim()) {
      setError('CPF é obrigatório');
      return;
    }
    
    if (!validateCPF(formData.cpf)) {
      setError('CPF inválido');
      return;
    }
    
    if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      setError('Email inválido');
      return;
    }

    try {
      setLoading(true);
      setError('');
      setSuccess('');

      // Remove formatação antes de enviar
      const dataToSend = {
        ...formData,
        cpf: cleanCPF(formData.cpf),
        phone: formData.phone.replace(/\D/g, ''),
        zipCode: formData.zipCode.replace(/\D/g, '')
      };

      if (isEditing) {
        await api.put(`/customers/${id}`, dataToSend);
        setSuccess('Cliente atualizado com sucesso!');
      } else {
        await api.post('/customers', dataToSend);
        setSuccess('Cliente cadastrado com sucesso!');
      }

      setTimeout(() => {
        navigate('/customers');
      }, 2000);

    } catch (error) {
      console.error('Erro ao salvar cliente:', error);
      if (error.response?.data) {
        setError(error.response.data);
      } else {
        setError('Erro ao salvar cliente');
      }
    } finally {
      setLoading(false);
    }
  };

  if (loading && isEditing) {
    return (
      <Container maxWidth="lg" sx={{ py: 4, px: { xs: 1, sm: 2 } }}>
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  return (
    <Container 
      maxWidth="xl" 
      sx={{ 
        py: 3, 
        px: { xs: 2, sm: 3 },
      }}
    >
          {/* Botão Voltar */}
          <Box sx={{ mb: { xs: 2, sm: 3 } }}>
            <Button
              startIcon={<ArrowBackIcon sx={{ fontSize: { xs: 18, sm: 20 } }} />}
              onClick={() => navigate('/customers')}
              variant="outlined"
              color="primary"
              sx={{
                fontSize: { xs: '0.875rem', sm: '1rem' },
                py: { xs: 1, sm: 1.5 },
                px: { xs: 2, sm: 3 },
                borderRadius: { xs: 1.5, sm: 2 },
              }}
            >
              {isMobile ? 'Voltar' : 'Voltar para Lista de Clientes'}
            </Button>
          </Box>

          {/* Alertas */}
          {error && (
            <Alert 
              severity="error" 
              sx={{ 
                mb: { xs: 2, sm: 3 },
                fontSize: { xs: '0.875rem', sm: '1rem' },
              }}
            >
              {error}
            </Alert>
          )}

          {success && (
            <Alert 
              severity="success" 
              sx={{ 
                mb: { xs: 2, sm: 3 },
                fontSize: { xs: '0.875rem', sm: '1rem' },
              }}
            >
              {success}
            </Alert>
          )}

          {/* Formulário */}
          <Card
            sx={{
              borderRadius: { xs: 2, sm: 3 },
              boxShadow: theme.shadows[2],
            }}
          >
            <CardContent sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
              <Typography 
                variant={isMobile ? "h6" : "h5"} 
                component="h2" 
                gutterBottom
                sx={{
                  mb: { xs: 2, sm: 3 },
                  fontWeight: 'bold',
                  color: 'text.primary',
                }}
              >
                {isEditing ? 'Editar Cliente' : 'Cadastro de Novo Cliente'}
              </Typography>
              
              <Box component="form" onSubmit={handleSubmit} sx={{ mt: { xs: 2, sm: 3 } }}>
                <Grid container spacing={{ xs: 2, sm: 3 }}>
                  {/* Nome */}
                  <Grid item xs={12}>
                    <TextField
                      required
                      fullWidth
                      name="name"
                      label="Nome Completo"
                      value={formData.name}
                      onChange={handleInputChange}
                      placeholder="Digite o nome completo"
                      variant="outlined"
                      sx={{
                        '& .MuiInputBase-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                        '& .MuiInputLabel-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                      }}
                    />
                  </Grid>

                  {/* CPF */}
                  <Grid item xs={12} sm={6}>
                    <TextField
                      required
                      fullWidth
                      name="cpf"
                      label="CPF"
                      value={formData.cpf}
                      onChange={handleInputChange}
                      placeholder="000.000.000-00"
                      variant="outlined"
                      inputProps={{ maxLength: 14 }}
                      sx={{
                        '& .MuiInputBase-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                        '& .MuiInputLabel-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                      }}
                    />
                  </Grid>

                  {/* Email */}
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      name="email"
                      label="Email"
                      type="email"
                      value={formData.email}
                      onChange={handleInputChange}
                      placeholder="email@exemplo.com"
                      variant="outlined"
                      sx={{
                        '& .MuiInputBase-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                        '& .MuiInputLabel-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                      }}
                    />
                  </Grid>

                  {/* Telefone */}
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      name="phone"
                      label="Telefone"
                      value={formData.phone}
                      onChange={handleInputChange}
                      placeholder="(00) 00000-0000"
                      variant="outlined"
                      inputProps={{ maxLength: 15 }}
                      sx={{
                        '& .MuiInputBase-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                        '& .MuiInputLabel-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                      }}
                    />
                  </Grid>

                  {/* CEP */}
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      name="zipCode"
                      label="CEP"
                      value={formData.zipCode}
                      onChange={handleInputChange}
                      placeholder="00000-000"
                      variant="outlined"
                      inputProps={{ maxLength: 9 }}
                      sx={{
                        '& .MuiInputBase-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                        '& .MuiInputLabel-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                      }}
                    />
                  </Grid>

                  {/* Endereço */}
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      name="address"
                      label="Endereço"
                      value={formData.address}
                      onChange={handleInputChange}
                      placeholder="Rua, número, complemento"
                      variant="outlined"
                      sx={{
                        '& .MuiInputBase-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                        '& .MuiInputLabel-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                      }}
                    />
                  </Grid>

                  {/* Cidade */}
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      name="city"
                      label="Cidade"
                      value={formData.city}
                      onChange={handleInputChange}
                      placeholder="Nome da cidade"
                      variant="outlined"
                      sx={{
                        '& .MuiInputBase-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                        '& .MuiInputLabel-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                      }}
                    />
                  </Grid>

                  {/* Estado */}
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      select
                      name="state"
                      label="Estado"
                      value={formData.state}
                      onChange={handleInputChange}
                      variant="outlined"
                      sx={{
                        '& .MuiInputBase-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                        '& .MuiInputLabel-root': {
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                        },
                      }}
                    >
                      <MenuItem value="">Selecione o estado</MenuItem>
                      {brazilianStates.map((state) => (
                        <MenuItem key={state.value} value={state.value}>
                          {state.label}
                        </MenuItem>
                      ))}
                    </TextField>
                  </Grid>

                  {/* Status Ativo */}
                  {isEditing && (
                    <Grid item xs={12}>
                      <FormControlLabel
                        control={
                          <Checkbox
                            checked={formData.active}
                            onChange={handleInputChange}
                            name="active"
                            color="primary"
                            sx={{
                              '& .MuiSvgIcon-root': {
                                fontSize: { xs: 20, sm: 24 },
                              },
                            }}
                          />
                        }
                        label={
                          <Typography 
                            sx={{ 
                              fontSize: { xs: '0.875rem', sm: '1rem' },
                            }}
                          >
                            Cliente ativo
                          </Typography>
                        }
                      />
                    </Grid>
                  )}
                </Grid>

                {/* Botões */}
                <Box 
                  sx={{ 
                    display: 'flex', 
                    flexDirection: { xs: 'column', sm: 'row' },
                    justifyContent: 'flex-end', 
                    gap: { xs: 1.5, sm: 2 }, 
                    mt: { xs: 3, sm: 4 },
                  }}
                >
                  <Button
                    variant="outlined"
                    onClick={() => navigate('/customers')}
                    disabled={loading}
                    sx={{
                      fontSize: { xs: '0.875rem', sm: '1rem' },
                      py: { xs: 1.5, sm: 1.5 },
                      px: { xs: 3, sm: 4 },
                      borderRadius: { xs: 1.5, sm: 2 },
                      order: { xs: 2, sm: 1 },
                    }}
                  >
                    Cancelar
                  </Button>
                  <Button
                    type="submit"
                    variant="contained"
                    startIcon={loading ? <CircularProgress size={isMobile ? 16 : 20} /> : <SaveIcon sx={{ fontSize: { xs: 18, sm: 20 } }} />}
                    disabled={loading}
                    sx={{
                      fontSize: { xs: '0.875rem', sm: '1rem' },
                      py: { xs: 1.5, sm: 1.5 },
                      px: { xs: 3, sm: 4 },
                      borderRadius: { xs: 1.5, sm: 2 },
                      order: { xs: 1, sm: 2 },
                      fontWeight: 'medium',
                    }}
                  >
                    {loading ? 'Salvando...' : 'Salvar'}
                  </Button>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Container>
  );
};

export default CustomerForm;