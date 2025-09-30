import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Container,
  Typography,
  Grid,
  Card,
  CardContent,
  AppBar,
  Toolbar,
  IconButton,
  Box,
  useTheme,
  useMediaQuery,
  Button,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Checkbox,
  FormControlLabel,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Alert,
  CircularProgress,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  DialogContentText,
} from '@mui/material';
import {
  ExitToApp as ExitToAppIcon,
  Menu as MenuIcon,
  Add as AddIcon,
  Search as SearchIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  Refresh as RefreshIcon,
  Clear as ClearIcon,
  People as PeopleIcon,
} from '@mui/icons-material';
import { useAuth } from '../contexts/AuthContext';
import { toast } from 'react-toastify';
import Sidebar from '../components/Sidebar';
import api from '../services/api';
import { formatCPF } from '../utils/cpfValidator';

// Constantes de largura da sidebar (mesmas do componente Sidebar)
const DRAWER_WIDTH = 280;
const DRAWER_WIDTH_MOBILE = 260;

const CustomerList = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile);

  // Calcular largura dinâmica da sidebar
  const sidebarWidth = isMobile ? DRAWER_WIDTH_MOBILE : DRAWER_WIDTH;

  // Estados da página
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [searchType, setSearchType] = useState('name');
  const [showInactive, setShowInactive] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState({ open: false, customer: null });

  // Ajustar sidebar baseado no tamanho da tela
  useEffect(() => {
    setSidebarOpen(!isMobile);
  }, [isMobile]);

  useEffect(() => {
    fetchCustomers();
  }, [showInactive]);

  const handleSidebarToggle = () => {
    setSidebarOpen(!sidebarOpen);
  };

  const handleSidebarClose = () => {
    setSidebarOpen(false);
  };

  const handleLogout = async () => {
    try {
      await logout();
      toast.success('Logout realizado com sucesso!');
    } catch (error) {
      console.error('Erro no logout:', error);
      toast.error('Erro ao fazer logout');
    }
  };

  const fetchCustomers = async () => {
    try {
      setLoading(true);
      const endpoint = showInactive ? '/api/customers' : '/api/customers/active';
      const response = await api.get(endpoint);
      setCustomers(response.data);
      setError('');
    } catch (error) {
      console.error('Erro ao buscar clientes:', error);
      setError('Erro ao carregar lista de clientes');
      toast.error('Erro ao carregar lista de clientes');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!searchTerm.trim()) {
      fetchCustomers();
      return;
    }

    try {
      setLoading(true);
      const response = await api.get(`/api/customers/search/${searchType}`, {
        params: { [searchType]: searchTerm }
      });
      setCustomers(response.data);
      setError('');
    } catch (error) {
      console.error('Erro ao buscar clientes:', error);
      setError('Erro ao buscar clientes');
      toast.error('Erro ao buscar clientes');
    } finally {
      setLoading(false);
    }
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setSearchType('name');
    setShowInactive(false);
    fetchCustomers();
  };

  const handleDeleteClick = (customer) => {
    setDeleteDialog({ open: true, customer });
  };

  const handleDeleteConfirm = async () => {
    const { customer } = deleteDialog;
    try {
      await api.delete(`/api/customers/${customer.id}`);
      toast.success('Cliente excluído com sucesso!');
      fetchCustomers();
    } catch (error) {
      console.error('Erro ao excluir cliente:', error);
      toast.error('Erro ao excluir cliente');
    } finally {
      setDeleteDialog({ open: false, customer: null });
    }
  };

  const handleDeleteCancel = () => {
    setDeleteDialog({ open: false, customer: null });
  };

  const handleToggleActive = async (id, isActive) => {
    try {
      const endpoint = isActive ? 'deactivate' : 'activate';
      await api.patch(`/api/customers/${id}/${endpoint}`);
      toast.success(`Cliente ${isActive ? 'desativado' : 'ativado'} com sucesso!`);
      fetchCustomers();
    } catch (error) {
      console.error('Erro ao alterar status do cliente:', error);
      toast.error('Erro ao alterar status do cliente');
    }
  };

  const formatPhone = (phone) => {
    if (!phone) return '';
    if (phone.length === 11) {
      return phone.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
    }
    if (phone.length === 10) {
      return phone.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
    }
    return phone;
  };

  return (
    <Box sx={{ display: 'flex' }}>
      {/* Sidebar */}
      <Sidebar
        open={sidebarOpen}
        onClose={handleSidebarClose}
        onToggle={handleSidebarToggle}
      />

      {/* Conteúdo Principal */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          transition: theme.transitions.create(['margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
          marginLeft: 0,
        }}
      >
        {/* AppBar */}
        <AppBar
          position="fixed"
          sx={{
            zIndex: theme.zIndex.drawer + 1,
            transition: theme.transitions.create(['margin', 'width'], {
              easing: theme.transitions.easing.sharp,
              duration: theme.transitions.duration.leavingScreen,
            }),
            marginLeft: 0,
            width: '100%',
          }}
        >
          <Toolbar>
            {/* Botão do Menu */}
            {(!sidebarOpen || isMobile) && (
              <IconButton
                color="inherit"
                aria-label="Abrir menu"
                onClick={handleSidebarToggle}
                edge="start"
                sx={{ mr: 2 }}
              >
                <MenuIcon />
              </IconButton>
            )}

            <PeopleIcon sx={{ mr: 1 }} />
            <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
              Gestão de Clientes
            </Typography>

            <Typography variant="body2" sx={{ mr: 2 }}>
              Olá, {user?.name || 'Usuário'}!
            </Typography>

            <IconButton
              color="inherit"
              onClick={handleLogout}
              aria-label="Sair do sistema"
            >
              <ExitToAppIcon />
            </IconButton>
          </Toolbar>
        </AppBar>

        {/* Conteúdo da Página */}
        <Container 
          maxWidth="xl" 
          sx={{ 
            mt: 10, // Margem superior para compensar a AppBar fixa
            mb: 4,
            px: { xs: 1, sm: 2 }
          }}
        >
          {/* Cabeçalho da Página */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Typography variant="h4" gutterBottom sx={{ mb: 0 }}>
              Lista de Clientes
            </Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => navigate('/customers/new')}
              sx={{
                borderRadius: 2,
                textTransform: 'none',
                px: 3,
              }}
            >
              Novo Cliente
            </Button>
          </Box>

          {/* Card de Filtros e Busca */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Filtros e Busca
              </Typography>
              <Grid container spacing={2} alignItems="end">
                <Grid item xs={12} sm={6} md={3}>
                  <FormControl fullWidth size="small">
                    <InputLabel>Buscar por</InputLabel>
                    <Select
                      value={searchType}
                      label="Buscar por"
                      onChange={(e) => setSearchType(e.target.value)}
                    >
                      <MenuItem value="name">Nome</MenuItem>
                      <MenuItem value="cpf">CPF</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} sm={6} md={4}>
                  <TextField
                    fullWidth
                    size="small"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    placeholder={searchType === 'name' ? 'Digite o nome...' : 'Digite o CPF...'}
                    onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                  />
                </Grid>
                <Grid item xs={12} sm={6} md={2}>
                  <Button
                    fullWidth
                    variant="contained"
                    startIcon={<SearchIcon />}
                    onClick={handleSearch}
                    sx={{ textTransform: 'none' }}
                  >
                    Buscar
                  </Button>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button
                      variant="outlined"
                      startIcon={<RefreshIcon />}
                      onClick={fetchCustomers}
                      sx={{ textTransform: 'none' }}
                    >
                      Atualizar
                    </Button>
                    <Button
                      variant="outlined"
                      startIcon={<ClearIcon />}
                      onClick={handleClearFilters}
                      sx={{ textTransform: 'none' }}
                    >
                      Limpar
                    </Button>
                  </Box>
                </Grid>
                <Grid item xs={12}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={showInactive}
                        onChange={(e) => setShowInactive(e.target.checked)}
                      />
                    }
                    label="Mostrar clientes inativos"
                  />
                </Grid>
              </Grid>
            </CardContent>
          </Card>

          {/* Mensagem de Erro */}
          {error && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {error}
            </Alert>
          )}

          {/* Card da Tabela */}
          <Card>
            <CardContent sx={{ p: 0 }}>
              {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                  <CircularProgress />
                </Box>
              ) : (
                <>
                  <TableContainer>
                    <Table>
                      <TableHead>
                        <TableRow sx={{ bgcolor: 'grey.50' }}>
                          <TableCell><strong>Nome</strong></TableCell>
                          <TableCell><strong>CPF</strong></TableCell>
                          <TableCell><strong>Email</strong></TableCell>
                          <TableCell><strong>Telefone</strong></TableCell>
                          <TableCell><strong>Cidade</strong></TableCell>
                          <TableCell><strong>Status</strong></TableCell>
                          <TableCell align="center"><strong>Ações</strong></TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {customers.length === 0 ? (
                          <TableRow>
                            <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                              <Typography variant="body1" color="text.secondary">
                                Nenhum cliente encontrado
                              </Typography>
                            </TableCell>
                          </TableRow>
                        ) : (
                          customers.map((customer) => (
                            <TableRow 
                              key={customer.id} 
                              hover
                              sx={{ '&:hover': { bgcolor: 'action.hover' } }}
                            >
                              <TableCell>
                                <Typography variant="body2" fontWeight="medium">
                                  {customer.name}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <Typography variant="body2">
                                  {formatCPF(customer.cpf)}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <Typography variant="body2">
                                  {customer.email || '-'}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <Typography variant="body2">
                                  {formatPhone(customer.phone) || '-'}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <Typography variant="body2">
                                  {customer.city || '-'}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <Chip
                                  label={customer.active ? 'Ativo' : 'Inativo'}
                                  color={customer.active ? 'success' : 'error'}
                                  size="small"
                                  variant="outlined"
                                />
                              </TableCell>
                              <TableCell align="center">
                                <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                                  <Tooltip title="Editar">
                                    <IconButton
                                      size="small"
                                      color="primary"
                                      onClick={() => navigate(`/customers/edit/${customer.id}`)}
                                    >
                                      <EditIcon fontSize="small" />
                                    </IconButton>
                                  </Tooltip>
                                  <Tooltip title={customer.active ? 'Desativar' : 'Ativar'}>
                                    <IconButton
                                      size="small"
                                      color={customer.active ? 'warning' : 'success'}
                                      onClick={() => handleToggleActive(customer.id, customer.active)}
                                    >
                                      {customer.active ? 
                                        <VisibilityOffIcon fontSize="small" /> : 
                                        <VisibilityIcon fontSize="small" />
                                      }
                                    </IconButton>
                                  </Tooltip>
                                  <Tooltip title="Excluir">
                                    <IconButton
                                      size="small"
                                      color="error"
                                      onClick={() => handleDeleteClick(customer)}
                                    >
                                      <DeleteIcon fontSize="small" />
                                    </IconButton>
                                  </Tooltip>
                                </Box>
                              </TableCell>
                            </TableRow>
                          ))
                        )}
                      </TableBody>
                    </Table>
                  </TableContainer>

                  {/* Rodapé da Tabela */}
                  {customers.length > 0 && (
                    <Box sx={{ p: 2, bgcolor: 'grey.50', borderTop: 1, borderColor: 'divider' }}>
                      <Typography variant="body2" color="text.secondary">
                        Total: {customers.length} cliente(s) encontrado(s)
                      </Typography>
                    </Box>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </Container>
      </Box>

      {/* Dialog de Confirmação de Exclusão */}
      <Dialog
        open={deleteDialog.open}
        onClose={handleDeleteCancel}
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-description"
      >
        <DialogTitle id="delete-dialog-title">
          Confirmar Exclusão
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            Tem certeza que deseja excluir o cliente "{deleteDialog.customer?.name}"?
            Esta ação não pode ser desfeita.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} color="primary">
            Cancelar
          </Button>
          <Button onClick={handleDeleteConfirm} color="error" variant="contained">
            Excluir
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CustomerList;