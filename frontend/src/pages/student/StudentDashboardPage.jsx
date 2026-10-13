import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { 
  PlusCircle, 
  FileText, 
  Clock, 
  CheckCircle2, 
  AlertCircle, 
  ArrowRight,
  Inbox
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { complaintService } from '../../api/complaintService';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { CardSkeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { formatDate } from '../../utils/constants';

export const StudentDashboardPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [complaints, setComplaints] = useState([]);
  const [stats, setStats] = useState({
    total: 0,
    inProgress: 0,
    awaitingVerification: 0,
    closed: 0,
  });

  useEffect(() => {
    let isMounted = true;

    const loadData = async () => {
      setLoading(true);
      setError(null);
      try {
        // Fetch student's complaints (page 0, size 20 to compute personal stats)
        const data = await complaintService.getMyComplaints({ page: 0, size: 20 });
        if (!isMounted) return;

        const items = data.content || [];
        setComplaints(items);

        // Compute metrics directly from student's real data
        const total = data.totalElements || items.length;
        const inProgress = items.filter(
          (c) => c.status === 'IN_PROGRESS' || c.status === 'ACCEPTED' || c.status === 'ASSIGNED'
        ).length;
        const awaitingVerification = items.filter(
          (c) => c.status === 'RESOLVED' || c.verificationStatus === 'PENDING'
        ).length;
        const closed = items.filter((c) => c.status === 'CLOSED').length;

        setStats({ total, inProgress, awaitingVerification, closed });
      } catch (err) {
        if (isMounted) {
          setError(err.message || 'Unable to load student complaints');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    loadData();
    return () => {
      isMounted = false;
    };
  }, []);

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
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
              Welcome back, {user?.name || 'Student'}!
            </h2>
            <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)', marginTop: '4px' }}>
              Campus maintenance portal. Report facility defects, monitor repair workflows, and verify completed resolutions.
            </p>
          </div>

          <Button
            variant="primary"
            icon={<PlusCircle size={18} />}
            onClick={() => navigate('/complaints/new')}
          >
            Report Maintenance Issue
          </Button>
        </div>
      </Card>

      {/* Verification Attention Alert */}
      {stats.awaitingVerification > 0 && (
        <div
          role="alert"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '16px 20px',
            borderRadius: 'var(--radius-lg)',
            backgroundColor: 'var(--color-warning-bg)',
            border: '1px solid var(--color-warning-border)',
            color: 'var(--color-warning-text)',
            flexWrap: 'wrap',
            gap: '12px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <AlertCircle size={22} style={{ flexShrink: 0 }} />
            <div>
              <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)' }}>
                Action Required: {stats.awaitingVerification} Repair{stats.awaitingVerification > 1 ? 's' : ''} Awaiting Your Verification
              </div>
              <div style={{ fontSize: 'var(--font-size-xs)', opacity: 0.9 }}>
                Maintenance staff has resolved your reported issue. Please review the repair notes and confirm whether it is fixed.
              </div>
            </div>
          </div>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => navigate('/complaints?status=RESOLVED')}
          >
            Review Repairs
          </Button>
        </div>
      )}

      {/* Summary Cards */}
      <div className="stats-grid">
        <Card hoverable onClick={() => navigate('/complaints')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-primary-light)',
                color: 'var(--color-primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <FileText size={18} />
            </div>
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Total Reported
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-text-primary)' }}>
            {stats.total}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/complaints?status=IN_PROGRESS')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-warning-bg)',
                color: 'var(--color-warning)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Clock size={18} />
            </div>
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Under Progress
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-text-primary)' }}>
            {stats.inProgress}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/complaints?status=RESOLVED')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-info-bg)',
                color: 'var(--color-info)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <CheckCircle2 size={18} />
            </div>
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Awaiting Verification
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-text-primary)' }}>
            {stats.awaitingVerification}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/complaints?status=CLOSED')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-success-bg)',
                color: 'var(--color-success)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <CheckCircle2 size={18} />
            </div>
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Closed & Verified
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-text-primary)' }}>
            {stats.closed}
          </div>
        </Card>
      </div>

      {/* Error notification if API failed */}
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

      {/* Recent Complaints Table */}
      <Card
        title="Recent Reported Issues"
        action={
          <Link
            to="/complaints"
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
        {complaints.length === 0 ? (
          <EmptyState
            icon={<Inbox size={40} color="var(--color-text-muted)" />}
            title="No complaints reported yet"
            description="Whenever you notice broken lights, water leakages, faulty equipment, or cleanliness issues on campus, file a report here."
            actionLabel="Submit First Complaint"
            onAction={() => navigate('/complaints/new')}
          />
        ) : (
          <div className="table-container" style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: 'var(--font-size-sm)' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-xs)' }}>
                  <th style={{ padding: '12px 14px' }}>Reference</th>
                  <th style={{ padding: '12px 14px' }}>Title & Location</th>
                  <th style={{ padding: '12px 14px' }}>Category</th>
                  <th style={{ padding: '12px 14px' }}>Priority</th>
                  <th style={{ padding: '12px 14px' }}>Status</th>
                  <th style={{ padding: '12px 14px' }}>Submitted</th>
                  <th style={{ padding: '12px 14px', textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {complaints.slice(0, 5).map((complaint) => (
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
                    <td style={{ padding: '12px 14px' }}>
                      <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>
                        {complaint.title}
                      </div>
                      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                        {complaint.building}, {complaint.floor}, {complaint.room}
                      </div>
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
                    <td style={{ padding: '12px 14px', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
                      {formatDate(complaint.createdAt)}
                    </td>
                    <td style={{ padding: '12px 14px', textAlign: 'right' }}>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => navigate(`/complaints/${complaint.id}`)}
                      >
                        Details
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
