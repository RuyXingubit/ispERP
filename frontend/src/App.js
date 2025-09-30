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
              !isAuthenticated ? <Navigate to="/login" replace /> : 
              <Dashboard />
            } 
          />
          <Route 
            path="/dashboard/usuarios" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              !isAuthenticated ? <Navigate to="/login" replace /> : 
              <UserList />
            } 
          />
          <Route 
            path="/dashboard/empresas" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              !isAuthenticated ? <Navigate to="/login" replace /> : 
              <CompanyList />
            } 
          />
          <Route 
            path="/dashboard/configuracoes" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              !isAuthenticated ? <Navigate to="/login" replace /> : 
              <SiteSettings />
            } 
          />
          <Route 
            path="/customers" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              !isAuthenticated ? <Navigate to="/login" replace /> : 
              <CustomerList />
            } 
          />
          <Route 
            path="/customers/new" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              !isAuthenticated ? <Navigate to="/login" replace /> : 
              <CustomerForm />
            } 
          />
          <Route 
            path="/customers/edit/:id" 
            element={
              !isSetupCompleted ? <Navigate to="/setup" replace /> : 
              !isAuthenticated ? <Navigate to="/login" replace /> : 
              <CustomerForm />
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