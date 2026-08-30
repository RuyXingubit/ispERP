import React from 'react';
import {
  Container,
  Paper,
  TextField,
  Button,
  Typography,
  Box,
  Avatar,
  Link,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import { LockOutlined } from '@mui/icons-material';
import { Formik, Form, Field } from 'formik';
import * as Yup from 'yup';
import { toast } from 'react-toastify';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

const validationSchema = Yup.object({
  email: Yup.string().email('Email inválido').required('Email é obrigatório'),
  password: Yup.string().required('Senha é obrigatória'),
});

const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const isTablet = useMediaQuery(theme.breakpoints.between('sm', 'md'));

  const handleSubmit = async (values, { setSubmitting }) => {
    try {
      const response = await login(values);
      
      if (response.success) {
        toast.success('Login realizado com sucesso!');
        // Aguardar um pouco para o toast aparecer e depois redirecionar
        setTimeout(() => {
          // Usar navigate para redirecionar - o contexto já atualizou o estado
          navigate('/dashboard');
        }, 1000);
      } else {
        toast.error(response.message || 'Erro ao fazer login');
      }
    } catch (error) {
      console.error('Erro no login:', error);
      toast.error('Credenciais inválidas ou erro no servidor');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Container 
      component="main" 
      maxWidth={isMobile ? 'xs' : isTablet ? 'sm' : 'xs'}
      sx={{
        px: { xs: 2, sm: 3 },
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <Box
        sx={{
          width: '100%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          py: { xs: 2, sm: 4 },
        }}
      >
        <Paper 
          elevation={isMobile ? 1 : 3} 
          sx={{ 
            p: { xs: 3, sm: 4, md: 5 }, 
            width: '100%',
            maxWidth: { xs: '100%', sm: 400, md: 450 },
            borderRadius: { xs: 2, sm: 3 },
            boxShadow: {
              xs: theme.shadows[1],
              sm: theme.shadows[3],
              md: theme.shadows[8],
            },
          }}
        >
          <Box
            sx={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
            }}
          >
            <Avatar 
              sx={{ 
                m: { xs: 1, sm: 2 }, 
                bgcolor: 'secondary.main',
                width: { xs: 40, sm: 48, md: 56 },
                height: { xs: 40, sm: 48, md: 56 },
              }}
            >
              <LockOutlined sx={{ fontSize: { xs: 20, sm: 24, md: 28 } }} />
            </Avatar>
            <Typography 
              component="h1" 
              variant={isMobile ? "h6" : "h5"}
              sx={{
                mb: { xs: 2, sm: 3 },
                textAlign: 'center',
                fontWeight: 'medium',
              }}
            >
              Entrar no Sistema
            </Typography>
            <Formik
              initialValues={{
                email: '',
                password: '',
              }}
              validationSchema={validationSchema}
              onSubmit={handleSubmit}
            >
              {({ errors, touched, isSubmitting }) => (
                <Form style={{ width: '100%' }}>
                  <Field
                    as={TextField}
                    variant="outlined"
                    margin="normal"
                    required
                    fullWidth
                    id="email"
                    label="Email"
                    name="email"
                    autoComplete="email"
                    autoFocus
                    error={touched.email && !!errors.email}
                    helperText={touched.email && errors.email}
                    sx={{
                      mb: { xs: 2, sm: 2.5 },
                      '& .MuiInputBase-root': {
                        fontSize: { xs: '0.875rem', sm: '1rem' },
                      },
                      '& .MuiInputLabel-root': {
                        fontSize: { xs: '0.875rem', sm: '1rem' },
                      },
                    }}
                  />
                  <Field
                    as={TextField}
                    variant="outlined"
                    margin="normal"
                    required
                    fullWidth
                    name="password"
                    label="Senha"
                    type="password"
                    id="password"
                    autoComplete="current-password"
                    error={touched.password && !!errors.password}
                    helperText={touched.password && errors.password}
                    sx={{
                      mb: { xs: 2, sm: 3 },
                      '& .MuiInputBase-root': {
                        fontSize: { xs: '0.875rem', sm: '1rem' },
                      },
                      '& .MuiInputLabel-root': {
                        fontSize: { xs: '0.875rem', sm: '1rem' },
                      },
                    }}
                  />
                  <Button
                    type="submit"
                    fullWidth
                    variant="contained"
                    disabled={isSubmitting}
                    sx={{ 
                      mt: { xs: 2, sm: 3 }, 
                      mb: { xs: 2, sm: 3 },
                      py: { xs: 1.5, sm: 2 },
                      fontSize: { xs: '0.875rem', sm: '1rem' },
                      fontWeight: 'medium',
                      borderRadius: { xs: 1.5, sm: 2 },
                      textTransform: 'none',
                      boxShadow: theme.shadows[2],
                      '&:hover': {
                        boxShadow: theme.shadows[4],
                      },
                    }}
                  >
                    {isSubmitting ? 'Entrando...' : 'Entrar'}
                  </Button>
                  <Box 
                    textAlign="center"
                    sx={{ mt: { xs: 1, sm: 2 } }}
                  >
                    <Link 
                      href="#" 
                      variant="body2"
                      sx={{
                        fontSize: { xs: '0.75rem', sm: '0.875rem' },
                        textDecoration: 'none',
                        '&:hover': {
                          textDecoration: 'underline',
                        },
                      }}
                    >
                      Esqueceu sua senha?
                    </Link>
                  </Box>
                </Form>
              )}
            </Formik>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default Login;