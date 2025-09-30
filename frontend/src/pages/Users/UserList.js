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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Grid,
  AppBar,
  Toolbar,
  useTheme,
  useMediaQuery,
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Menu as MenuIcon,
  ArrowBack as ArrowBackIcon,
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
  name: Yup.string().required('Nome é obrigatório'),
  email: Yup.string().email('Email inválido').required('Email é obrigatório'),
  role: Yup.string().required('Perfil é obrigatório'),
  password: Yup.string().when('isEditing', {
    is: false,
    then: (schema) => schema.min(6, 'Senha deve ter pelo menos 6 caracteres').required('Senha é obrigatória'),
    otherwise: (schema) => schema.min(6, 'Senha deve ter pelo menos 6 caracteres'),
  }),
});

const UserList = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [isEditing, setIsEditing] = useState(false);

  // Calcular largura dinâmica da sidebar
  const sidebarWidth = isMobile ? DRAWER_WIDTH_MOBILE : DRAWER_WIDTH;

  // Ajustar sidebar baseado no tamanho da tela
  useEffect(() => {
    setSidebarOpen(!isMobile);
  }, [isMobile]);

  // Carregar usuários
  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    try {
      setLoading(true);
      // Simulando dados para desenvolvimento
      const mockUsers = [
        {
          id: 1,
          name: 'Administrador',
          email: 'admin@isperp.com',
          role: 'ADMIN',
          createdAt: '2025-01-01',
        },
        {
          id: 2,
          name: 'João Silva',
          email: 'joao@empresa.com',
          role: 'USER',
          createdAt: '2025-01-02',
        },
      ];
      setUsers(mockUsers);
    } catch (error) {
      console.error('Erro ao carregar usuários:', error);
      toast.error('Erro ao carregar usuários');
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

  const handleAddUser = () => {
    setSelectedUser(null);
    setIsEditing(false);
    setDialogOpen(true);
  };

  const handleEditUser = (userToEdit) => {
    if (user.role !== 'ADMIN') {
      toast.error('Apenas administradores podem editar usuários');
      return;
    }
    setSelectedUser(userToEdit);
    setIsEditing(true);
    setDialogOpen(true);
  };

  const handleDeleteUser = (userToDelete) => {
    if (user.role !== 'ADMIN') {
      toast.error('Apenas administradores podem deletar usuários');
      return;
    }
    setSelectedUser(userToDelete);
    setDeleteDialogOpen(true);
  };

  const handleSubmit = async (values, { setSubmitting, resetForm }) => {
    try {
      if (isEditing) {
        // Simular atualização
        const updatedUsers = users.map(u => 
          u.id === selectedUser.id 
            ? { ...u, name: values.name, email: values.email, role: values.role }
            : u
        );
        setUsers(updatedUsers);
        toast.success('Usuário atualizado com sucesso!');
      } else {
        // Simular criação
        const newUser = {
          id: Date.now(),
          name: values.name,
          email: values.email,
          role: values.role,
          createdAt: new Date().toISOString().split('T')[0],
        };
        setUsers([...users, newUser]);
        toast.success('Usuário criado com sucesso!');
      }
      
      setDialogOpen(false);
      resetForm();
    } catch (error) {
      console.error('Erro ao salvar usuário:', error);
      toast.error('Erro ao salvar usuário');
    } finally {
      setSubmitting(false);
    }
  };

  const confirmDelete = async () => {
    try {
      const updatedUsers = users.filter(u => u.id !== selectedUser.id);
      setUsers(updatedUsers);
      toast.success('Usuário deletado com sucesso!');
      setDeleteDialogOpen(false);
    } catch (error) {
      console.error('Erro ao deletar usuário:', error);
      toast.error('Erro ao deletar usuário');
    }
  };

  const getRoleLabel = (role) => {
    switch (role) {
      case 'ADMIN':
        return 'Administrador';
      case 'USER':
        return 'Usuário';
      default:
        return role;
    }
  };

  const getRoleColor = (role) => {
    switch (role) {
      case 'ADMIN':
        return 'error';
      case 'USER':
        return 'primary';
      default:
        return 'default';
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
              Gerenciamento de Usuários
            </Typography>
          </Toolbar>
        </AppBar>

        {/* Conteúdo */}
        <Container maxWidth="lg" sx={{ mt: 10, mb: 4, px: { xs: 1, sm: 2 } }}>
          <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h4" component="h1">
              Usuários
            </Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={handleAddUser}
            >
              Novo Usuário
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
                    <TableCell>Email</TableCell>
                    <TableCell>Perfil</TableCell>
                    <TableCell>Data de Criação</TableCell>
                    <TableCell align="center">Ações</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {users.map((userItem) => (
                    <TableRow key={userItem.id}>
                      <TableCell>{userItem.name}</TableCell>
                      <TableCell>{userItem.email}</TableCell>
                      <TableCell>
                        <Chip
                          label={getRoleLabel(userItem.role)}
                          color={getRoleColor(userItem.role)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>{new Date(userItem.createdAt).toLocaleDateString('pt-BR')}</TableCell>
                      <TableCell align="center">
                        <IconButton
                          onClick={() => handleEditUser(userItem)}
                          color="primary"
                          disabled={user.role !== 'ADMIN'}
                        >
                          <EditIcon />
                        </IconButton>
                        <IconButton
                          onClick={() => handleDeleteUser(userItem)}
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
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {isEditing ? 'Editar Usuário' : 'Novo Usuário'}
        </DialogTitle>
        <Formik
          initialValues={{
            name: selectedUser?.name || '',
            email: selectedUser?.email || '',
            role: selectedUser?.role || 'USER',
            password: '',
            isEditing: isEditing,
          }}
          validationSchema={validationSchema}
          onSubmit={handleSubmit}
        >
          {({ errors, touched, isSubmitting }) => (
            <Form>
              <DialogContent>
                <Grid container spacing={2}>
                  <Grid item xs={12}>
                    <Field
                      as={TextField}
                      name="name"
                      label="Nome Completo"
                      fullWidth
                      error={touched.name && !!errors.name}
                      helperText={touched.name && errors.name}
                    />
                  </Grid>
                  <Grid item xs={12}>
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
                  <Grid item xs={12}>
                    <FormControl fullWidth>
                      <InputLabel>Perfil</InputLabel>
                      <Field
                        as={Select}
                        name="role"
                        label="Perfil"
                        error={touched.role && !!errors.role}
                      >
                        <MenuItem value="USER">Usuário</MenuItem>
                        <MenuItem value="ADMIN">Administrador</MenuItem>
                      </Field>
                    </FormControl>
                  </Grid>
                  <Grid item xs={12}>
                    <Field
                      as={TextField}
                      name="password"
                      label={isEditing ? "Nova Senha (deixe em branco para manter)" : "Senha"}
                      type="password"
                      fullWidth
                      error={touched.password && !!errors.password}
                      helperText={touched.password && errors.password}
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
            Tem certeza que deseja excluir o usuário "{selectedUser?.name}"?
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

export default UserList;