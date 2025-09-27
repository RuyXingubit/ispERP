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
import { setupService } from './services/setupService';
import { authService } from './services/authService';

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

function App() {
  const [isSetupCompleted, setIsSetupCompleted] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkSetupStatus();
    checkAuthStatus();
    
    // Listener para mudanças no localStorage (logout)
    const handleStorageChange = () => {
      checkAuthStatus();
    };
    
    window.addEventListener('storage', handleStorageChange);
    
    // Cleanup
    return () => {
      window.removeEventListener('storage', handleStorageChange);
    };
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

  const checkAuthStatus = () => {
    const authenticated = authService.isAuthenticated();
    setIsAuthenticated(authenticated);
    setLoading(false);
  };

  if (loading) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Box 
          display="flex" 
          justifyContent="center" 
          alignItems="center" 
          minHeight="100vh"
        >
          <CircularProgress />
        </Box>
      </ThemeProvider>
    );
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
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
    </ThemeProvider>
  );
}

export default App;