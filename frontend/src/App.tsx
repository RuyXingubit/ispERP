import React, { useState, useEffect, Suspense, lazy } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { ToastContainer } from 'react-toastify';
import { CircularProgress, Box, Typography } from '@mui/material';
import 'react-toastify/dist/ReactToastify.css';

import ProtectedRoute from './components/Auth/ProtectedRoute';
import MainLayout from './components/Layout/MainLayout';
import { setupService } from './services/setupService';
import { AuthProvider, useAuth } from './contexts/AuthContext';

// Lazy-loaded pages for high-performance Code-Splitting
const Setup = lazy(() => import('./pages/Setup/Setup'));
const Home = lazy(() => import('./pages/Home/Home'));
const Login = lazy(() => import('./pages/Login/Login'));
const Dashboard = lazy(() => import('./pages/Dashboard/Dashboard'));
const UserList = lazy(() => import('./pages/Users/UserList'));
const CompanyList = lazy(() => import('./pages/Companies/CompanyList'));
const SiteSettings = lazy(() => import('./pages/SiteSettings/SiteSettings'));
const CustomerList = lazy(() => import('./pages/CustomerList'));
const CustomerForm = lazy(() => import('./pages/CustomerForm'));
const PlanList = lazy(() => import('./pages/Plans/PlanList'));
const SaleForm = lazy(() => import('./pages/Sales/SaleForm'));
const ContractList = lazy(() => import('./pages/Contracts/ContractList'));
const ContractTemplateManager = lazy(() => import('./pages/Contracts/ContractTemplateManager').then(m => ({ default: m.ContractTemplateManager })));
const PublicSignaturePage = lazy(() => import('./pages/Public/PublicSignaturePage').then(m => ({ default: m.PublicSignaturePage })));
const WorkOrderList = lazy(() => import('./pages/WorkOrders/WorkOrderList'));
const TechnicianPortal = lazy(() => import('./pages/Technician/TechnicianPortal'));
const InvoiceList = lazy(() => import('./pages/Financial/InvoiceList'));
const GatewayConfig = lazy(() => import('./pages/Financial/GatewayConfig'));
const FiscalDashboard = lazy(() => import('./pages/Financial/FiscalDashboard'));
const CashCustodyManager = lazy(() => import('./pages/Financial/CashCustodyManager'));
const ChartOfAccountsManager = lazy(() => import('./pages/Financial/ChartOfAccountsManager').then(m => ({ default: m.ChartOfAccountsManager })));
const PayablesManager = lazy(() => import('./pages/Financial/PayablesManager').then(m => ({ default: m.PayablesManager })));
const WorkOrderFeeWaivers = lazy(() => import('./pages/Financial/WorkOrderFeeWaivers').then(m => ({ default: m.WorkOrderFeeWaivers })));
const DeleveragingDashboard = lazy(() => import('./pages/Financial/DeleveragingDashboard').then(m => ({ default: m.DeleveragingDashboard })));
const DreReportViewer = lazy(() => import('./pages/Financial/DreReportViewer').then(m => ({ default: m.DreReportViewer })));
const NetworkProjectsPayback = lazy(() => import('./pages/Financial/NetworkProjectsPayback').then(m => ({ default: m.NetworkProjectsPayback })));
const SentinelWatchdog = lazy(() => import('./pages/Financial/SentinelWatchdog').then(m => ({ default: m.SentinelWatchdog })));
const BackupDisasterRecoveryDashboard = lazy(() => import('./pages/Financial/BackupDisasterRecoveryDashboard').then(m => ({ default: m.BackupDisasterRecoveryDashboard })));
const OnuList = lazy(() => import('./pages/Network/OnuList'));
const NetworkDeviceList = lazy(() => import('./pages/Network/NetworkDeviceList'));
const IpamManager = lazy(() => import('./pages/Network/IpamManager'));
const RadiusManager = lazy(() => import('./pages/Network/RadiusManager'));
const FtthManager = lazy(() => import('./pages/Network/FtthManager'));
const NocDashboard = lazy(() => import('./pages/Network/NocDashboard'));
const CgnatManager = lazy(() => import('./pages/Network/CgnatManager'));
const MarcoCivilSearch = lazy(() => import('./pages/Network/MarcoCivilSearch'));
const ReportValidation = lazy(() => import('./pages/Public/ReportValidation'));
const ClientPortal = lazy(() => import('./pages/Portal/ClientPortal'));
const InventoryManager = lazy(() => import('./pages/Inventory/InventoryManager'));
const RoutePlanner = lazy(() => import('./pages/WorkOrders/RoutePlanner'));
const InstallationDispatchDashboard = lazy(() => import('./pages/WorkOrders/InstallationDispatchDashboard').then(m => ({ default: m.InstallationDispatchDashboard })));
const TicketList = lazy(() => import('./pages/Helpdesk/TicketList'));
const NotificationConfigList = lazy(() => import('./pages/Settings/NotificationConfigList'));
const StorageConfig = lazy(() => import('./pages/Settings/StorageConfig'));

