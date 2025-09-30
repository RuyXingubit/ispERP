import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Collapse,
  IconButton,
  Box,
  Typography,
  Divider,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import {
  Menu as MenuIcon,
  ChevronLeft as ChevronLeftIcon,
  ExpandLess,
  ExpandMore,
  PersonAdd as PersonAddIcon,
  Assignment as AssignmentIcon,
  RoomService as RoomServiceIcon,
  Inventory as InventoryIcon,
  SupervisorAccount as SupervisorAccountIcon,
  AppRegistration as AppRegistrationIcon,
  Business as BusinessIcon,
  Settings as SettingsIcon,
  Person as PersonIcon,
} from '@mui/icons-material';

const DRAWER_WIDTH = 280;
const DRAWER_WIDTH_MOBILE = 260;

const Sidebar = ({ open, onClose, onToggle }) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const isTablet = useMediaQuery(theme.breakpoints.between('md', 'lg'));
  const navigate = useNavigate();
  const [cadastroOpen, setCadastroOpen] = useState(false);

  const handleCadastroClick = () => {
    setCadastroOpen(!cadastroOpen);
  };

  const handleMenuItemClick = (item) => {
    console.log(`Navegando para: ${item}`);
    
    // Implementar navegação real
    switch (item) {
      case 'usuarios':
        navigate('/dashboard/usuarios');
        break;
      case 'empresas':
        navigate('/dashboard/empresas');
        break;
      case 'clientes':
        navigate('/customers');
        break;
      case 'configuracoes':
        navigate('/dashboard/configuracoes');
        break;
      default:
        console.log(`Rota não implementada: ${item}`);
    }
    
    // Recolher o menu automaticamente após seleção
    if (isMobile) {
      onClose();
    }
  };

  const cadastroItems = [
    {
      text: 'Cadastro de Usuários',
      icon: <SupervisorAccountIcon />,
      description: 'Usuários do Sistema',
      action: 'usuarios'
    },
    {
      text: 'Cadastro de Empresas',
      icon: <BusinessIcon />,
      description: 'Empresas Parceiras',
      action: 'empresas'
    },
    {
      text: 'Cadastro de Clientes',
      icon: <PersonIcon />,
      description: 'Clientes Pessoa Física',
      action: 'clientes'
    },
    {
      text: 'Configurações do Site',
      icon: <SettingsIcon />,
      description: 'Personalização do Site',
      action: 'configuracoes'
    },
  ];

  const drawerContent = (
    <Box
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: 'background.paper',
      }}
    >
      {/* Header do Sidebar */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          p: { xs: 1.5, sm: 2 },
          minHeight: { xs: 56, sm: 64 },
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
        }}
      >
        <Typography 
          variant={isMobile ? "subtitle1" : "h6"} 
          noWrap 
          component="div"
          sx={{
            fontSize: { xs: '1.1rem', sm: '1.25rem' },
            fontWeight: 'bold',
          }}
        >
          ISP ERP
        </Typography>
        <IconButton
          onClick={onToggle}
          sx={{ 
            color: 'inherit',
            p: { xs: 0.5, sm: 1 },
          }}
          aria-label="Fechar menu"
        >
          <ChevronLeftIcon sx={{ fontSize: { xs: 20, sm: 24 } }} />
        </IconButton>
      </Box>

      <Divider />

      {/* Menu Items */}
      <List sx={{ flexGrow: 1, py: { xs: 0.5, sm: 1 } }}>
        {/* Item Cadastro */}
        <ListItem disablePadding>
          <ListItemButton
            onClick={handleCadastroClick}
            sx={{
              minHeight: { xs: 44, sm: 48 },
              px: { xs: 2, sm: 2.5 },
              py: { xs: 1, sm: 1.5 },
              '&:hover': {
                bgcolor: 'action.hover',
              },
            }}
            aria-expanded={cadastroOpen}
            aria-label="Menu Cadastro"
          >
            <ListItemIcon
              sx={{
                minWidth: 0,
                mr: { xs: 2, sm: 3 },
                justifyContent: 'center',
                color: 'primary.main',
              }}
            >
              <AppRegistrationIcon sx={{ fontSize: { xs: 20, sm: 24 } }} />
            </ListItemIcon>
            <ListItemText
              primary="Cadastro"
              primaryTypographyProps={{
                fontSize: { xs: '0.875rem', sm: '1rem' },
                fontWeight: 'medium',
              }}
            />
            {cadastroOpen ? 
              <ExpandLess sx={{ fontSize: { xs: 20, sm: 24 } }} /> : 
              <ExpandMore sx={{ fontSize: { xs: 20, sm: 24 } }} />
            }
          </ListItemButton>
        </ListItem>

        {/* Submenu Cadastro */}
        <Collapse in={cadastroOpen} timeout="auto" unmountOnExit>
          <List component="div" disablePadding>
            {cadastroItems.map((item, index) => (
              <ListItem key={index} disablePadding>
                <ListItemButton
                  onClick={() => handleMenuItemClick(item.action)}
                  sx={{
                    pl: { xs: 3, sm: 4 },
                    pr: { xs: 1.5, sm: 2 },
                    py: { xs: 0.75, sm: 1 },
                    minHeight: { xs: 40, sm: 44 },
                    '&:hover': {
                      bgcolor: 'action.hover',
                      transform: 'translateX(4px)',
                      transition: 'all 0.2s ease-in-out',
                    },
                    transition: 'all 0.2s ease-in-out',
                  }}
                  aria-label={item.text}
                >
                  <ListItemIcon
                    sx={{
                      minWidth: 0,
                      mr: { xs: 1.5, sm: 2 },
                      justifyContent: 'center',
                      color: 'text.secondary',
                    }}
                  >
                    {React.cloneElement(item.icon, { 
                      sx: { fontSize: { xs: 18, sm: 20 } } 
                    })}
                  </ListItemIcon>
                  <ListItemText
                    primary={item.text}
                    secondary={!isMobile ? item.description : undefined}
                    primaryTypographyProps={{
                      fontSize: { xs: '0.8rem', sm: '0.875rem' },
                      fontWeight: 'regular',
                      lineHeight: { xs: 1.2, sm: 1.4 },
                    }}
                    secondaryTypographyProps={{
                      fontSize: { xs: '0.7rem', sm: '0.75rem' },
                      display: { xs: 'none', sm: 'block' },
                    }}
                  />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Collapse>
      </List>

      {/* Footer do Sidebar */}
      <Box
        sx={{
          p: { xs: 1.5, sm: 2 },
          borderTop: 1,
          borderColor: 'divider',
          bgcolor: 'grey.50',
        }}
      >
        <Typography 
          variant="caption" 
          color="text.secondary" 
          align="center"
          sx={{
            fontSize: { xs: '0.7rem', sm: '0.75rem' },
            display: 'block',
          }}
        >
          © 2025 ISP ERP System
        </Typography>
      </Box>
    </Box>
  );

  return (
    <Drawer
      variant={isMobile ? 'temporary' : 'persistent'}
      anchor="left"
      open={open}
      onClose={onClose}
      sx={{
        width: isMobile ? DRAWER_WIDTH_MOBILE : DRAWER_WIDTH,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: isMobile ? DRAWER_WIDTH_MOBILE : DRAWER_WIDTH,
          boxSizing: 'border-box',
          borderRight: '1px solid',
          borderColor: 'divider',
          boxShadow: theme.shadows[isMobile ? 4 : 3],
          zIndex: theme.zIndex.drawer,
        },
      }}
      ModalProps={{
        keepMounted: true, // Melhor performance em mobile
      }}
    >
      {drawerContent}
    </Drawer>
  );
};

export default Sidebar;