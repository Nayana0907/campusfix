import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  BarChart3, 
  FileText, 
  Clock, 
  CheckCircle2, 
  AlertTriangle, 
  RotateCcw,
  Users,
  Activity
} from 'lucide-react';
import { adminService } from '../../api/adminService';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { CardSkeleton } from '../../components/common/Skeleton';

export const AdminDashboardPage = () => {
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [summary, setSummary] = useState(null);

  useEffect(() => {
    let isMounted = true;
    const fetchSummary = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await adminService.getSummary();
        if (isMounted) setSummary(data);
      } catch (err) {
        if (isMounted) setError(err.message || 'Unable to load administrative summary');
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchSummary();
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
      {/* Overview Banner */}
      <Card hoverable={false} style={{ borderLeft: '4px solid var(--color-primary)', background: 'var(--gradient-hero)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <h2 style={{ fontSize: 'var(--font-size-2xl)', fontWeight: 800, color: 'var(--color-text-primary)' }}>
              Campus Facilities & Maintenance Command Center
            </h2>
            <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)', marginTop: '4px' }}>
              Real-time campus complaint triage, maintenance technician allocations, and facility SLA intelligence.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <Button
              variant="outline"
              icon={<BarChart3 size={16} />}
              onClick={() => navigate('/admin/analytics')}
            >
              Analytics Intelligence
            </Button>
            <Button
              variant="primary"
              icon={<FileText size={16} />}
              onClick={() => navigate('/complaints')}
            >
              Manage Complaints
            </Button>
          </div>
        </div>
      </Card>

      {/* Critical Overdue Alert */}
      {summary?.overdue > 0 && (
        <div
          role="alert"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '16px 20px',
            borderRadius: 'var(--radius-lg)',
            backgroundColor: 'var(--color-danger-bg)',
            border: '1px solid var(--color-danger-border)',
            color: 'var(--color-danger-text)',
            flexWrap: 'wrap',
            gap: '12px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <AlertTriangle size={24} style={{ flexShrink: 0 }} />
            <div>
              <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)' }}>
                SLA Alert: {summary.overdue} Overdue Ticket{summary.overdue > 1 ? 's' : ''} Require Immediate Escalation
              </div>
              <div style={{ fontSize: 'var(--font-size-xs)', opacity: 0.9 }}>
                Resolution SLA breached for high-priority or pending complaints. Reassign or escalate immediately.
              </div>
            </div>
          </div>
          <Button
            variant="danger"
            size="sm"
            onClick={() => navigate('/complaints?status=IN_PROGRESS')}
          >
            Triage Overdue Tickets
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

      {/* Metric Cards Grid */}
      <div className="stats-grid">
        <Card hoverable onClick={() => navigate('/complaints')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <FileText size={18} color="var(--color-primary)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Total Complaints
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800 }}>
            {summary?.totalComplaints ?? 0}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/complaints?status=SUBMITTED')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <Clock size={18} color="var(--color-info)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Newly Submitted
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-info)' }}>
            {summary?.newlySubmitted ?? 0}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/complaints?status=ASSIGNED')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <Users size={18} color="var(--color-primary)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Assigned / Accepted
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-primary)' }}>
            {(summary?.assigned ?? 0) + (summary?.accepted ?? 0)}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/complaints?status=IN_PROGRESS')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <Activity size={18} color="var(--color-warning)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              In Progress
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-warning)' }}>
            {summary?.inProgress ?? 0}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/complaints?status=RESOLVED')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <CheckCircle2 size={18} color="var(--color-success)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Resolved (Awaiting Verify)
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-success)' }}>
            {(summary?.resolved ?? 0) + (summary?.awaitingVerification ?? 0)}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/complaints?status=REOPENED')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <RotateCcw size={18} color="var(--color-danger)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Reopened
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-danger)' }}>
            {summary?.reopened ?? 0}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/complaints?status=CLOSED')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <CheckCircle2 size={18} color="var(--color-text-secondary)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Closed & Verified
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800 }}>
            {summary?.closed ?? 0}
          </div>
        </Card>

        <Card hoverable onClick={() => navigate('/admin/analytics')} style={{ cursor: 'pointer' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <BarChart3 size={18} color="var(--color-primary)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Live SLA Engine
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-base)', fontWeight: 700, color: 'var(--color-primary)', marginTop: '8px' }}>
            Explore Reports →
          </div>
        </Card>
      </div>
    </div>
  );
};
