import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Box, Typography, Alert, Paper, Button } from '@mui/material';

const ProtectedRoute = ({ children, allowedRoles }) => {
  const { user, isAuthenticated, loading } = useAuth();

  if (loading) {
    return null;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const userRole = user?.role || 'USER';

  if (allowedRoles && !allowedRoles.includes(userRole) && userRole !== 'ADMIN') {
    return (
      <Box sx={{ p: 4, maxWidth: 600, mx: 'auto', mt: 8 }}>
        <Paper elevation={3} sx={{ p: 4, borderRadius: 3, textAlign: 'center' }}>
          <Alert severity="warning" sx={{ mb: 3, borderRadius: 2 }}>
            Acesso Restrito ao seu Perfil
          </Alert>
          <Typography variant="h6" fontWeight="bold" gutterBottom>
            Permissão Insuficiente ({userRole})
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Seu usuário não possui credenciais suficientes para visualizar este módulo ou métricas financeiras.
          </Typography>
          <Button variant="contained" color="primary" href="/home">
            Voltar ao Início
          </Button>
        </Paper>
      </Box>
    );
  }

  return children;
};

export default ProtectedRoute;
