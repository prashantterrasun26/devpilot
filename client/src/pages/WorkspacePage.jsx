import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Navbar } from '../components/Navbar';
import { FileTree } from '../components/FileTree';
import { ChatInterface } from '../components/ChatInterface';
import { MermaidViewer } from '../components/MermaidViewer';
import { 
  FolderGit2, 
  MessageSquare, 
  Network, 
  FileCode2, 
  Play, 
  CheckCircle2, 
  Clock, 
  AlertCircle,
  ExternalLink,
  ChevronLeft,
  X,
  Code
} from 'lucide-react';
import api from '../services/api';

export const WorkspacePage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [repo, setRepo] = useState(null);
  const [fileTree, setFileTree] = useState([]);
  const [activeTab, setActiveTab] = useState('chat'); // 'chat' | 'architecture' | 'code'
  const [selectedFile, setSelectedFile] = useState(null);
  const [fileContent, setFileContent] = useState('');
  const [fileLoading, setFileLoading] = useState(false);
  const [architecture, setArchitecture] = useState(null);
  const [archLoading, setArchLoading] = useState(false);

  useEffect(() => {
    fetchRepoDetails();
    fetchFileTree();
  }, [id]);

  const fetchRepoDetails = async () => {
    try {
      const res = await api.get(`/api/repositories/${id}`);
      if (res.data && res.data.success) {
        setRepo(res.data.data);
        if (res.data.data.architectureMermaid) {
          setArchitecture({
            mermaidDiagram: res.data.data.architectureMermaid,
            summary: res.data.data.architectureSummary,
          });
        }
      }
    } catch (err) {
      console.error('Failed to load repo:', err);
    }
  };

  const fetchFileTree = async () => {
    try {
      const res = await api.get(`/api/repositories/${id}/files`);
      if (res.data && res.data.success) {
        setFileTree(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load file tree:', err);
    }
  };

  const handleSelectFile = async (path, targetLine = null) => {
    setSelectedFile(path);
    setActiveTab('code');
    setFileLoading(true);
    try {
      const res = await api.get(`/api/repositories/${id}/file-content?path=${encodeURIComponent(path)}`);
      if (res.data && res.data.success) {
        setFileContent(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load file content:', err);
      setFileContent('// Could not load file content');
    } finally {
      setFileLoading(false);
    }
  };

  const handleGenerateArchitecture = async () => {
    setArchLoading(true);
    setActiveTab('architecture');
    try {
      const res = await api.post(`/api/repositories/${id}/architecture`);
      if (res.data && res.data.success) {
        setArchitecture(res.data.data);
      }
    } catch (err) {
      console.error('Failed to generate architecture:', err);
    } finally {
      setArchLoading(false);
    }
  };

  const isAnalyzing = repo && (repo.status === 'INDEXING' || repo.status === 'QUEUED');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>
      <Navbar />

      {/* Workspace Header Bar */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 24px',
        background: 'rgba(14, 19, 31, 0.95)',
        borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
        zIndex: 10,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <button
            onClick={() => navigate('/dashboard')}
            className="btn btn-secondary"
            style={{ padding: '6px 10px', fontSize: '0.8rem' }}
          >
            <ChevronLeft size={16} /> Back
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <FolderGit2 size={20} color="#38BDF8" />
            <span style={{ fontSize: '1.1rem', fontWeight: 700, color: '#F1F5F9' }}>
              {repo?.fullName || repo?.name || 'Repository Workspace'}
            </span>
            {repo?.defaultBranch && (
              <span style={{
                fontSize: '0.75rem',
                padding: '2px 8px',
                borderRadius: '4px',
                background: 'rgba(255, 255, 255, 0.06)',
                color: '#94A3B8',
                fontFamily: 'JetBrains Mono, monospace',
              }}>
                {repo.defaultBranch}
              </span>
            )}
          </div>

          {/* Status Indicator */}
          {repo?.status === 'READY' && (
            <span className="badge badge-ready"><CheckCircle2 size={12} /> Ready</span>
          )}
          {isAnalyzing && (
            <span className="badge badge-indexing"><Clock size={12} className="pulse-glow" /> Indexing Code</span>
          )}
        </div>

        {/* View Switcher Tabs */}
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            onClick={() => setActiveTab('chat')}
            className={`btn ${activeTab === 'chat' ? 'btn-primary' : 'btn-secondary'}`}
            style={{ padding: '8px 14px', fontSize: '0.85rem' }}
          >
            <MessageSquare size={15} /> AI Assistant
          </button>

          <button
            onClick={handleGenerateArchitecture}
            className={`btn ${activeTab === 'architecture' ? 'btn-primary' : 'btn-secondary'}`}
            style={{ padding: '8px 14px', fontSize: '0.85rem' }}
          >
            <Network size={15} /> Architecture
          </button>

          {selectedFile && (
            <button
              onClick={() => setActiveTab('code')}
              className={`btn ${activeTab === 'code' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ padding: '8px 14px', fontSize: '0.85rem' }}
            >
              <Code size={15} /> {selectedFile.split('/').pop()}
            </button>
          )}
        </div>
      </div>

      {/* Main Workspace Split Layout */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* Left Side: Repository File Explorer */}
        <div style={{
          width: '300px',
          background: '#0E131F',
          borderRight: '1px solid rgba(255, 255, 255, 0.08)',
          display: 'flex',
          flexDirection: 'column',
        }}>
          <div style={{
            padding: '12px 16px',
            borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
            fontSize: '0.75rem',
            fontWeight: 700,
            color: '#64748B',
            textTransform: 'uppercase',
            letterSpacing: '0.05em',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}>
            <span>EXPLORER</span>
            <span style={{ fontSize: '0.7rem', color: '#38BDF8' }}>{repo?.filesCount || fileTree.length} files</span>
          </div>

          <div style={{ flex: 1, overflowY: 'auto' }}>
            {fileTree.length > 0 ? (
              <FileTree nodes={fileTree} onSelectFile={handleSelectFile} selectedFile={selectedFile} />
            ) : (
              <div style={{ padding: '24px 16px', color: '#64748B', fontSize: '0.85rem', textAlign: 'center' }}>
                {isAnalyzing ? 'Scanning files...' : 'No files indexed yet. Click Analyze Repository.'}
              </div>
            )}
          </div>
        </div>

        {/* Right Side: Tab Contents (AI Chat / Architecture / File Content) */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', background: '#0B0F17' }}>
          {activeTab === 'chat' && (
            <ChatInterface
              repositoryId={id}
              onSelectSourceFile={(file, line) => handleSelectFile(file, line)}
            />
          )}

          {activeTab === 'architecture' && (
            <div style={{ flex: 1, overflowY: 'auto', padding: '32px' }}>
              <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                  <div>
                    <h2 style={{ fontSize: '1.4rem', fontWeight: 800 }}>High-Level System Architecture</h2>
                    <p style={{ color: '#94A3B8', fontSize: '0.9rem' }}>
                      Automatically detected layers, controllers, services, database entities, and external dependencies.
                    </p>
                  </div>
                  <button
                    onClick={handleGenerateArchitecture}
                    disabled={archLoading}
                    className="btn btn-secondary"
                    style={{ padding: '8px 14px' }}
                  >
                    {archLoading ? 'Regenerating...' : 'Regenerate'}
                  </button>
                </div>

                {archLoading ? (
                  <div style={{ textAlign: 'center', padding: '60px', color: '#38BDF8' }}>
                    Analyzing module dependencies and building Mermaid diagram...
                  </div>
                ) : architecture ? (
                  <div>
                    <MermaidViewer chart={architecture.mermaidDiagram} />

                    {architecture.summary && (
                      <div className="glass-panel" style={{ marginTop: '24px', padding: '20px' }}>
                        <h4 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '8px', color: '#38BDF8' }}>
                          Architecture Summary
                        </h4>
                        <p style={{ color: '#CBD5E1', fontSize: '0.9rem', lineHeight: 1.6 }}>
                          {architecture.summary}
                        </p>
                      </div>
                    )}

                    {architecture.apiEndpoints && architecture.apiEndpoints.length > 0 && (
                      <div className="glass-panel" style={{ marginTop: '16px', padding: '20px' }}>
                        <h4 style={{ fontSize: '0.95rem', fontWeight: 700, marginBottom: '12px', color: '#F1F5F9' }}>
                          Detected REST API Endpoints
                        </h4>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                          {architecture.apiEndpoints.map((ep, i) => (
                            <span key={i} style={{
                              padding: '4px 10px',
                              borderRadius: '6px',
                              background: 'rgba(56, 189, 248, 0.1)',
                              border: '1px solid rgba(56, 189, 248, 0.2)',
                              color: '#38BDF8',
                              fontFamily: 'JetBrains Mono, monospace',
                              fontSize: '0.8rem',
                            }}>
                              {ep}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ) : (
                  <div style={{ textAlign: 'center', padding: '60px', color: '#64748B' }}>
                    Click "Generate Architecture" to analyze repository components.
                  </div>
                )}
              </div>
            </div>
          )}

          {activeTab === 'code' && (
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
              {/* File Code Header */}
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '10px 20px',
                background: '#0E131F',
                borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <FileCode2 size={16} color="#38BDF8" />
                  <span style={{ fontSize: '0.85rem', fontFamily: 'JetBrains Mono, monospace', color: '#CBD5E1' }}>
                    {selectedFile}
                  </span>
                </div>
                <button
                  onClick={() => setActiveTab('chat')}
                  className="btn btn-secondary"
                  style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                >
                  <X size={14} /> Close
                </button>
              </div>

              {/* Code Viewer Area */}
              <div style={{ flex: 1, overflowY: 'auto', padding: '20px', background: '#080C14' }}>
                {fileLoading ? (
                  <div style={{ color: '#94A3B8', fontSize: '0.9rem' }}>Loading source content...</div>
                ) : (
                  <pre style={{
                    fontFamily: 'JetBrains Mono, monospace',
                    fontSize: '0.85rem',
                    lineHeight: 1.6,
                    color: '#E2E8F0',
                    margin: 0,
                    whiteSpace: 'pre-wrap',
                  }}>
                    <code>{fileContent}</code>
                  </pre>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
