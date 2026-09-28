import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Loader2 } from 'lucide-react';

export const OAuthCallback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginWithToken } = useAuth();

  useEffect(() => {
    const token = searchParams.get('token');
    if (token) {
      loginWithToken(token)
        .then(() => navigate('/dashboard'))
        .catch((err) => {
          console.error('Failed to authenticate with token:', err);
          navigate('/login');
        });
    } else {
      navigate('/login');
    }
  }, [searchParams, loginWithToken, navigate]);

  return (
    <div style={{
      minHeight: '80vh',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '16px',
    }}>
      <Loader2 size={36} color="#38BDF8" style={{ animation: 'spin 1.5s linear infinite' }} />
      <p style={{ color: '#94A3B8', fontSize: '1rem' }}>Authenticating with GitHub...</p>
    </div>
  );
};