const PageLoadingFallback = () => (
  <Box 
    display="flex" 
    flexDirection="column"
    justifyContent="center" 
    alignItems="center" 
    minHeight="60vh"
    gap={2}
  >
    <CircularProgress size={36} thickness={4} />
    <Typography variant="caption" color="text.secondary">
      Carregando módulo ispERP...
    </Typography>
  </Box>
);

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
});

function AppContent() {
  const [isSetupCompleted, setIsSetupCompleted] = useState(null);
  const { isAuthenticated, loading } = useAuth();

  useEffect(() => {
    checkSetupStatus();
  }, []);

  const checkSetupStatus = async () => {
    try {
      const status: any = await setupService.getSetupStatus();
      setIsSetupCompleted(status?.data?.isSetupCompleted ?? status?.isSetupCompleted ?? true);
    } catch (error) {
      console.error('Erro ao verificar status do setup:', error);
      setIsSetupCompleted(false);
    }
  };

  if (loading || isSetupCompleted === null) {
    return (
      <Box 
        display="flex" 
        justifyContent="center" 
        alignItems="center" 
        minHeight="100vh"
      >
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Router>
      <div className="App">
        <Suspense fallback={<PageLoadingFallback />}>
          <Routes>
          <Route 
            path="/setup" 
            element={isSetupCompleted ? <Navigate to="/" replace /> : <Setup />} 
          />
          <Route 
            path="/home" 
            element={!isSetupCompleted ? <Navigate to="/setup" replace /> : <Home />} 
          />
          <Route 
            path="/login" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              isAuthenticated ? <Navigate to="/dashboard" replace /> : 
              <Login />
            } 
          />
          <Route 
            path="/portal/client" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ClientPortal />
            } 
          />
          <Route 
            path="/public/validar-laudo/:token" 
            element={<ReportValidation />} 
          />
          <Route 
            path="/sign/:token" 
            element={<PublicSignaturePage />} 
          />

          {/* Rotas Autenticadas com Menu Lateral Persistente */}
          <Route
            element={
              !isSetupCompleted ? (
                <Navigate to="/setup" replace />
              ) : (
                <ProtectedRoute>
                  <MainLayout />
                </ProtectedRoute>
              )
            }
          >
            <Route 
              path="/dashboard" 
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL']}>
                  <Dashboard />
                </ProtectedRoute>
              } 
            />
          <Route 
            path="/dashboard/usuarios" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <UserList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/dashboard/empresas" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <CompanyList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/dashboard/configuracoes" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <SiteSettings />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/customers" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL', 'SUPPORT_ANALYST', 'ADMINISTRATIVE_ASSISTANT']}>
                <CustomerList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/customers/new" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL', 'SUPPORT_ANALYST', 'ADMINISTRATIVE_ASSISTANT']}>
                <CustomerForm />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/customers/edit/:id" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL', 'SUPPORT_ANALYST', 'ADMINISTRATIVE_ASSISTANT']}>
                <CustomerForm />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/plans" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL', 'SUPPORT_ANALYST', 'ADMINISTRATIVE_ASSISTANT']}>
                <PlanList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/sales/new" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL', 'SUPPORT_ANALYST', 'ADMINISTRATIVE_ASSISTANT']}>
                <SaleForm />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/contracts" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL', 'SUPPORT_ANALYST', 'ADMINISTRATIVE_ASSISTANT']}>
                <ContractList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/contracts/templates" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL', 'SUPPORT_ANALYST', 'ADMINISTRATIVE_ASSISTANT']}>
                <ContractTemplateManager />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/work-orders" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'TECHNICIAN', 'SUPPORT_ANALYST', 'SUPPORT_N2']}>
                <WorkOrderList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/technician/field" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'TECHNICIAN', 'SUPPORT_ANALYST', 'SUPPORT_N2']}>
                <TechnicianPortal />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/invoices" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL', 'ADMINISTRATIVE_ASSISTANT']}>
                <InvoiceList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/payment-gateways" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL']}>
                <GatewayConfig />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/fiscal" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL', 'ADMINISTRATIVE_ASSISTANT']}>
                <FiscalDashboard />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/financial/custody" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL', 'ATTENDANT', 'TECHNICIAN']}>
                <CashCustodyManager />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/financial/chart-of-accounts" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL']}>
                <ChartOfAccountsManager />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/financial/payables" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL']}>
                <PayablesManager />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/financial/fee-waivers" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL', 'ATTENDANT']}>
                <WorkOrderFeeWaivers />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/financial/deleveraging" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL']}>
                <DeleveragingDashboard />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/financial/dre" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL']}>
                <DreReportViewer />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/financial/projects-payback" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL']}>
                <NetworkProjectsPayback />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/financial/sentinel" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL']}>
                <SentinelWatchdog />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/onus" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_N2']}>
                <OnuList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/network-devices" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_N2']}>
                <NetworkDeviceList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/network/ipam" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_N2']}>
                <IpamManager />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/network/radius" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_N2']}>
                <RadiusManager />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/network/ftth" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_N2', 'FIELD_TECH']}>
                <FtthManager />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/network/noc" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_N2', 'FIELD_TECH']}>
                <NocDashboard />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/network/cgnat" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_N2']}>
                <CgnatManager />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/network/marco-civil" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_N2', 'FINANCIAL', 'ADMINISTRATIVE_ASSISTANT']}>
                <MarcoCivilSearch />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/inventory" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_N2', 'ADMINISTRATIVE_ASSISTANT', 'FINANCIAL']}>
                <InventoryManager />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/routes/planner" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_ANALYST', 'SUPPORT_N2', 'TECHNICIAN']}>
                <RoutePlanner />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/dispatch/installations" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_ANALYST', 'SUPPORT_N2', 'FINANCIAL']}>
                <InstallationDispatchDashboard />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/helpdesk" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'ATTENDANT', 'SUPPORT_N2', 'SUPPORT_ANALYST']}>
                <TicketList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/settings/notifications" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <NotificationConfigList />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/settings/storage" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <StorageConfig />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/settings/backup" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL']}>
                <BackupDisasterRecoveryDashboard />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/financial/backup" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'FINANCIAL']}>
                <BackupDisasterRecoveryDashboard />
              </ProtectedRoute>
            } 
          />
          </Route>

          <Route 
            path="/" 
            element={
              <Navigate 
                to={
                  !isSetupCompleted ? "/setup" : 
                  "/home"
                } 
                replace 
              />
            } 
          />
        </Routes>
        </Suspense>
        <ToastContainer
          position="top-right"
          autoClose={5000}
          hideProgressBar={false}
          newestOnTop={false}
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
        />
      </div>
    </Router>
  );
}

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;