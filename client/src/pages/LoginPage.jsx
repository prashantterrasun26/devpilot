import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { GitFork, Bot, Sparkles, Terminal, Shield, Cpu, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const LoginPage = () => {
  const { demoLogin } = useAuth();
  const navigate = useNavigate();
  const [loadingDemo, setLoadingDemo] = useState(false);

  const handleGitHubOAuth = () => {
    // Redirects to backend OAuth endpoint which connects with GitHub OAuth App
    window.location.href = 'http://localhost:8080/oauth2/authorization/github';
  };

  const handleDemoAccess = async () => {
    setLoadingDemo(true);
    try {
      await demoLogin();
      navigate('/dashboard');
    } catch (err) {
      alert('Demo login failed: ' + err.message);
    } finally {
      setLoadingDemo(false);
    }
  };

  return (
    <div style={{
      minHeight: 'calc(100vh - 72px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '40px 20px',
      position: 'relative',
    }}>
      {/* Glow Orbs */}
      <div style={{
        position: 'absolute',
        top: '20%',
        left: '30%',
        width: '350px',
        height: '350px',
        background: 'radial-gradient(circle, rgba(56, 189, 248, 0.15) 0%, transparent 70%)',
        filter: 'blur(40px)',
        pointerEvents: 'none',
      }} />

      <div className="glass-panel" style={{
        maxWidth: '480px',
        width: '100%',
        padding: '40px',
        textAlign: 'center',
        position: 'relative',
        zIndex: 1,
      }}>
        {/* Logo Badge */}
        <div style={{
          width: '64px',
          height: '64px',
          borderRadius: '16px',
          background: 'linear-gradient(135deg, #0284c7 0%, #38bdf8 100%)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto 24px',
          boxShadow: '0 0 30px rgba(56, 189, 248, 0.4)',
        }}>
          <Bot size={36} color="#070E1A" strokeWidth={2.5} />
        </div>

        <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: '8px', letterSpacing: '-0.02em' }}>
          DevPilot
        </h1>
        <p style={{ color: '#94A3B8', fontSize: '1rem', marginBottom: '32px' }}>
          Understand any GitHub repository with AI.
        </p>

        {/* Feature Pills */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '32px', textAlign: 'left' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.875rem', color: '#CBD5E1' }}>
            <Cpu size={16} color="#38BDF8" />
            <span>Parse source trees & generate vector embeddings</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.875rem', color: '#CBD5E1' }}>
            <Sparkles size={16} color="#818CF8" />
            <span>RAG-powered chat with exact source line citations</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.875rem', color: '#CBD5E1' }}>
            <Terminal size={16} color="#34D399" />
            <span>Mermaid architecture diagrams & code flow analysis</span>
          </div>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <button
            onClick={handleGitHubOAuth}
            className="btn btn-primary"
            style={{ width: '100%', padding: '14px', fontSize: '1rem' }}
          >
            <GitFork size={20} />
            Continue with GitHub
          </button>

          <button
            onClick={handleDemoAccess}
            disabled={loadingDemo}
            className="btn btn-secondary"
            style={{ width: '100%', padding: '12px', fontSize: '0.9rem' }}
          >
            <Sparkles size={16} color="#38BDF8" />
            {loadingDemo ? 'Preparing workspace...' : 'Explore Demo Workspace (No GitHub OAuth key needed)'}
          </button>
        </div>

        <div style={{ marginTop: '24px', fontSize: '0.75rem', color: '#64748B' }}>
          Production OAuth 2.0 • PostgreSQL pgvector • Spring AI
        </div>
      </div>
    </div>
  );
};
