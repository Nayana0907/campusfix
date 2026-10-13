import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { 
  Search, 
  RotateCcw, 
  ChevronLeft, 
  ChevronRight,
  UserCheck,
  FileText
} from 'lucide-react';
import { adminService } from '../../api/adminService';
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

export const AdminComplaintsPage = () => {
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

  // Filter state
  const [search, setSearch] = useState(searchParams.get('search') || '');
  const [category, setCategory] = useState(searchParams.get('category') || '');
  const [priority, setPriority] = useState(searchParams.get('priority') || '');
  const [status, setStatus] = useState(searchParams.get('status') || '');
  const [building, setBuilding] = useState('');
  const [page, setPage] = useState(0);

  // Assignment Modal State
  const [assignModal, setAssignModal] = useState({
    open: false,
    complaintId: null,
    staffId: '',
    staffList: [],
    loading: false,
  });

  const loadComplaints = useCallback(async () => {
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
      if (building.trim()) params.building = building.trim();

      const result = await adminService.getAllComplaints(params);
      setData(result);
    } catch (err) {
      setError(err.message || 'Unable to load complaint registry');
    } finally {
      setLoading(false);
    }
  }, [page, search, category, priority, status, building]);

  useEffect(() => {
    loadComplaints();
  }, [loadComplaints]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    loadComplaints();
  };

  const handleResetFilters = () => {
    setSearch('');
    setCategory('');
    setPriority('');
    setStatus('');
    setBuilding('');
    setPage(0);
  };

  const openAssignModal = async (complaint) => {
    try {
      const staffData = await adminService.getStaffPerformance();
      setAssignModal({
        open: true,
        complaintId: complaint.id,
        staffId: complaint.assignedStaff?.id || '',
        staffList: staffData.items || [],
        loading: false,
      });
    } catch {
      setAssignModal({
        open: true,
        complaintId: complaint.id,
        staffId: '',
        staffList: [],
        loading: false,
      });
    }
  };

  const handleAssignSave = async (e) => {
    e.preventDefault();
    if (!assignModal.staffId) {
      addToast({ title: 'Select Staff', message: 'Please choose a maintenance technician.', type: 'danger' });
      return;
    }

    setAssignModal((prev) => ({ ...prev, loading: true }));
    try {
      await adminService.assignStaff(assignModal.complaintId, assignModal.staffId);
      addToast({ title: 'Staff Allocated', message: 'Work order allocated successfully.', type: 'success' });
      setAssignModal({ open: false, complaintId: null, staffId: '', staffList: [], loading: false });
      await loadComplaints();
    } catch (err) {
      addToast({ title: 'Allocation Failed', message: err.message, type: 'danger' });
      setAssignModal((prev) => ({ ...prev, loading: false }));
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Page Header */}
      <div>
        <h2 style={{ fontSize: 'var(--font-size-2xl)', fontWeight: 800, color: 'var(--color-text-primary)' }}>
          Campus Complaint Management Registry
        </h2>
        <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
          Global ticket tracking, maintenance staff assignments, and priority triage across all campus facilities.
        </p>
      </div>

      {/* Filter Toolbar Card */}
      <Card>
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
            <div style={{ position: 'relative' }}>
              <input
                type="text"
                placeholder="Search reference, title..."
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

            <input
              type="text"
              placeholder="Filter Building..."
              value={building}
              onChange={(e) => setBuilding(e.target.value)}
              style={{
                padding: '9px 12px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-card)',
                fontSize: 'var(--font-size-sm)',
              }}
            />
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
              Filter Complaints
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

      {/* Main Table */}
      <Card>
        {loading ? (
          <CardSkeleton />
        ) : data.content.length === 0 ? (
          <EmptyState
            icon={<FileText size={48} color="var(--color-text-muted)" />}
            title="No complaints match query"
            description="No records found matching the active filters."
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
                    <th style={{ padding: '12px 14px' }}>Reporter</th>
                    <th style={{ padding: '12px 14px' }}>Category</th>
                    <th style={{ padding: '12px 14px' }}>Priority</th>
                    <th style={{ padding: '12px 14px' }}>Status</th>
                    <th style={{ padding: '12px 14px' }}>Assigned Staff</th>
                    <th style={{ padding: '12px 14px' }}>Date</th>
                    <th style={{ padding: '12px 14px', textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((complaint) => (
                    <tr
                      key={complaint.id}
                      style={{
                        borderBottom: '1px solid var(--color-border-subtle)',
                        transition: 'background-color var(--transition-fast)',
                      }}
                    >
                      <td style={{ padding: '12px 14px', fontWeight: 600 }}>
                        <Link
                          to={`/complaints/${complaint.id}`}
                          style={{ color: 'var(--color-primary)' }}
                        >
                          {complaint.referenceNumber}
                        </Link>
                      </td>
                      <td style={{ padding: '12px 14px', maxWidth: '280px' }}>
                        <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>
                          {complaint.title}
                        </div>
                        <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                          {complaint.building}, {complaint.floor}, {complaint.room}
                        </div>
                      </td>
                      <td style={{ padding: '12px 14px', fontSize: 'var(--font-size-xs)' }}>
                        <div style={{ fontWeight: 600 }}>{complaint.reporter?.name}</div>
                        <div style={{ color: 'var(--color-text-muted)' }}>{complaint.reporter?.email}</div>
                      </td>
                      <td style={{ padding: '12px 14px' }}>
                        <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 500 }}>
                          {complaint.category}
                        </span>
                      </td>
                      <td style={{ padding: '12px 14px' }}>
                        <StatusBadge value={complaint.priority} size="sm" />
                      </td>
                      <td style={{ padding: '12px 14px' }}>
                        <StatusBadge value={complaint.status} size="sm" />
                      </td>
                      <td style={{ padding: '12px 14px', fontSize: 'var(--font-size-xs)' }}>
                        {complaint.assignedStaff ? (
                          <span style={{ fontWeight: 600, color: 'var(--color-primary)' }}>
                            {complaint.assignedStaff.name}
                          </span>
                        ) : (
                          <Button
                            variant="ghost"
                            size="sm"
                            style={{ color: 'var(--color-warning)', padding: '2px 6px' }}
                            onClick={() => openAssignModal(complaint)}
                          >
                            + Assign
                          </Button>
                        )}
                      </td>
                      <td style={{ padding: '12px 14px', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', whiteSpace: 'nowrap' }}>
                        {formatDate(complaint.createdAt)}
                      </td>
                      <td style={{ padding: '12px 14px', textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '6px' }}>
                          <Button
                            variant="ghost"
                            size="sm"
                            icon={<UserCheck size={14} />}
                            onClick={() => openAssignModal(complaint)}
                            title="Assign / Reassign Staff"
                          />
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => navigate(`/complaints/${complaint.id}`)}
                          >
                            Manage
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
                  Showing page {data.page + 1} of {data.totalPages} ({data.totalElements} complaints)
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

      {/* Staff Assignment Modal */}
      <Modal
        isOpen={assignModal.open}
        onClose={() => setAssignModal((prev) => ({ ...prev, open: false }))}
        title="Assign Maintenance Staff"
      >
        <form onSubmit={handleAssignSave} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600 }}>
              Select Technician
            </label>
            <select
              value={assignModal.staffId}
              onChange={(e) => setAssignModal((prev) => ({ ...prev, staffId: e.target.value }))}
              required
              style={{
                width: '100%',
                padding: '10px 14px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-card)',
                fontSize: 'var(--font-size-sm)',
              }}
            >
              <option value="">-- Choose Staff Member --</option>
              {assignModal.staffList.map((st) => (
                <option key={st.staffId} value={st.staffId}>
                  {st.staffName} ({st.staffEmail}) — Active Tasks: {st.inProgressCount || 0}
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <Button
              type="button"
              variant="outline"
              onClick={() => setAssignModal((prev) => ({ ...prev, open: false }))}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={assignModal.loading}
            >
              Save Allocation
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
