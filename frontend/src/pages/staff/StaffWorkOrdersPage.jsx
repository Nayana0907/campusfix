import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { 
  Search, 
  RotateCcw, 
  ChevronLeft, 
  ChevronRight,
  CheckCircle,
  Wrench,
  Check,
  FileText
} from 'lucide-react';
import { staffService } from '../../api/staffService';
import { verificationService } from '../../api/verificationService';
import { useToast } from '../../context/ToastContext';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { EmptyState } from '../../components/common/EmptyState';
import { Modal } from '../../components/common/Modal';
import { CardSkeleton } from '../../components/common/Skeleton';
import { 
  COMPLAINT_CATEGORIES, 
  COMPLAINT_PRIORITIES, 
  COMPLAINT_STATUSES,
  formatDate 
} from '../../utils/constants';

export const StaffWorkOrdersPage = () => {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [searchParams] = useSearchParams();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [data, setData] = useState({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });

  const [search, setSearch] = useState(searchParams.get('search') || '');
  const [category, setCategory] = useState(searchParams.get('category') || '');
  const [priority, setPriority] = useState(searchParams.get('priority') || '');
  const [status, setStatus] = useState(searchParams.get('status') || '');
  const [page, setPage] = useState(0);

  // Quick Action Modal for Resolution
  const [resolveModal, setResolveModal] = useState({
    open: false,
    complaintId: null,
    notes: '',
    afterImage: null,
    submitting: false,
  });

  const loadWorkOrders = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {
        page,
        size: 10,
        sortBy: 'createdAt',
        direction: 'desc',
      };
      if (search.trim()) params.search = search.trim();
      if (category) params.category = category;
      if (priority) params.priority = priority;
      if (status) params.status = status;

      const result = await staffService.getAssignedComplaints(params);
      setData(result);
    } catch (err) {
      setError(err.message || 'Unable to retrieve assigned work orders');
    } finally {
      setLoading(false);
    }
  }, [page, search, category, priority, status]);

  useEffect(() => {
    loadWorkOrders();
  }, [loadWorkOrders]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    loadWorkOrders();
  };

  const handleResetFilters = () => {
    setSearch('');
    setCategory('');
    setPriority('');
    setStatus('');
    setPage(0);
  };

  // Quick Inline Actions
  const handleAccept = async (complaintId) => {
    try {
      await staffService.acceptAssignment(complaintId);
      addToast({ title: 'Accepted', message: 'Work order accepted.', type: 'success' });
      await loadWorkOrders();
    } catch (err) {
      addToast({ title: 'Failed', message: err.message, type: 'danger' });
    }
  };

  const handleStart = async (complaintId) => {
    try {
      await staffService.startWork(complaintId);
      addToast({ title: 'In Progress', message: 'Started repair work on ticket.', type: 'success' });
      await loadWorkOrders();
    } catch (err) {
      addToast({ title: 'Failed', message: err.message, type: 'danger' });
    }
  };

  const handleResolveSubmit = async (e) => {
    e.preventDefault();
    if (!resolveModal.notes || resolveModal.notes.trim().length < 5) {
      addToast({ title: 'Notes Required', message: 'Please specify repair notes (min 5 characters).', type: 'danger' });
      return;
    }

    setResolveModal((prev) => ({ ...prev, submitting: true }));
    try {
      await verificationService.submitResolution(
        resolveModal.complaintId,
        resolveModal.notes.trim(),
        resolveModal.afterImage
      );

      addToast({
        title: 'Resolution Submitted',
        message: 'Work marked resolved and sent for student verification.',
        type: 'success',
      });

      setResolveModal({ open: false, complaintId: null, notes: '', afterImage: null, submitting: false });
      await loadWorkOrders();
    } catch (err) {
      addToast({ title: 'Resolution Failed', message: err.message, type: 'danger' });
      setResolveModal((prev) => ({ ...prev, submitting: false }));
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header */}
      <div>
        <h2 style={{ fontSize: 'var(--font-size-2xl)', fontWeight: 800, color: 'var(--color-text-primary)' }}>
          Assigned Maintenance Work Orders
        </h2>
        <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
          Execute repair tickets assigned directly to your department. Accept allocations, initiate repairs, and upload resolution proof.
        </p>
      </div>

      {/* Filter Toolbar Card */}
      <Card>
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px' }}>
            <div style={{ position: 'relative' }}>
              <input
                type="text"
                placeholder="Search ref, defect..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                style={{
                  width: '100%',
                  padding: '9px 12px 9px 36px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border)',
                  backgroundColor: 'var(--color-bg-card)',
                  color: 'var(--color-text-primary)',
                  fontSize: 'var(--font-size-sm)',
                  boxSizing: 'border-box',
                }}
              />
              <Search
                size={16}
                color="var(--color-text-muted)"
                style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }}
              />
            </div>

            <select
              value={category}
              onChange={(e) => {
                setCategory(e.target.value);
                setPage(0);
              }}
              style={{
                padding: '9px 12px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-card)',
                fontSize: 'var(--font-size-sm)',
              }}
            >
              <option value="">All Categories</option>
              {COMPLAINT_CATEGORIES.map((c) => (
                <option key={c.value} value={c.value}>{c.label}</option>
              ))}
            </select>

            <select
              value={priority}
              onChange={(e) => {
                setPriority(e.target.value);
                setPage(0);
              }}
              style={{
                padding: '9px 12px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-card)',
                fontSize: 'var(--font-size-sm)',
              }}
            >
              <option value="">All Priorities</option>
              {COMPLAINT_PRIORITIES.map((p) => (
                <option key={p.value} value={p.value}>{p.label}</option>
              ))}
            </select>

            <select
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(0);
              }}
              style={{
                padding: '9px 12px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-card)',
                fontSize: 'var(--font-size-sm)',
              }}
            >
              <option value="">All Statuses</option>
              {COMPLAINT_STATUSES.map((s) => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              icon={<RotateCcw size={14} />}
              onClick={handleResetFilters}
            >
              Reset
            </Button>
            <Button
              type="submit"
              variant="secondary"
              size="sm"
              icon={<Search size={14} />}
            >
              Filter Tasks
            </Button>
          </div>
        </form>
      </Card>

      {error && (
        <div
          role="alert"
          style={{
            padding: '14px 18px',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'var(--color-danger-bg)',
            border: '1px solid var(--color-danger-border)',
            color: 'var(--color-danger-text)',
            fontSize: 'var(--font-size-sm)',
          }}
        >
          {error}
        </div>
      )}

      {/* Work Orders Table */}
      <Card>
        {loading ? (
          <CardSkeleton />
        ) : data.content.length === 0 ? (
          <EmptyState
            icon={<FileText size={48} color="var(--color-text-muted)" />}
            title="No tasks match query"
            description="You currently have no tasks matching the selected filters."
            actionLabel="Reset Filters"
            onAction={handleResetFilters}
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="table-container" style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: 'var(--font-size-sm)' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-xs)' }}>
                    <th style={{ padding: '12px 14px' }}>Ref #</th>
                    <th style={{ padding: '12px 14px' }}>Title & Location</th>
                    <th style={{ padding: '12px 14px' }}>Category</th>
                    <th style={{ padding: '12px 14px' }}>Priority</th>
                    <th style={{ padding: '12px 14px' }}>Status</th>
                    <th style={{ padding: '12px 14px' }}>Allocated Date</th>
                    <th style={{ padding: '12px 14px', textAlign: 'right' }}>Workflow Action</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((task) => (
                    <tr
                      key={task.id}
                      style={{
                        borderBottom: '1px solid var(--color-border-subtle)',
                        transition: 'background-color var(--transition-fast)',
                      }}
                    >
                      <td style={{ padding: '12px 14px', fontWeight: 600 }}>
                        <Link to={`/complaints/${task.id}`} style={{ color: 'var(--color-primary)' }}>
                          {task.referenceNumber}
                        </Link>
                      </td>
                      <td style={{ padding: '12px 14px', maxWidth: '300px' }}>
                        <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>
                          {task.title}
                        </div>
                        <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                          {task.building}, {task.floor}, {task.room}
                        </div>
                      </td>
                      <td style={{ padding: '12px 14px' }}>{task.category}</td>
                      <td style={{ padding: '12px 14px' }}>
                        <StatusBadge value={task.priority} size="sm" />
                      </td>
                      <td style={{ padding: '12px 14px' }}>
                        <StatusBadge value={task.status} size="sm" />
                      </td>
                      <td style={{ padding: '12px 14px', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
                        {formatDate(task.createdAt)}
                      </td>
                      <td style={{ padding: '12px 14px', textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '6px' }}>
                          {task.status === 'ASSIGNED' && (
                            <Button
                              variant="primary"
                              size="sm"
                              icon={<Check size={14} />}
                              onClick={() => handleAccept(task.id)}
                            >
                              Accept
                            </Button>
                          )}
                          {(task.status === 'ACCEPTED' || task.status === 'REOPENED') && (
                            <Button
                              variant="secondary"
                              size="sm"
                              icon={<Wrench size={14} />}
                              onClick={() => handleStart(task.id)}
                            >
                              Start
                            </Button>
                          )}
                          {task.status === 'IN_PROGRESS' && (
                            <Button
                              variant="primary"
                              size="sm"
                              icon={<CheckCircle size={14} />}
                              onClick={() => setResolveModal({ open: true, complaintId: task.id, notes: '', afterImage: null, submitting: false })}
                            >
                              Resolve
                            </Button>
                          )}
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => navigate(`/complaints/${task.id}`)}
                          >
                            Details
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            {data.totalPages > 1 && (
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  paddingTop: '12px',
                  borderTop: '1px solid var(--color-border)',
                  fontSize: 'var(--font-size-sm)',
                  color: 'var(--color-text-secondary)',
                }}
              >
                <div>
                  Showing page {data.page + 1} of {data.totalPages} ({data.totalElements} tasks)
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <Button
                    variant="outline"
                    size="sm"
                    icon={<ChevronLeft size={16} />}
                    disabled={data.page === 0}
                    onClick={() => setPage((p) => Math.max(p - 1, 0))}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    icon={<ChevronRight size={16} />}
                    disabled={data.page >= data.totalPages - 1}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </div>
        )}
      </Card>

      {/* Resolution Submission Modal */}
      <Modal
        isOpen={resolveModal.open}
        onClose={() => setResolveModal((prev) => ({ ...prev, open: false }))}
        title="Submit Work Order Resolution"
      >
        <form onSubmit={handleResolveSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>
              Resolution Notes <span style={{ color: 'var(--color-danger)' }}>*</span>
            </label>
            <textarea
              rows={4}
              value={resolveModal.notes}
              onChange={(e) => setResolveModal((prev) => ({ ...prev, notes: e.target.value }))}
              placeholder="Detail repair actions performed, replaced parts, and checks done..."
              required
              style={{
                width: '100%',
                padding: '10px 14px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                fontFamily: 'inherit',
                fontSize: 'var(--font-size-sm)',
              }}
            />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>
              After-Repair Photo (Optional)
            </label>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) setResolveModal((prev) => ({ ...prev, afterImage: file }));
              }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <Button
              type="button"
              variant="outline"
              onClick={() => setResolveModal((prev) => ({ ...prev, open: false }))}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={resolveModal.submitting}
            >
              Submit Resolution
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
