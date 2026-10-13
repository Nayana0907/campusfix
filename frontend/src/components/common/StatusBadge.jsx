import React from 'react';

export const StatusBadge = ({ value, _type = 'status', size = 'md', style = {}, ...props }) => {
  if (!value) return null;

  const normalized = String(value).toUpperCase();

  const getBadgeStyle = () => {
    switch (normalized) {
      // Complaint Statuses
      case 'SUBMITTED':
        return { bg: '#F5F3FF', color: '#7C3AED', border: '#DDD6FE', label: 'Submitted' };
      case 'ASSIGNED':
        return { bg: '#EDE9FE', color: '#7C3AED', border: '#C4B5FD', label: 'Assigned' };
      case 'ACCEPTED':
        return { bg: '#F5F3FF', color: '#6D28D9', border: '#DDD6FE', label: 'Accepted' };
      case 'IN_PROGRESS':
        return { bg: '#FFFBEB', color: '#D97706', border: '#FDE68A', label: 'In Progress' };
      case 'RESOLVED':
        return { bg: '#ECFDF5', color: '#059669', border: '#A7F3D0', label: 'Resolved' };
      case 'CLOSED':
        return { bg: '#F3F4F6', color: '#4B5563', border: '#E5E7EB', label: 'Closed' };
      case 'REOPENED':
        return { bg: '#FEF2F2', color: '#DC2626', border: '#FECACA', label: 'Reopened' };

      // Priorities
      case 'URGENT':
        return { bg: '#FEF2F2', color: '#DC2626', border: '#FECACA', label: 'Urgent' };
      case 'HIGH':
        return { bg: '#FFF7ED', color: '#EA580C', border: '#FFEDD5', label: 'High' };
      case 'MEDIUM':
        return { bg: '#FFFBEB', color: '#D97706', border: '#FDE68A', label: 'Medium' };
      case 'LOW':
        return { bg: '#ECFDF5', color: '#059669', border: '#A7F3D0', label: 'Low' };

      // User Roles
      case 'ADMIN':
        return { bg: '#EDE9FE', color: '#7C3AED', border: '#DDD6FE', label: 'Administrator' };
      case 'MAINTENANCE_STAFF':
        return { bg: '#F5F3FF', color: '#6D28D9', border: '#DDD6FE', label: 'Maintenance Staff' };
      case 'STUDENT':
        return { bg: '#F3F4F6', color: '#374151', border: '#E5E7EB', label: 'Student' };

      // Verification Statuses
      case 'PENDING':
        return { bg: '#FFFBEB', color: '#D97706', border: '#FDE68A', label: 'Verification Pending' };
      case 'VERIFIED':
        return { bg: '#ECFDF5', color: '#059669', border: '#A7F3D0', label: 'Verified' };
      case 'REJECTED':
        return { bg: '#FEF2F2', color: '#DC2626', border: '#FECACA', label: 'Rejected' };

      default:
        return { bg: 'var(--color-bg-subtle)', color: 'var(--color-text-secondary)', border: 'var(--color-border)', label: normalized };
    }
  };

  const badge = getBadgeStyle();
  const isSmall = size === 'sm';

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: isSmall ? '2px 8px' : '4px 10px',
        fontSize: isSmall ? 'var(--font-size-xs)' : '0.8125rem',
        fontWeight: 600,
        borderRadius: 'var(--radius-full)',
        backgroundColor: badge.bg,
        color: badge.color,
        border: `1px solid ${badge.border}`,
        lineHeight: 1.2,
        whiteSpace: 'nowrap',
        ...style,
      }}
      {...props}
    >
      {badge.label}
    </span>
  );
};
