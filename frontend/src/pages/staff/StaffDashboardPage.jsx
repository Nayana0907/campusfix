import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { 
  CheckSquare, 
  Clock, 
  CheckCircle2, 
  ArrowRight,
  Wrench,
  Inbox
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { staffService } from '../../api/staffService';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { CardSkeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { formatDate } from '../../utils/constants';

export const StaffDashboardPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [stats, setStats] = useState({
    assigned: 0,
    inProgress: 0,
    resolved: 0,
    total: 0,
  });

  useEffect(() => {
    let isMounted = true;
    const loadTasks = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await staffService.getAssignedComplaints({ page: 0, size: 20 });
        if (!isMounted) return;

        const items = data.content || [];
        setTasks(items);

        const total = data.totalElements || items.length;
        const assigned = items.filter((t) => t.status === 'ASSIGNED').length;
        const inProgress = items.filter((t) => t.status === 'IN_PROGRESS' || t.status === 'ACCEPTED').length;
        const resolved = items.filter((t) => t.status === 'RESOLVED' || t.status === 'CLOSED').length;

        setStats({ assigned, inProgress, resolved, total });
      } catch (err) {
        if (isMounted) setError(err.message || 'Unable to retrieve assigned tasks');
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    loadTasks();
    return () => {
      isMounted = false;
    };
  }, []);

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <CardSkeleton />
        <div className="stats-grid">
          <CardSkeleton />
          <CardSkeleton />
          <CardSkeleton />
          <CardSkeleton />
        </div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Welcome Banner */}
      <Card hoverable={false} style={{ borderLeft: '4px solid var(--color-primary)', background: 'var(--gradient-hero)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <h2 style={{ fontSize: 'var(--font-size-2xl)', fontWeight: 800, color: 'var(--color-text-primary)' }}>
              Welcome, {user?.name || 'Technician'}!
            </h2>
            <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)', marginTop: '4px' }}>
              Maintenance staff portal. Accept work orders, manage in-progress repairs, and upload resolution proof.
            </p>
          </div>

          <Button
            variant="primary"
            icon={<CheckSquare size={18} />}
            onClick={() => navigate('/staff/assigned')}
          >
            View All Work Orders
          </Button>
        </div>
      </Card>

      {/* Action Required: Awaiting Acceptance Alert */}
      {stats.assigned > 0 && (
        <div
          role="alert"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '16px 20px',
            borderRadius: 'var(--radius-lg)',
            backgroundColor: 'var(--color-primary-light)',
            border: '1px solid var(--color-primary-subtle)',
            color: 'var(--color-primary)',
            flexWrap: 'wrap',
            gap: '12px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Wrench size={22} style={{ flexShrink: 0 }} />
            <div>
              <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)' }}>
                {stats.assigned} New Task{stats.assigned > 1 ? 's' : ''} Awaiting Acceptance
              </div>
              <div style={{ fontSize: 'var(--font-size-xs)', opacity: 0.9 }}>
                Facility supervisors have allocated new work orders to you. Please accept to proceed.
              </div>
            </div>
          </div>
          <Button
            variant="primary"
            size="sm"
            onClick={() => navigate('/staff/assigned?status=ASSIGNED')}
          >
            Review New Assignments
          </Button>
        </div>
      )}

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

      {/* Workload Metric Cards */}
      <div className="stats-grid">
        <Card hoverable onClick={() => navigate('/staff/assigned')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <CheckSquare size={18} color="var(--color-primary)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Total Allocated
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800 }}>
            {stats.total}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/staff/assigned?status=ASSIGNED')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <Clock size={18} color="var(--color-info)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Awaiting Acceptance
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-info)' }}>
            {stats.assigned}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/staff/assigned?status=IN_PROGRESS')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <Wrench size={18} color="var(--color-warning)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Active / In Progress
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-warning)' }}>
            {stats.inProgress}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/staff/assigned?status=RESOLVED')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <CheckCircle2 size={18} color="var(--color-success)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Resolved Repairs
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-success)' }}>
            {stats.resolved}
          </div>
        </Card>
      </div>

      {/* Recent Work Orders */}
      <Card
        title="Immediate Maintenance Work Orders"
        action={
          <Link
            to="/staff/assigned"
            style={{
              fontSize: 'var(--font-size-xs)',
              fontWeight: 600,
              color: 'var(--color-primary)',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            <span>View All</span>
            <ArrowRight size={14} />
          </Link>
        }
      >
        {tasks.length === 0 ? (
          <EmptyState
            icon={<Inbox size={40} color="var(--color-text-muted)" />}
            title="No assigned work orders"
            description="You currently have no outstanding maintenance tickets assigned to you."
          />
        ) : (
          <div className="table-container" style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: 'var(--font-size-sm)' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-xs)' }}>
                  <th style={{ padding: '12px 14px' }}>Ref #</th>
                  <th style={{ padding: '12px 14px' }}>Defect & Location</th>
                  <th style={{ padding: '12px 14px' }}>Category</th>
                  <th style={{ padding: '12px 14px' }}>Priority</th>
                  <th style={{ padding: '12px 14px' }}>Status</th>
                  <th style={{ padding: '12px 14px' }}>Date</th>
                  <th style={{ padding: '12px 14px', textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {tasks.slice(0, 5).map((task) => (
                  <tr key={task.id} style={{ borderBottom: '1px solid var(--color-border-subtle)' }}>
                    <td style={{ padding: '12px 14px', fontWeight: 600 }}>
                      <Link to={`/complaints/${task.id}`} style={{ color: 'var(--color-primary)' }}>
                        {task.referenceNumber}
                      </Link>
                    </td>
                    <td style={{ padding: '12px 14px' }}>
                      <div style={{ fontWeight: 600 }}>{task.title}</div>
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
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => navigate(`/complaints/${task.id}`)}
                      >
                        Manage
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
};
