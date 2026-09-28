import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { Search, RefreshCw, FolderGit2, Star, GitFork, ArrowUpRight, Play, CheckCircle2, AlertCircle, Clock } from 'lucide-react';
import api from '../services/api';

export const DashboardPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [repositories, setRepositories] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [analyzingIds, setAnalyzingIds] = useState(new Set());

  const fetchRepositories = async () => {
    try {
      const res = await api.get('/api/repositories');
      if (res.data && res.data.success) {
        setRepositories(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load repositories:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRepositories();
  }, []);

  const handleSyncGitHub = async () => {
    setSyncing(true);
    try {
      const res = await api.post('/api/repositories/sync');
      if (res.data && res.data.success) {
        setRepositories(res.data.data);
      }
    } catch (err) {
      console.error('Failed to sync repositories:', err);
    } finally {
      setSyncing(false);
    }
  };

  const handleAnalyze = async (repoId, e) => {
    e.stopPropagation();
    setAnalyzingIds((prev) => new Set(prev).add(repoId));

    try {
      const res = await api.post(`/api/repositories/${repoId}/analyze`);
      if (res.data && res.data.success) {
        // Poll for progress updates
        pollStatus(repoId);
      }
    } catch (err) {
      console.error('Failed to start analysis:', err);
      setAnalyzingIds((prev) => {
        const next = new Set(prev);
        next.delete(repoId);
        return next;
      });
    }
  };

  const pollStatus = async (repoId) => {
    const interval = setInterval(async () => {
      try {
        const res = await api.get(`/api/repositories/${repoId}/status`);
        if (res.data && res.data.success) {
          const statusObj = res.data.data;
          setRepositories((prev) =>
            prev.map((r) => (r.id === repoId ? { ...r, ...statusObj } : r))
          );

          if (statusObj.status === 'READY' || statusObj.status === 'FAILED') {
            clearInterval(interval);
            setAnalyzingIds((prev) => {
              const next = new Set(prev);
              next.delete(repoId);
              return next;
            });
          }
        }
      } catch (err) {
        clearInterval(interval);
      }
    }, 2500);
  };

  const filteredRepos = repositories.filter(
    (r) =>
      r.name.toLowerCase().includes(search.toLowerCase()) ||
      (r.description && r.description.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '40px 24px' }}>
      {/* Top Banner */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '32px' }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 800, marginBottom: '6px' }}>
            Your Repositories
          </h1>
          <p style={{ color: '#94A3B8', fontSize: '0.95rem' }}>
            Select any repository to analyze its structure, generate embeddings, and chat with AI.
          </p>
        </div>

        <button
          onClick={handleSyncGitHub}
          disabled={syncing}
          className="btn btn-secondary"
        >
          <RefreshCw size={16} className={syncing ? 'pulse-glow' : ''} style={{ animation: syncing ? 'spin 1s linear infinite' : 'none' }} />
          {syncing ? 'Syncing GitHub...' : 'Sync GitHub Repos'}
        </button>
      </div>

      {/* Search Input Bar */}
      <div style={{ position: 'relative', marginBottom: '28px' }}>
        <Search size={18} color="#64748B" style={{ position: 'absolute', left: '16px', top: '14px' }} />
        <input
          type="text"
          placeholder="Search repositories by name or description..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{
            width: '100%',
            background: 'rgba(18, 24, 38, 0.7)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            borderRadius: '10px',
            padding: '12px 16px 12px 46px',
            color: '#F1F5F9',
            fontSize: '0.95rem',
            outline: 'none',
          }}
        />
      </div>

      {/* Repository Cards Grid */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: '#94A3B8' }}>
          Loading your repositories...
        </div>
      ) : filteredRepos.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '60px 24px' }}>
          <FolderGit2 size={48} color="#64748B" style={{ margin: '0 auto 16px' }} />
          <h3 style={{ fontSize: '1.2rem', marginBottom: '8px' }}>No repositories found</h3>
          <p style={{ color: '#94A3B8', marginBottom: '20px' }}>
            Click "Sync GitHub Repos" to load your authorized repositories.
          </p>
          <button onClick={handleSyncGitHub} className="btn btn-primary">
            <RefreshCw size={16} /> Sync Now
          </button>
        </div>
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
          gap: '20px',
        }}>
          {filteredRepos.map((repo) => {
            const isAnalyzing = analyzingIds.has(repo.id) || repo.status === 'INDEXING' || repo.status === 'QUEUED';
            const isReady = repo.status === 'READY';

            return (
              <div
                key={repo.id}
                className="glass-panel"
                onClick={() => navigate(`/workspace/${repo.id}`)}
                style={{
                  padding: '24px',
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  gap: '16px',
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '10px' }}>
                    <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#F1F5F9', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <FolderGit2 size={18} color="#38BDF8" />
                      {repo.name}
                    </h3>

                    {/* Status Badge */}
                    {isReady && <span className="badge badge-ready"><CheckCircle2 size={12} /> Ready</span>}
                    {isAnalyzing && <span className="badge badge-indexing"><Clock size={12} className="pulse-glow" /> Indexing</span>}
                    {repo.status === 'FAILED' && <span className="badge badge-failed"><AlertCircle size={12} /> Failed</span>}
                    {(!repo.status || repo.status === 'NOT_ANALYZED') && <span className="badge badge-not-analyzed">Not Analyzed</span>}
                  </div>

                  <p style={{
                    color: '#94A3B8',
                    fontSize: '0.85rem',
                    lineHeight: 1.5,
                    height: '40px',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical',
                    marginBottom: '16px',
                  }}>
                    {repo.description || 'No description provided'}
                  </p>

                  <div style={{ display: 'flex', gap: '12px', fontSize: '0.75rem', color: '#64748B' }}>
                    {repo.language && (
                      <span style={{ color: '#38BDF8', fontWeight: 600 }}>● {repo.language}</span>
                    )}
                    {repo.filesCount > 0 && <span>{repo.filesCount} files</span>}
                    {repo.chunksCount > 0 && <span>{repo.chunksCount} chunks</span>}
                  </div>
                </div>

                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  borderTop: '1px solid rgba(255, 255, 255, 0.06)',
                  paddingTop: '14px',
                }}>
                  {isReady ? (
                    <span style={{ fontSize: '0.85rem', color: '#38BDF8', display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600 }}>
                      Open Workspace <ArrowUpRight size={16} />
                    </span>
                  ) : (
                    <button
                      onClick={(e) => handleAnalyze(repo.id, e)}
                      disabled={isAnalyzing}
                      className="btn btn-outline"
                      style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                    >
                      <Play size={13} />
                      {isAnalyzing ? 'Analyzing Repository...' : 'Analyze Repository'}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
