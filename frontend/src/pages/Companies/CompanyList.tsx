import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container,
  Typography,
  Box,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Grid,
  AppBar,
  Toolbar,
  useTheme,
  useMediaQuery,
  CircularProgress,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Menu as MenuIcon,
  ArrowBack as ArrowBackIcon,
  Business as BusinessIcon,
} from '@mui/icons-material';
import { Formik, Form, Field } from 'formik';
import * as Yup from 'yup';
import { toast } from 'react-toastify';
import { useAuth } from '../../contexts/AuthContext';
import Sidebar from '../../components/Sidebar';

// Constantes de largura da sidebar (mesmas do componente Sidebar)
const DRAWER_WIDTH = 280;
const DRAWER_WIDTH_MOBILE = 260;

const validationSchema = Yup.object({
  name: Yup.string().required('Nome da empresa é obrigatório'),
  cnpj: Yup.string(),
  email: Yup.string().email('Email inválido'),
  phone: Yup.string(),
  address: Yup.string(),
  website: Yup.string().url('URL inválida'),
});

const CompanyList = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile);
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedCompany, setSelectedCompany] = useState(null);
  const [isEditing, setIsEditing] = useState(false);

  // Calcular largura dinâmica da sidebar
  const sidebarWidth = isMobile ? DRAWER_WIDTH_MOBILE : DRAWER_WIDTH;

  // Ajustar sidebar baseado no tamanho da tela
  useEffect(() => {
    setSidebarOpen(!isMobile);
  }, [isMobile]);

  // Carregar empresas
  useEffect(() => {
    loadCompanies();
  }, []);

  const loadCompanies = async () => {
    try {
      setLoading(true);
      // Simulando dados para desenvolvimento
      const mockCompanies = [
        {
          id: 1,
          name: 'ISP Connect Ltda',
          cnpj: '12.345.678/0001-90',
          email: 'contato@ispconnect.com',
          phone: '(11) 99999-9999',
          address: 'Rua das Fibras, 123 - São Paulo/SP',
          website: 'https://www.ispconnect.com',
          createdAt: '2025-01-01',
        },
        {
          id: 2,
          name: 'Empresa Parceira ABC',
          cnpj: '98.765.432/0001-10',
          email: 'contato@abc.com',
          phone: '(11) 88888-8888',
          address: 'Av. Principal, 456 - São Paulo/SP',
          website: 'https://www.abc.com',
          createdAt: '2025-01-02',
        },
      ];
      setCompanies(mockCompanies);
    } catch (error) {
      console.error('Erro ao carregar empresas:', error);
      toast.error('Erro ao carregar empresas');
    } finally {
      setLoading(false);
    }
  };

  const handleSidebarToggle = () => {
    setSidebarOpen(!sidebarOpen);
  };

  const handleSidebarClose = () => {
    setSidebarOpen(false);
  };

  const handleAddCompany = () => {
    setSelectedCompany(null);
    setIsEditing(false);
    setDialogOpen(true);
  };

  const handleEditCompany = (companyToEdit) => {
    if (user.role !== 'ADMIN') {
      toast.error('Apenas administradores podem editar empresas');
      return;
    }
    setSelectedCompany(companyToEdit);
    setIsEditing(true);
    setDialogOpen(true);
  };

  const handleDeleteCompany = (companyToDelete) => {
    if (user.role !== 'ADMIN') {
      toast.error('Apenas administradores podem deletar empresas');
      return;
    }
    setSelectedCompany(companyToDelete);
    setDeleteDialogOpen(true);
  };

  const handleSubmit = async (values, { setSubmitting, resetForm }) => {
    try {
      if (isEditing) {
        // Simular atualização
        const updatedCompanies = companies.map(c => 
          c.id === selectedCompany.id 
            ? { ...c, ...values }
            : c
        );
        setCompanies(updatedCompanies);
        toast.success('Empresa atualizada com sucesso!');
      } else {
        // Simular criação
        const newCompany = {
          id: Date.now(),
          ...values,
          createdAt: new Date().toISOString().split('T')[0],
        };
        setCompanies([...companies, newCompany]);
        toast.success('Empresa criada com sucesso!');
      }
      
      setDialogOpen(false);
      resetForm();
    } catch (error) {
      console.error('Erro ao salvar empresa:', error);
      toast.error('Erro ao salvar empresa');
    } finally {
      setSubmitting(false);
    }
  };

  const confirmDelete = async () => {
    try {
      const updatedCompanies = companies.filter(c => c.id !== selectedCompany.id);
      setCompanies(updatedCompanies);
      toast.success('Empresa deletada com sucesso!');
      setDeleteDialogOpen(false);
    } catch (error) {
      console.error('Erro ao deletar empresa:', error);
      toast.error('Erro ao deletar empresa');
    }
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
            <IconButton
              color="inherit"
              aria-label="toggle sidebar"
              onClick={handleSidebarToggle}
              edge="start"
              sx={{ mr: 2 }}
            >
              <MenuIcon />
            </IconButton>
            <IconButton
              color="inherit"
              aria-label="voltar"
              onClick={() => navigate('/dashboard')}
              edge="start"
              sx={{ mr: 2 }}
            >
              <ArrowBackIcon />
            </IconButton>
            <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
              Gerenciamento de Empresas
            </Typography>
          </Toolbar>
        </AppBar>

        {/* Conteúdo */}
        <Container maxWidth="lg" sx={{ mt: 10, mb: 4, px: { xs: 1, sm: 2 } }}>
          <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h4" component="h1">
              Empresas
            </Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={handleAddCompany}
            >
              Nova Empresa
            </Button>
          </Box>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Nome</TableCell>
                    <TableCell>CNPJ</TableCell>
                    <TableCell>Email</TableCell>
                    <TableCell>Telefone</TableCell>
                    <TableCell>Data de Criação</TableCell>
                    <TableCell align="center">Ações</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {companies.map((company) => (
                    <TableRow key={company.id}>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <BusinessIcon sx={{ mr: 1, color: 'primary.main' }} />
                          {company.name}
                        </Box>
                      </TableCell>
                      <TableCell>{company.cnpj || '-'}</TableCell>
                      <TableCell>{company.email || '-'}</TableCell>
                      <TableCell>{company.phone || '-'}</TableCell>
                      <TableCell>{new Date(company.createdAt).toLocaleDateString('pt-BR')}</TableCell>
                      <TableCell align="center">
                        <IconButton
                          onClick={() => handleEditCompany(company)}
                          color="primary"
                          disabled={user.role !== 'ADMIN'}
                        >
                          <EditIcon />
                        </IconButton>
                        <IconButton
                          onClick={() => handleDeleteCompany(company)}
                          color="error"
                          disabled={user.role !== 'ADMIN'}
                        >
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Container>
      </Box>

      {/* Dialog de Criação/Edição */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {isEditing ? 'Editar Empresa' : 'Nova Empresa'}
        </DialogTitle>
        <Formik
          initialValues={{
            name: selectedCompany?.name || '',
            cnpj: selectedCompany?.cnpj || '',
            email: selectedCompany?.email || '',
            phone: selectedCompany?.phone || '',
            address: selectedCompany?.address || '',
            website: selectedCompany?.website || '',
          }}
          validationSchema={validationSchema}
          onSubmit={handleSubmit}
        >
          {({ errors, touched, isSubmitting }) => (
            <Form>
              <DialogContent>
                <Grid container spacing={2}>
                  <Grid item xs={12} md={8}>
                    <Field
                      as={TextField}
                      name="name"
                      label="Nome da Empresa"
                      fullWidth
                      error={touched.name && !!errors.name}
                      helperText={touched.name && errors.name}
                    />
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <Field
                      as={TextField}
                      name="cnpj"
                      label="CNPJ"
                      fullWidth
                      error={touched.cnpj && !!errors.cnpj}
                      helperText={touched.cnpj && errors.cnpj}
                    />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Field
                      as={TextField}
                      name="email"
                      label="Email"
                      type="email"
                      fullWidth
                      error={touched.email && !!errors.email}
                      helperText={touched.email && errors.email}
                    />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Field
                      as={TextField}
                      name="phone"
                      label="Telefone"
                      fullWidth
                      error={touched.phone && !!errors.phone}
                      helperText={touched.phone && errors.phone}
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <Field
                      as={TextField}
                      name="address"
                      label="Endereço"
                      fullWidth
                      multiline
                      rows={2}
                      error={touched.address && !!errors.address}
                      helperText={touched.address && errors.address}
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <Field
                      as={TextField}
                      name="website"
                      label="Website"
                      fullWidth
                      error={touched.website && !!errors.website}
                      helperText={touched.website && errors.website}
                    />
                  </Grid>
                </Grid>
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setDialogOpen(false)}>
                  Cancelar
                </Button>
                <Button type="submit" variant="contained" disabled={isSubmitting}>
                  {isSubmitting ? <CircularProgress size={24} /> : (isEditing ? 'Atualizar' : 'Criar')}
                </Button>
              </DialogActions>
            </Form>
          )}
        </Formik>
      </Dialog>

      {/* Dialog de Confirmação de Exclusão */}
      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Confirmar Exclusão</DialogTitle>
        <DialogContent>
          <Typography>
            Tem certeza que deseja excluir a empresa "{selectedCompany?.name}"?
            Esta ação não pode ser desfeita.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>
            Cancelar
          </Button>
          <Button onClick={confirmDelete} color="error" variant="contained">
            Excluir
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CompanyList;