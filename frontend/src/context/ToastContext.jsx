import React, { createContext, useContext, useState, useCallback } from 'react';
import { CheckCircle2, AlertTriangle, AlertCircle, Info, X } from 'lucide-react';

const ToastContext = createContext(null);

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const addToast = useCallback(({ title, message, type = 'info', duration = 4000 }) => {
    const id = Date.now() + Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, title, message, type }]);

    if (duration > 0) {
      setTimeout(() => {
        removeToast(id);
      }, duration);
    }
  }, [removeToast]);

  const success = useCallback((message, title = 'Success') => {
    addToast({ title, message, type: 'success' });
  }, [addToast]);

  const error = useCallback((message, title = 'Error') => {
    addToast({ title, message, type: 'danger' });
  }, [addToast]);

  const warning = useCallback((message, title = 'Warning') => {
    addToast({ title, message, type: 'warning' });
  }, [addToast]);

  const info = useCallback((message, title = 'Information') => {
    addToast({ title, message, type: 'info' });
  }, [addToast]);

  return (
    <ToastContext.Provider value={{ addToast, removeToast, success, error, warning, info }}>
      {children}
      <div
        role="region"
        aria-live="polite"
        aria-label="Notifications"
        style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          zIndex: 9999,
          display: 'flex',
          flexDirection: 'column',
          gap: '12px',
          maxWidth: '400px',
          width: 'calc(100vw - 48px)',
          pointerEvents: 'none',
        }}
      >
        {toasts.map((toast) => (
          <ToastItem key={toast.id} toast={toast} onClose={() => removeToast(toast.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  );
};

const ToastItem = ({ toast, onClose }) => {
  const config = {
    success: {
      icon: <CheckCircle2 size={18} color="var(--color-success)" />,
      border: 'var(--color-success)',
      bg: 'var(--color-bg-card)',
    },
    danger: {
      icon: <AlertCircle size={18} color="var(--color-danger)" />,
      border: 'var(--color-danger)',
      bg: 'var(--color-bg-card)',
    },
    warning: {
      icon: <AlertTriangle size={18} color="var(--color-warning)" />,
      border: 'var(--color-warning)',
      bg: 'var(--color-bg-card)',
    },
    info: {
      icon: <Info size={18} color="var(--color-info)" />,
      border: 'var(--color-info)',
      bg: 'var(--color-bg-card)',
    },
  }[toast.type] || {
    icon: <Info size={18} />,
    border: 'var(--color-border)',
    bg: 'var(--color-bg-card)',
  };

  return (
    <div
      style={{
        pointerEvents: 'auto',
        backgroundColor: config.bg,
        borderLeft: `4px solid ${config.border}`,
        borderRadius: 'var(--radius-md)',
        boxShadow: 'var(--shadow-lg)',
        padding: '14px 16px',
        display: 'flex',
        alignItems: 'flex-start',
        gap: '12px',
        border: '1px solid var(--color-border)',
        borderLeftWidth: '4px',
        animation: 'slideIn 200ms ease-out',
      }}
    >
      <div style={{ marginTop: '2px', flexShrink: 0 }}>{config.icon}</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        {toast.title && (
          <div style={{ fontWeight: 600, fontSize: 'var(--font-size-sm)', color: 'var(--color-text-primary)' }}>
            {toast.title}
          </div>
        )}
        <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', wordBreak: 'break-word' }}>
          {toast.message}
        </div>
      </div>
      <button
        onClick={onClose}
        aria-label="Dismiss notification"
        style={{
          color: 'var(--color-text-muted)',
          padding: '4px',
          borderRadius: 'var(--radius-sm)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <X size={16} />
      </button>
    </div>
  );
};

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};
