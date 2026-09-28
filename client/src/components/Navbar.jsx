import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Bot, LogOut, GitFork, Sparkles, FolderGit2 } from 'lucide-react';
import { Link } from 'react-router-dom';

export const Navbar = () => {
  const { user, logout } = useAuth();

  return (
    <nav style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '16px 32px',
      background: 'rgba(11, 15, 23, 0.85)',
      backdropFilter: 'blur(12px)',
      borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
      position: 'sticky',
      top: 0,
      zIndex: 50,
    }}>
      <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '12px', textDecoration: 'none' }}>
        <div style={{
          width: '38px',
          height: '38px',
          borderRadius: '10px',
          background: 'linear-gradient(135deg, #0284c7 0%, #38bdf8 100%)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 0 16px rgba(56, 189, 248, 0.4)',
        }}>
          <Bot size={22} color="#070E1A" strokeWidth={2.5} />
        </div>
        <div>
          <span style={{ fontSize: '1.25rem', fontWeight: 800, letterSpacing: '-0.02em', color: '#F1F5F9' }}>
            Dev<span style={{ color: '#38BDF8' }}>Pilot</span>
          </span>
          <span style={{
            marginLeft: '8px',
            fontSize: '0.65rem',
            padding: '2px 6px',
            borderRadius: '4px',
            background: 'rgba(56, 189, 248, 0.15)',
            color: '#38BDF8',
            border: '1px solid rgba(56, 189, 248, 0.3)',
            fontWeight: 700,
            verticalAlign: 'middle',
          }}>AI</span>
        </div>
      </Link>

      <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
        {user ? (
          <>
            <Link to="/dashboard" style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              color: '#94A3B8',
              textDecoration: 'none',
              fontSize: '0.9rem',
              fontWeight: 500,
            }}>
              <FolderGit2 size={16} /> Repositories
            </Link>

            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <img
                src={user.avatarUrl || 'https://avatars.githubusercontent.com/u/583231?v=4'}
                alt={user.username}
                style={{ width: '32px', height: '32px', borderRadius: '50%', border: '1px solid #38BDF8' }}
              />
              <span style={{ fontSize: '0.875rem', fontWeight: 600, color: '#F1F5F9' }}>{user.username}</span>
              <button
                onClick={logout}
                className="btn btn-secondary"
                style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                title="Sign out"
              >
                <LogOut size={14} />
              </button>
            </div>
          </>
        ) : (
          <Link to="/login" className="btn btn-primary" style={{ padding: '8px 16px', fontSize: '0.85rem' }}>
            <GitFork size={16} /> Sign In
          </Link>
        )}
      </div>
    </nav>
  );
};
