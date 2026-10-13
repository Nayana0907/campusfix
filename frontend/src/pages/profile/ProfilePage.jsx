import React, { useState } from 'react';
import { Mail, Phone, Shield, CheckCircle2, RefreshCw } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';

export const ProfilePage = () => {
  const { user, role, refreshProfile } = useAuth();
  const { addToast } = useToast();
  const [isRefreshing, setIsRefreshing] = useState(false);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      await refreshProfile();
      addToast({
        title: 'Profile Updated',
        message: 'Account details refreshed from backend.',
        type: 'success',
      });
    } catch {
      addToast({
        title: 'Refresh Failed',
        message: 'Could not refresh profile from backend.',
        type: 'danger',
      });
    } finally {
      setIsRefreshing(false);
    }
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <Card
        title="User Profile & Account Information"
        action={
          <Button
            variant="outline"
            size="sm"
            isLoading={isRefreshing}
            icon={<RefreshCw size={14} />}
            onClick={handleRefresh}
          >
            Refresh
          </Button>
        }
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '20px', paddingBottom: '24px', borderBottom: '1px solid var(--color-border)', flexWrap: 'wrap' }}>
          <div
            style={{
              width: '72px',
              height: '72px',
              borderRadius: 'var(--radius-full)',
              backgroundColor: 'var(--color-primary)',
              color: '#FFFFFF',
              fontWeight: 700,
              fontSize: 'var(--font-size-2xl)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: 'var(--shadow-md)',
            }}
          >
            {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
          </div>

          <div>
            <h2 style={{ fontSize: 'var(--font-size-xl)', fontWeight: 800, color: 'var(--color-text-primary)', marginBottom: '6px' }}>
              {user?.name || 'User'}
            </h2>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <StatusBadge value={role} />
              <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                User ID #{user?.id || '—'}
              </span>
            </div>
          </div>
        </div>

        {/* Profile details grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '20px', paddingTop: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
            <div style={{ color: 'var(--color-primary)', marginTop: '2px' }}>
              <Mail size={18} />
            </div>
            <div>
              <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Campus Email
              </div>
              <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)', marginTop: '2px' }}>
                {user?.email || 'N/A'}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
            <div style={{ color: 'var(--color-primary)', marginTop: '2px' }}>
              <Phone size={18} />
            </div>
            <div>
              <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Contact Phone
              </div>
              <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)', marginTop: '2px' }}>
                {user?.phone || 'Not provided'}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
            <div style={{ color: 'var(--color-primary)', marginTop: '2px' }}>
              <Shield size={18} />
            </div>
            <div>
              <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Assigned Role
              </div>
              <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-text-primary)', marginTop: '2px' }}>
                {role || 'STUDENT'}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
            <div style={{ color: 'var(--color-success)', marginTop: '2px' }}>
              <CheckCircle2 size={18} />
            </div>
            <div>
              <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Account Status
              </div>
              <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, color: 'var(--color-success)', marginTop: '2px' }}>
                Active & Verified
              </div>
            </div>
          </div>
        </div>
      </Card>

      <Card title="Security & Authentication Guidelines">
        <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
          <p style={{ marginBottom: '12px' }}>
            Your account is secured via JWT bearer tokens and role-based access control. All requests to protected campus endpoints require valid, unexpired bearer credentials.
          </p>
          <ul style={{ paddingLeft: '20px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <li>Tokens are kept strictly in browser session storage and cleared on sign-out.</li>
            <li>Role permissions are evaluated on every Spring Boot backend API request.</li>
            <li>For role changes or administrative access, contact your campus facility coordinator.</li>
          </ul>
        </div>
      </Card>
    </div>
  );
};
