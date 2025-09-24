import React from 'react';
import {
  Container,
  Typography,
  Grid,
  Card,
  CardContent,
  Box,
  Avatar,
} from '@mui/material';
import {
  People,
  Business,
  AttachMoney,
  TrendingUp,
} from '@mui/icons-material';

const Dashboard = () => {
  const stats = [
    {
      title: 'Clientes Ativos',
      value: '1,234',
      icon: <People />,
      color: '#1976d2',
    },
    {
      title: 'Receita Mensal',
      value: 'R$ 45.678',
      icon: <AttachMoney />,
      color: '#2e7d32',
    },
    {
      title: 'Novos Clientes',
      value: '89',
      icon: <TrendingUp />,
      color: '#ed6c02',
    },
    {
      title: 'Empresas Parceiras',
      value: '12',
      icon: <Business />,
      color: '#9c27b0',
    },
  ];

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>
      <Typography variant="body1" color="textSecondary" paragraph>
        Bem-vindo ao painel de controle do ISP ERP
      </Typography>

      <Grid container spacing={3}>
        {stats.map((stat, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center">
                  <Avatar
                    sx={{
                      bgcolor: stat.color,
                      mr: 2,
                    }}
                  >
                    {stat.icon}
                  </Avatar>
                  <Box>
                    <Typography color="textSecondary" gutterBottom>
                      {stat.title}
                    </Typography>
                    <Typography variant="h5" component="h2">
                      {stat.value}
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={3} sx={{ mt: 3 }}>
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Atividades Recentes
              </Typography>
              <Typography variant="body2" color="textSecondary">
                Nenhuma atividade recente encontrada.
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Container>
  );
};

export default Dashboard;