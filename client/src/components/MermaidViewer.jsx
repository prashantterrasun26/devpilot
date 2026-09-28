import React, { useEffect, useRef, useState } from 'react';
import mermaid from 'mermaid';

mermaid.initialize({
  startOnLoad: false,
  theme: 'dark',
  securityLevel: 'loose',
  themeVariables: {
    darkMode: true,
    background: '#0E131F',
    primaryColor: '#0284c7',
    primaryTextColor: '#F1F5F9',
    primaryBorderColor: '#38BDF8',
    lineColor: '#818CF8',
    secondaryColor: '#1E293B',
    tertiaryColor: '#0F172A',
  },
});

export const MermaidViewer = ({ chart }) => {
  const containerRef = useRef(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!chart || !containerRef.current) return;

    let isMounted = true;
    const renderChart = async () => {
      try {
        setError(null);
        containerRef.current.innerHTML = '';
        const id = 'mermaid-' + Math.random().toString(36).substr(2, 9);
        const { svg } = await mermaid.render(id, chart);
        if (isMounted && containerRef.current) {
          containerRef.current.innerHTML = svg;
        }
      } catch (err) {
        console.error('Mermaid render error:', err);
        if (isMounted) {
          setError('Could not render architecture diagram format');
        }
      }
    };

    renderChart();

    return () => {
      isMounted = false;
    };
  }, [chart]);

  if (error) {
    return (
      <div style={{ padding: '20px', color: '#F87171', background: 'rgba(248, 113, 113, 0.1)', borderRadius: '8px' }}>
        <p>{error}</p>
        <pre style={{ fontSize: '0.8rem', marginTop: '8px', color: '#94A3B8' }}>{chart}</pre>
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      style={{
        display: 'flex',
        justifyContent: 'center',
        padding: '24px',
        overflowX: 'auto',
        background: 'rgba(14, 19, 31, 0.6)',
        borderRadius: '12px',
        border: '1px solid rgba(255, 255, 255, 0.08)',
      }}
    />
  );
};
