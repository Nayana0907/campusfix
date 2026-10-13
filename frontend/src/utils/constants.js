export const COMPLAINT_CATEGORIES = [
  { value: 'ELECTRICAL', label: 'Electrical' },
  { value: 'PLUMBING', label: 'Plumbing' },
  { value: 'WIFI', label: 'Wi-Fi & Network' },
  { value: 'FURNITURE', label: 'Furniture & Carpentry' },
  { value: 'CLEANING', label: 'Cleaning & Hygiene' },
  { value: 'EQUIPMENT', label: 'Lab & Appliance Equipment' },
  { value: 'OTHER', label: 'Other Facility' },
];

export const COMPLAINT_PRIORITIES = [
  { value: 'LOW', label: 'Low', color: 'var(--color-text-secondary)' },
  { value: 'MEDIUM', label: 'Medium', color: 'var(--color-primary)' },
  { value: 'HIGH', label: 'High', color: 'var(--color-warning)' },
  { value: 'URGENT', label: 'Urgent', color: 'var(--color-danger)' },
];

export const COMPLAINT_STATUSES = [
  { value: 'SUBMITTED', label: 'Submitted' },
  { value: 'ASSIGNED', label: 'Assigned' },
  { value: 'ACCEPTED', label: 'Accepted' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'RESOLVED', label: 'Resolved' },
  { value: 'CLOSED', label: 'Closed' },
  { value: 'REOPENED', label: 'Reopened' },
];

/**
 * Format ISO datetime string to localized human-readable string.
 * @param {string|Date} dateStr
 * @returns {string}
 */
export const formatDate = (dateStr) => {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return String(dateStr);
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(d);
  } catch {
    return String(dateStr);
  }
};

/**
 * Format relative time (e.g. 2 hours ago).
 * @param {string|Date} dateStr
 * @returns {string}
 */
export const formatRelativeTime = (dateStr) => {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    const now = new Date();
    const diffMs = now - d;
    const diffSecs = Math.floor(diffMs / 1000);
    const diffMins = Math.floor(diffSecs / 60);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffSecs < 60) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return formatDate(dateStr);
  } catch {
    return formatDate(dateStr);
  }
};
