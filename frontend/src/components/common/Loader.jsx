import React from 'react';
import { Loader2 } from 'lucide-react';

export const Loader = ({ size = 28, text = 'Loading...', fullScreen = false }) => {
  if (fullScreen) {
    return (
      <div
        style={{
          position: 'fixed',
          inset: 0,
          backgroundColor: 'var(--color-bg-main)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '16px',
          zIndex: 9999,
        }}
      >
        <Loader2
          size={40}
          color="var(--color-primary)"
          style={{ animation: 'spin 1s linear infinite' }}
        />
        {text && (
          <span
            style={{
              fontSize: 'var(--font-size-sm)',
              fontWeight: 500,
              color: 'var(--color-text-secondary)',
            }}
          >
            {text}
          </span>
        )}
      </div>
    );
  }

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '10px',
        padding: '24px',
        color: 'var(--color-text-secondary)',
      }}
    >
      <Loader2
        size={size}
        color="var(--color-primary)"
        style={{ animation: 'spin 1s linear infinite' }}
      />
      {text && (
        <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 500 }}>
          {text}
        </span>
      )}
    </div>
  );
};

export const FullScreenLoader = ({ message = 'Loading...' }) => {
  return <Loader fullScreen text={message} />;
};
