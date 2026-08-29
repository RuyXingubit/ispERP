import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { ToastContainer } from 'react-toastify';
import { CircularProgress, Box } from '@mui/material';
import 'react-toastify/dist/ReactToastify.css';

import Setup from './pages/Setup/Setup';
import Home from './pages/Home/Home';
import Login from './pages/Login/Login';
import Dashboard from './pages/Dashboard/Dashboard';
import UserList from './pages/Users/UserList';
import CompanyList from './pages/Companies/CompanyList';
import SiteSettings from './pages/SiteSettings/SiteSettings';
import CustomerList from './pages/CustomerList';
import CustomerForm from './pages/CustomerForm';
import PlanList from './pages/Plans/PlanList';
import SaleForm from './pages/Sales/SaleForm';
import ContractList from './pages/Contracts/ContractList';
import WorkOrderList from './pages/WorkOrders/WorkOrderList';
import InvoiceList from './pages/Financial/InvoiceList';
import GatewayConfig from './pages/Financial/GatewayConfig';
import OnuList from './pages/Network/OnuList';
import NetworkDeviceList from './pages/Network/NetworkDeviceList';
import ClientPortal from './pages/Portal/ClientPortal';
import InventoryManager from './pages/Inventory/InventoryManager';
import RoutePlanner from './pages/WorkOrders/RoutePlanner';
import TicketList from './pages/Helpdesk/TicketList';
import NotificationConfigList from './pages/Settings/NotificationConfigList';
import ProtectedRoute from './components/Auth/ProtectedRoute';
import { setupService } from './services/setupService';
import { AuthProvider, useAuth } from './contexts/AuthContext';

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
      const status = await setupService.getSetupStatus();
      setIsSetupCompleted(status.isSetupCompleted);
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
            path="/dashboard" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
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
            path="/work-orders" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'TECHNICIAN', 'SUPPORT_ANALYST', 'SUPPORT_N2']}>
                <WorkOrderList />
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
            path="/helpdesk" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN', 'ATTENDANT', 'SUPPORT_N2', 'SUPPORT_ANALYST']}>
                <TicketList />
              </ProtectedRoute>
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
            path="/settings/notifications" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <NotificationConfigList />
              </ProtectedRoute>
            } 
          />
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