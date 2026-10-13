import React from 'react';

export const Skeleton = ({
  width = '100%',
  height = '16px',
  borderRadius = 'var(--radius-md)',
  style = {},
  className = '',
}) => {
  return (
    <div
      style={{
        width,
        height,
        borderRadius,
        backgroundColor: 'var(--color-border)',
        animation: 'pulse 1.5s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        ...style,
      }}
      className={className}
    />
  );
};

export const CardSkeleton = () => (
  <div
    style={{
      backgroundColor: 'var(--color-bg-card)',
      border: '1px solid var(--color-border)',
      borderRadius: 'var(--radius-lg)',
      padding: '24px',
      display: 'flex',
      flexDirection: 'column',
      gap: '16px',
    }}
  >
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <Skeleton width="40%" height="24px" />
      <Skeleton width="60px" height="24px" borderRadius="var(--radius-full)" />
    </div>
    <Skeleton width="100%" height="16px" />
    <Skeleton width="80%" height="16px" />
    <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
      <Skeleton width="30%" height="14px" />
      <Skeleton width="30%" height="14px" />
    </div>
  </div>
);
