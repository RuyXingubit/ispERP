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
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import { Formik, Form, Field } from 'formik';
import * as Yup from 'yup';
import { toast } from 'react-toastify';
import { useAuth } from '../../contexts/AuthContext';
import userService from '../../services/userService';
import api from '../../services/api';

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
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [isEditing, setIsEditing] = useState(false);

  // Carregar usuários
  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const data = await userService.getAll();
      setUsers(data || []);
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
    if (user?.role !== 'ADMIN') {
      toast.error('Apenas administradores podem editar usuários');
      return;
    }
    setSelectedUser(userToEdit);
    setIsEditing(true);
    setDialogOpen(true);
  };

  const handleDeleteUser = (userToDelete) => {
    if (user?.role !== 'ADMIN') {
      toast.error('Apenas administradores podem deletar usuários');
      return;
    }
    setSelectedUser(userToDelete);
    setDeleteDialogOpen(true);
  };

  const handleSubmit = async (values, { setSubmitting, resetForm }) => {
    try {
      if (isEditing && selectedUser) {
        await api.put(`/users/${selectedUser.id}`, values);
        toast.success('Usuário atualizado com sucesso!');
      } else {
        await userService.create(values);
        toast.success('Usuário criado com sucesso!');
      }
      await loadUsers();
      setDialogOpen(false);
      resetForm();
    } catch (error: any) {
      console.error('Erro ao salvar usuário:', error);
      toast.error(error?.response?.data?.message || 'Erro ao salvar usuário');
    } finally {
      setSubmitting(false);
    }
  };

  const confirmDelete = async () => {
    try {
      await api.delete(`/users/${selectedUser.id}`);
      toast.success('Usuário deletado com sucesso!');
      await loadUsers();
      setDeleteDialogOpen(false);
    } catch (error) {
      console.error('Erro ao deletar usuário:', error);
      toast.error('Erro ao deletar usuário');
    }
  };

  const getRoleLabel = (role) => {
    switch (role) {
      case 'ADMIN':
        return 'Diretoria / Admin';
      case 'CFO':
        return 'CFO / Financeiro';
      case 'SUPPORT_ANALYST':
        return 'Analista Comercial';
      case 'ADMINISTRATIVE_ASSISTANT':
        return 'Suporte N1';
      case 'ATTENDANT':
        return 'Cobrança / Atendimento';
      case 'TECHNICIAN':
        return 'Técnico de Campo';
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
      case 'CFO':
        return 'warning';
      case 'TECHNICIAN':
        return 'secondary';
      case 'SUPPORT_ANALYST':
      case 'ADMINISTRATIVE_ASSISTANT':
      case 'ATTENDANT':
        return 'info';
      case 'USER':
        return 'primary';
      default:
        return 'default';
    }
  };

  return (
    <Container maxWidth="xl" sx={{ py: 3, px: { xs: 2, sm: 3 } }}>
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
                          disabled={user?.role !== 'ADMIN'}
                        >
                          <EditIcon />
                        </IconButton>
                        <IconButton
                          onClick={() => handleDeleteUser(userItem)}
                          color="error"
                          disabled={user?.role !== 'ADMIN'}
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

      {/* Dialog de Criação/Edição */}
      <Dialog 
        open={dialogOpen} 
        onClose={() => setDialogOpen(false)} 
        maxWidth="xs" 
        fullWidth
        scroll="paper"
      >
        <DialogTitle sx={{ py: 1.5 }}>
          {isEditing ? 'Editar Usuário' : 'Novo Usuário'}
        </DialogTitle>
        <Formik
          initialValues={{
            name: selectedUser?.name || '',
            email: selectedUser?.email || '',
            role: selectedUser?.role || 'USER',
            password: '',
            active: true,
            isEditing: isEditing,
          }}
          validationSchema={validationSchema}
          onSubmit={handleSubmit}
        >
          {({ errors, touched, isSubmitting }) => (
            <Form>
              <DialogContent dividers sx={{ py: 2 }}>
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
                        <MenuItem value="ADMIN">Diretoria / Administrador</MenuItem>
                        <MenuItem value="CFO">CFO / Financeiro</MenuItem>
                        <MenuItem value="SUPPORT_ANALYST">Analista Comercial</MenuItem>
                        <MenuItem value="ADMINISTRATIVE_ASSISTANT">Suporte N1</MenuItem>
                        <MenuItem value="ATTENDANT">Cobrança / Atendimento</MenuItem>
                        <MenuItem value="TECHNICIAN">Técnico de Campo</MenuItem>
                        <MenuItem value="USER">Usuário Comum</MenuItem>
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
    </Container>
  );
};

export default UserList;