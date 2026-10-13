import React from 'react';
import { Inbox } from 'lucide-react';
import { Button } from './Button';

export const EmptyState = ({
  icon = <Inbox size={48} color="var(--color-text-muted)" />,
  title = 'No items found',
  description = 'There are no items to display at this moment.',
  action = null,
  actionLabel = null,
  onAction = null,
}) => {
  const hasAction = Boolean(action || (actionLabel && onAction));

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: '48px 24px',
        backgroundColor: 'var(--color-bg-card)',
        border: '1px dashed var(--color-border)',
        borderRadius: 'var(--radius-lg)',
      }}
    >
      <div style={{ marginBottom: '16px', opacity: 0.85 }}>{icon}</div>
      <h3
        style={{
          fontSize: 'var(--font-size-base)',
          fontWeight: 600,
          color: 'var(--color-text-primary)',
          marginBottom: '6px',
        }}
      >
        {title}
      </h3>
      <p
        style={{
          fontSize: 'var(--font-size-sm)',
          color: 'var(--color-text-secondary)',
          maxWidth: '360px',
          marginBottom: hasAction ? '20px' : 0,
        }}
      >
        {description}
      </p>
      {action ? (
        <div>{action}</div>
      ) : actionLabel ? (
        <div>
          <Button variant="primary" size="sm" onClick={onAction}>
            {actionLabel}
          </Button>
        </div>
      ) : null}
    </div>
  );
};
