import React, { useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Chip,
  Tooltip,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Logout as LogoutIcon,
} from '@mui/icons-material';
import { Sidebar } from '../Sidebar/Sidebar';
import { useAuth } from '../../contexts/AuthContext';

const DRAWER_WIDTH = 280;

export const MainLayout: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [sidebarOpen, setSidebarOpen] = useState<boolean>(() => {
    if (typeof window === 'undefined') return true;
    if (window.innerWidth < 900) return false;
    const saved = localStorage.getItem('isperp_sidebar_open');
    return saved !== null ? saved === 'true' : true;
  });

  const handleToggle = () => {
    setSidebarOpen(prev => {
      const next = !prev;
      localStorage.setItem('isperp_sidebar_open', String(next));
      return next;
    });
  };

  const handleClose = () => {
    if (isMobile) {
      setSidebarOpen(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isDrawerVisibleInDesktop = !isMobile && sidebarOpen;

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'grey.50' }}>
      {/* Sidebar Compartilhado e Permanente */}
      <Sidebar
        open={sidebarOpen}
        onClose={handleClose}
        onToggle={handleToggle}
      />

      {/* Barra de Navegação Superior */}
      <AppBar
        position="fixed"
        sx={{
          zIndex: theme.zIndex.drawer + 1,
          width: isDrawerVisibleInDesktop ? `calc(100% - ${DRAWER_WIDTH}px)` : '100%',
          ml: isDrawerVisibleInDesktop ? `${DRAWER_WIDTH}px` : 0,
          transition: theme.transitions.create(['margin', 'width'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
        }}
      >
        <Toolbar sx={{ px: { xs: 2, sm: 3 } }}>
          {(!sidebarOpen || isMobile) && (
            <IconButton
              color="inherit"
              aria-label="Abrir menu"
              onClick={handleToggle}
              edge="start"
              sx={{ mr: 2 }}
            >
              <MenuIcon />
            </IconButton>
          )}

          <Typography
            variant="h6"
            noWrap
            component="div"
            sx={{
              flexGrow: 1,
              fontSize: { xs: '1rem', sm: '1.25rem' },
              fontWeight: 'bold',
            }}
          >
            ISP ERP • Nexus Fibra Telecom
          </Typography>

          <Chip
            label={user?.role || 'ADMIN'}
            color="secondary"
            size="small"
            sx={{ mr: 2, fontWeight: 'bold' }}
          />

          <Tooltip title="Sair do Sistema">
            <IconButton color="inherit" onClick={handleLogout} edge="end">
              <LogoutIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      {/* Conteúdo Principal das Páginas (Outlet) */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          minWidth: 0,
          minHeight: '100vh',
          width: isDrawerVisibleInDesktop ? `calc(100% - ${DRAWER_WIDTH}px)` : '100%',
          ml: isDrawerVisibleInDesktop ? `${DRAWER_WIDTH}px` : 0,
          transition: theme.transitions.create(['margin', 'width'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
        }}
      >
        {/* Espaçador para compensar a altura fixa da AppBar */}
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
};

export default MainLayout;
