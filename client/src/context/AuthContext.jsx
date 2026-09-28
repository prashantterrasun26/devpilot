import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('devpilot_token');
    if (token) {
      fetchCurrentUser();
    } else {
      setLoading(false);
    }
  }, []);

  const fetchCurrentUser = async () => {
    try {
      const res = await api.get('/api/auth/me');
      if (res.data && res.data.success) {
        setUser(res.data.data);
      } else {
        logout();
      }
    } catch (err) {
      console.warn('Auth check failed:', err.message);
      logout();
    } finally {
      setLoading(false);
    }
  };

  const loginWithToken = (token) => {
    localStorage.setItem('devpilot_token', token);
    return fetchCurrentUser();
  };

  const demoLogin = async () => {
    setLoading(true);
    try {
      const res = await api.post('/api/auth/demo-login?username=octocat-developer');
      if (res.data && res.data.success && res.data.data.token) {
        localStorage.setItem('devpilot_token', res.data.data.token);
        setUser(res.data.data);
        return res.data.data;
      }
    } catch (err) {
      console.error('Demo login failed:', err);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('devpilot_token');
    localStorage.removeItem('devpilot_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, loginWithToken, demoLogin, logout, fetchCurrentUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
