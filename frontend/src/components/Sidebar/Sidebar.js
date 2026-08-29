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
  ChevronLeft as ChevronLeftIcon,
  ExpandLess,
  ExpandMore,
  SupervisorAccount as SupervisorAccountIcon,
  AppRegistration as AppRegistrationIcon,
  Business as BusinessIcon,
  Settings as SettingsIcon,
  Person as PersonIcon,
  ShoppingCart as ShoppingCartIcon,
  Speed as SpeedIcon,
  Description as ContractIcon,
  Storefront as CommercialIcon,
  Build as OperationsIcon,
  Engineering as WorkOrderIcon,
  AccountBalance as FinancialIcon,
  Receipt as InvoiceIcon,
  Payment as GatewayIcon,
  Router as NetworkIcon,
  Sensors as OnuIcon,
  Storage as OltIcon,
} from '@mui/icons-material';

const DRAWER_WIDTH = 280;
const DRAWER_WIDTH_MOBILE = 260;

const Sidebar = ({ open, onClose, onToggle }) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const navigate = useNavigate();
  const [cadastroOpen, setCadastroOpen] = useState(false);
  const [comercialOpen, setComercialOpen] = useState(true);
  const [operacoesOpen, setOperacoesOpen] = useState(true);
  const [financeiroOpen, setFinanceiroOpen] = useState(true);
  const [redeOpen, setRedeOpen] = useState(true);

  const handleNavigate = (path) => {
    navigate(path);
    if (isMobile) {
      onClose();
    }
  };

  const redeItems = [
    {
      text: 'NOC - ONUs & Fibra',
      icon: <OnuIcon />,
      path: '/onus',
    },
    {
      text: 'OLTs & Concentradores',
      icon: <OltIcon />,
      path: '/network-devices',
    },
  ];

  const financeiroItems = [
    {
      text: 'Faturas & Cobranças',
      icon: <InvoiceIcon />,
      path: '/invoices',
    },
    {
      text: 'Gateways de Pagamento',
      icon: <GatewayIcon />,
      path: '/payment-gateways',
    },
  ];

  const comercialItems = [
    {
      text: 'Planos de Internet',
      icon: <SpeedIcon />,
      path: '/plans',
    },
    {
      text: 'Venda Rápida',
      icon: <ShoppingCartIcon />,
      path: '/sales/new',
    },
    {
      text: 'Contratos',
      icon: <ContractIcon />,
      path: '/contracts',
    },
  ];

  const operacoesItems = [
    {
      text: 'Ordens de Serviço (O.S.)',
      icon: <WorkOrderIcon />,
      path: '/work-orders',
    },
  ];

  const cadastroItems = [
    {
      text: 'Usuários do Sistema',
      icon: <SupervisorAccountIcon />,
      path: '/dashboard/usuarios',
    },
    {
      text: 'Empresas Parceiras',
      icon: <BusinessIcon />,
      path: '/dashboard/empresas',
    },
    {
      text: 'Clientes',
      icon: <PersonIcon />,
      path: '/customers',
    },
    {
      text: 'Central do Assinante',
      icon: <PersonIcon />,
      path: '/portal/client',
    },
    {
      text: 'Notificações & WhatsApp',
      icon: <SettingsIcon />,
      path: '/settings/notifications',
    },
    {
      text: 'Configurações do Site',
      icon: <SettingsIcon />,
      path: '/dashboard/configuracoes',
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
      {/* Header */}
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
        {/* Rede & NOC */}
        <ListItem disablePadding>
          <ListItemButton
            onClick={() => setRedeOpen(!redeOpen)}
            sx={{ minHeight: 44, px: 2 }}
          >
            <ListItemIcon sx={{ minWidth: 0, mr: 2, color: 'primary.main' }}>
              <NetworkIcon />
            </ListItemIcon>
            <ListItemText
              primary="Rede & NOC"
              primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: 'bold' }}
            />
            {redeOpen ? <ExpandLess /> : <ExpandMore />}
          </ListItemButton>
        </ListItem>

        <Collapse in={redeOpen} timeout="auto" unmountOnExit>
          <List component="div" disablePadding>
            {redeItems.map((item, index) => (
              <ListItem key={index} disablePadding>
                <ListItemButton
                  onClick={() => handleNavigate(item.path)}
                  sx={{ pl: 4, py: 0.8, '&:hover': { bgcolor: 'action.hover' } }}
                >
                  <ListItemIcon sx={{ minWidth: 0, mr: 2, color: 'text.secondary' }}>
                    {item.icon}
                  </ListItemIcon>
                  <ListItemText primary={item.text} primaryTypographyProps={{ fontSize: '0.85rem' }} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Collapse>

        <Divider sx={{ my: 0.5 }} />

        {/* Financeiro & Faturamento */}
        <ListItem disablePadding>
          <ListItemButton
            onClick={() => setFinanceiroOpen(!financeiroOpen)}
            sx={{ minHeight: 44, px: 2 }}
          >
            <ListItemIcon sx={{ minWidth: 0, mr: 2, color: 'primary.main' }}>
              <FinancialIcon />
            </ListItemIcon>
            <ListItemText
              primary="Financeiro & Cobrança"
              primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: 'bold' }}
            />
            {financeiroOpen ? <ExpandLess /> : <ExpandMore />}
          </ListItemButton>
        </ListItem>

        <Collapse in={financeiroOpen} timeout="auto" unmountOnExit>
          <List component="div" disablePadding>
            {financeiroItems.map((item, index) => (
              <ListItem key={index} disablePadding>
                <ListItemButton
                  onClick={() => handleNavigate(item.path)}
                  sx={{ pl: 4, py: 0.8, '&:hover': { bgcolor: 'action.hover' } }}
                >
                  <ListItemIcon sx={{ minWidth: 0, mr: 2, color: 'text.secondary' }}>
                    {item.icon}
                  </ListItemIcon>
                  <ListItemText primary={item.text} primaryTypographyProps={{ fontSize: '0.85rem' }} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Collapse>

        <Divider sx={{ my: 0.5 }} />

        {/* Comercial & Vendas */}
        <ListItem disablePadding>
          <ListItemButton
            onClick={() => setComercialOpen(!comercialOpen)}
            sx={{ minHeight: 44, px: 2 }}
          >
            <ListItemIcon sx={{ minWidth: 0, mr: 2, color: 'primary.main' }}>
              <CommercialIcon />
            </ListItemIcon>
            <ListItemText
              primary="Comercial & Vendas"
              primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: 'bold' }}
            />
            {comercialOpen ? <ExpandLess /> : <ExpandMore />}
          </ListItemButton>
        </ListItem>

        <Collapse in={comercialOpen} timeout="auto" unmountOnExit>
          <List component="div" disablePadding>
            {comercialItems.map((item, index) => (
              <ListItem key={index} disablePadding>
                <ListItemButton
                  onClick={() => handleNavigate(item.path)}
                  sx={{ pl: 4, py: 0.8, '&:hover': { bgcolor: 'action.hover' } }}
                >
                  <ListItemIcon sx={{ minWidth: 0, mr: 2, color: 'text.secondary' }}>
                    {item.icon}
                  </ListItemIcon>
                  <ListItemText primary={item.text} primaryTypographyProps={{ fontSize: '0.85rem' }} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Collapse>

        <Divider sx={{ my: 0.5 }} />

        {/* Operações & Campo */}
        <ListItem disablePadding>
          <ListItemButton
            onClick={() => setOperacoesOpen(!operacoesOpen)}
            sx={{ minHeight: 44, px: 2 }}
          >
            <ListItemIcon sx={{ minWidth: 0, mr: 2, color: 'primary.main' }}>
              <OperationsIcon />
            </ListItemIcon>
            <ListItemText
              primary="Operações & Campo"
              primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: 'bold' }}
            />
            {operacoesOpen ? <ExpandLess /> : <ExpandMore />}
          </ListItemButton>
        </ListItem>

        <Collapse in={operacoesOpen} timeout="auto" unmountOnExit>
          <List component="div" disablePadding>
            {operacoesItems.map((item, index) => (
              <ListItem key={index} disablePadding>
                <ListItemButton
                  onClick={() => handleNavigate(item.path)}
                  sx={{ pl: 4, py: 0.8, '&:hover': { bgcolor: 'action.hover' } }}
                >
                  <ListItemIcon sx={{ minWidth: 0, mr: 2, color: 'text.secondary' }}>
                    {item.icon}
                  </ListItemIcon>
                  <ListItemText primary={item.text} primaryTypographyProps={{ fontSize: '0.85rem' }} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Collapse>

        <Divider sx={{ my: 0.5 }} />

        {/* Cadastros Gerais */}
        <ListItem disablePadding>
          <ListItemButton
            onClick={() => setCadastroOpen(!cadastroOpen)}
            sx={{ minHeight: 44, px: 2 }}
          >
            <ListItemIcon sx={{ minWidth: 0, mr: 2, color: 'primary.main' }}>
              <AppRegistrationIcon />
            </ListItemIcon>
            <ListItemText
              primary="Cadastros Gerais"
              primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: 'medium' }}
            />
            {cadastroOpen ? <ExpandLess /> : <ExpandMore />}
          </ListItemButton>
        </ListItem>

        <Collapse in={cadastroOpen} timeout="auto" unmountOnExit>
          <List component="div" disablePadding>
            {cadastroItems.map((item, index) => (
              <ListItem key={index} disablePadding>
                <ListItemButton
                  onClick={() => handleNavigate(item.path)}
                  sx={{ pl: 4, py: 0.8, '&:hover': { bgcolor: 'action.hover' } }}
                >
                  <ListItemIcon sx={{ minWidth: 0, mr: 2, color: 'text.secondary' }}>
                    {item.icon}
                  </ListItemIcon>
                  <ListItemText primary={item.text} primaryTypographyProps={{ fontSize: '0.85rem' }} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Collapse>
      </List>

      {/* Footer */}
      <Box sx={{ p: 1.5, borderTop: 1, borderColor: 'divider', bgcolor: 'grey.50' }}>
        <Typography variant="caption" color="text.secondary" align="center" sx={{ display: 'block' }}>
          © 2026 ISP ERP System
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
        keepMounted: true,
      }}
    >
      {drawerContent}
    </Drawer>
  );
};

export default Sidebar;