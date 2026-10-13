import React from 'react';

export const Card = ({
  children,
  title,
  subtitle,
  action,
  footer,
  hoverable = false,
  padding = '24px',
  style = {},
  className = '',
  onClick,
  ...props
}) => {
  const isClickable = Boolean(onClick);

  return (
    <div
      role={isClickable ? 'button' : undefined}
      tabIndex={isClickable ? 0 : undefined}
      onClick={onClick}
      onKeyDown={(e) => {
        if (isClickable && (e.key === 'Enter' || e.key === ' ')) {
          e.preventDefault();
          onClick(e);
        }
      }}
      style={{
        backgroundColor: 'var(--color-bg-card)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-lg)',
        boxShadow: hoverable ? 'var(--shadow-sm)' : 'var(--shadow-xs)',
        transition: 'transform var(--transition-fast), box-shadow var(--transition-fast)',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        ...(hoverable || isClickable
          ? {
              cursor: 'pointer',
            }
          : {}),
        ...style,
      }}
      className={className}
      {...props}
    >
      {(title || subtitle || action) && (
        <div
          style={{
            padding: `${padding} ${padding} 0 ${padding}`,
            display: 'flex',
            alignItems: 'flex-start',
            justifyContent: 'space-between',
            gap: '16px',
            marginBottom: '16px',
          }}
        >
          <div>
            {title && (
              <h3
                style={{
                  fontSize: 'var(--font-size-lg)',
                  fontWeight: 600,
                  color: 'var(--color-text-primary)',
                  letterSpacing: '-0.01em',
                }}
              >
                {title}
              </h3>
            )}
            {subtitle && (
              <p
                style={{
                  fontSize: 'var(--font-size-sm)',
                  color: 'var(--color-text-secondary)',
                  marginTop: '4px',
                }}
              >
                {subtitle}
              </p>
            )}
          </div>
          {action && <div style={{ flexShrink: 0 }}>{action}</div>}
        </div>
      )}

      <div
        style={{
          padding: title || subtitle || action ? `0 ${padding} ${padding} ${padding}` : padding,
          flex: 1,
        }}
      >
        {children}
      </div>

      {footer && (
        <div
          style={{
            padding: `12px ${padding}`,
            borderTop: '1px solid var(--color-border)',
            backgroundColor: 'var(--color-bg-subtle)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: '12px',
          }}
        >
          {footer}
        </div>
      )}
    </div>
  );
};
