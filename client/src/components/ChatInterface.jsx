import React, { useState, useEffect, useRef } from 'react';
import { Send, Bot, User as UserIcon, Sparkles, FileText, ArrowRight, CornerDownLeft, Loader2, Workflow } from 'lucide-react';
import api from '../services/api';

const QUICK_PROMPTS = [
  "Explain this repository.",
  "Explain the authentication workflow.",
  "Where is JWT authentication implemented?",
  "Show me the flow from controller to database.",
  "What external APIs does this project use?",
  "Which classes are responsible for repository indexing?"
];

export const ChatInterface = ({ repositoryId, onSelectSourceFile }) => {
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: "Hello! I'm your DevPilot AI assistant. I've analyzed this repository and can explain its architecture, code flow, classes, or authentication pipelines. Ask me anything!",
      sources: []
    }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  const handleSend = async (questionText) => {
    const textToSend = questionText || input;
    if (!textToSend.trim() || loading) return;

    const userMessage = { role: 'user', content: textToSend, sources: [] };
    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setLoading(true);

    try {
      const res = await api.post(`/api/repositories/${repositoryId}/chat`, {
        question: textToSend,
      });

      if (res.data && res.data.success) {
        const { answer, sources, codeFlow } = res.data.data;
        setMessages((prev) => [
          ...prev,
          {
            role: 'assistant',
            content: answer,
            sources: sources || [],
            codeFlow: codeFlow || null
          }
        ]);
      }
    } catch (err) {
      console.error('Chat query failed:', err);
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: "Sorry, I encountered an issue analyzing the repository for your question. Please ensure the repository indexing is complete and try again.",
          sources: []
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', position: 'relative' }}>
      {/* Messages Scroll Area */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '20px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {messages.map((msg, idx) => (
          <div
            key={idx}
            style={{
              display: 'flex',
              gap: '12px',
              maxWidth: '85%',
              alignSelf: msg.role === 'user' ? 'flex-end' : 'flex-start',
            }}
          >
            {msg.role === 'assistant' && (
              <div style={{
                width: '32px',
                height: '32px',
                borderRadius: '8px',
                background: 'linear-gradient(135deg, #0284c7 0%, #38bdf8 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}>
                <Bot size={18} color="#070E1A" />
              </div>
            )}

            <div style={{
              background: msg.role === 'user' ? '#0284c7' : 'rgba(18, 24, 38, 0.9)',
              color: msg.role === 'user' ? '#FFFFFF' : '#F1F5F9',
              padding: '14px 18px',
              borderRadius: msg.role === 'user' ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
              border: msg.role === 'user' ? 'none' : '1px solid rgba(255, 255, 255, 0.08)',
              fontSize: '0.92rem',
              lineHeight: 1.6,
            }}>
              <div style={{ whiteSpace: 'pre-wrap' }}>{msg.content}</div>

              {/* Code Flow Trace Box */}
              {msg.codeFlow && (
                <div style={{
                  marginTop: '14px',
                  padding: '12px',
                  background: 'rgba(56, 189, 248, 0.08)',
                  border: '1px solid rgba(56, 189, 248, 0.25)',
                  borderRadius: '8px',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', color: '#38BDF8', fontWeight: 700, marginBottom: '6px' }}>
                    <Workflow size={14} /> DETECTED COMPONENT FLOW
                  </div>
                  <pre style={{
                    fontFamily: 'JetBrains Mono, monospace',
                    fontSize: '0.82rem',
                    color: '#E2E8F0',
                    margin: 0,
                    whiteSpace: 'pre-wrap',
                  }}>{msg.codeFlow}</pre>
                </div>
              )}

              {/* Sources Section */}
              {msg.sources && msg.sources.length > 0 && (
                <div style={{ marginTop: '14px', borderTop: '1px solid rgba(255, 255, 255, 0.08)', paddingTop: '10px' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94A3B8', textTransform: 'uppercase', marginBottom: '8px', letterSpacing: '0.05em' }}>
                    Repository Sources Referenced:
                  </div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                    {msg.sources.map((s, sIdx) => (
                      <button
                        key={sIdx}
                        onClick={() => onSelectSourceFile && onSelectSourceFile(s.file, s.startLine)}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '6px',
                          background: 'rgba(255, 255, 255, 0.06)',
                          border: '1px solid rgba(255, 255, 255, 0.12)',
                          padding: '4px 8px',
                          borderRadius: '6px',
                          color: '#38BDF8',
                          fontSize: '0.75rem',
                          fontFamily: 'JetBrains Mono, monospace',
                          cursor: 'pointer',
                          transition: 'all 0.15s ease',
                        }}
                        onMouseEnter={(e) => e.currentTarget.style.borderColor = '#38BDF8'}
                        onMouseLeave={(e) => e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.12)'}
                      >
                        <FileText size={12} />
                        <span>{s.file}:{s.startLine}-{s.endLine}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {msg.role === 'user' && (
              <div style={{
                width: '32px',
                height: '32px',
                borderRadius: '8px',
                background: '#1E293B',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}>
                <UserIcon size={18} color="#94A3B8" />
              </div>
            )}
          </div>
        ))}

        {loading && (
          <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
            <div style={{
              width: '32px',
              height: '32px',
              borderRadius: '8px',
              background: 'linear-gradient(135deg, #0284c7 0%, #38bdf8 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <Bot size={18} color="#070E1A" />
            </div>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '12px 16px',
              background: 'rgba(18, 24, 38, 0.9)',
              borderRadius: '16px',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              color: '#94A3B8',
              fontSize: '0.85rem',
            }}>
              <Loader2 size={16} className="pulse-glow" style={{ animation: 'spin 1.5s linear infinite' }} />
              Searching indexed code chunks & reasoning...
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Suggested Prompts Pill Bar */}
      <div style={{
        padding: '8px 20px',
        display: 'flex',
        gap: '8px',
        overflowX: 'auto',
        background: 'rgba(11, 15, 23, 0.7)',
        borderTop: '1px solid rgba(255, 255, 255, 0.05)',
      }}>
        {QUICK_PROMPTS.map((prompt, i) => (
          <button
            key={i}
            onClick={() => handleSend(prompt)}
            disabled={loading}
            style={{
              whiteSpace: 'nowrap',
              background: 'rgba(255, 255, 255, 0.04)',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              borderRadius: '20px',
              padding: '4px 12px',
              fontSize: '0.75rem',
              color: '#94A3B8',
              cursor: 'pointer',
              transition: 'all 0.15s ease',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.borderColor = '#38BDF8';
              e.currentTarget.style.color = '#38BDF8';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.08)';
              e.currentTarget.style.color = '#94A3B8';
            }}
          >
            {prompt}
          </button>
        ))}
      </div>

      {/* Input Form */}
      <div style={{ padding: '16px 20px', background: 'rgba(11, 15, 23, 0.95)', borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSend();
          }}
          style={{ display: 'flex', gap: '10px' }}
        >
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask anything about this repository (e.g., Explain the login flow, database queries)..."
            style={{
              flex: 1,
              background: '#151C2C',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              padding: '12px 16px',
              color: '#F1F5F9',
              fontSize: '0.9rem',
              outline: 'none',
            }}
          />
          <button
            type="submit"
            disabled={loading || !input.trim()}
            className="btn btn-primary"
            style={{ padding: '0 20px', opacity: (loading || !input.trim()) ? 0.5 : 1 }}
          >
            <Send size={18} />
          </button>
        </form>
      </div>
    </div>
  );
};
