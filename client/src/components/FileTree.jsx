import React, { useState } from 'react';
import { Folder, FolderOpen, FileCode, ChevronRight, ChevronDown } from 'lucide-react';

export const FileTree = ({ nodes, onSelectFile, selectedFile }) => {
  return (
    <div style={{ padding: '8px', fontSize: '0.85rem' }}>
      {nodes.map((node) => (
        <TreeNode key={node.path || node.name} node={node} onSelectFile={onSelectFile} selectedFile={selectedFile} />
      ))}
    </div>
  );
};

const TreeNode = ({ node, onSelectFile, selectedFile }) => {
  const [isOpen, setIsOpen] = useState(false);
  const isDir = node.type === 'dir';
  const isSelected = selectedFile === node.path;

  const handleClick = () => {
    if (isDir) {
      setIsOpen(!isOpen);
    } else {
      onSelectFile(node.path);
    }
  };

  return (
    <div>
      <div
        onClick={handleClick}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          padding: '5px 8px',
          borderRadius: '6px',
          cursor: 'pointer',
          background: isSelected ? 'rgba(56, 189, 248, 0.15)' : 'transparent',
          color: isSelected ? '#38BDF8' : '#CBD5E1',
          transition: 'all 0.15s ease',
        }}
        onMouseEnter={(e) => {
          if (!isSelected) e.currentTarget.style.background = 'rgba(255, 255, 255, 0.05)';
        }}
        onMouseLeave={(e) => {
          if (!isSelected) e.currentTarget.style.background = 'transparent';
        }}
      >
        {isDir ? (
          <>
            {isOpen ? <ChevronDown size={14} color="#94A3B8" /> : <ChevronRight size={14} color="#94A3B8" />}
            {isOpen ? <FolderOpen size={16} color="#38BDF8" /> : <Folder size={16} color="#38BDF8" />}
          </>
        ) : (
          <>
            <span style={{ width: '14px' }} />
            <FileCode size={15} color="#94A3B8" />
          </>
        )}
        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {node.name}
        </span>
      </div>

      {isDir && isOpen && node.children && (
        <div style={{ paddingLeft: '14px', borderLeft: '1px solid rgba(255, 255, 255, 0.08)', marginLeft: '12px' }}>
          {node.children.map((child) => (
            <TreeNode key={child.path || child.name} node={child} onSelectFile={onSelectFile} selectedFile={selectedFile} />
          ))}
        </div>
      )}
    </div>
  );
};
