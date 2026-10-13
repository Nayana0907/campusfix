import React, { useState, useEffect, useCallback } from 'react';
import { 
  BarChart, 
  Bar, 
  LineChart, 
  Line, 
  PieChart, 
  Pie, 
  Cell, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend, 
  ResponsiveContainer 
} from 'recharts';
import { 
  Clock, 
  CheckCircle, 
  AlertCircle,
  Users, 
  MapPin, 
  RotateCcw
} from 'lucide-react';
import { adminService } from '../../api/adminService';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { CardSkeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';

const PALETTE = ['#7C3AED', '#A78BFA', '#059669', '#D97706', '#DC2626', '#8B5CF6', '#C4B5FD', '#6B7280'];

export const AnalyticsDashboardPage = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Date filters
  const [dateFilter, setDateFilter] = useState('30d');

  // Data states from real backend endpoints
  const [monthlyTrends, setMonthlyTrends] = useState([]);
  const [categoriesData, setCategoriesData] = useState([]);
  const [prioritiesData, setPrioritiesData] = useState([]);
  const [slaMetrics, setSlaMetrics] = useState(null);
  const [staffPerformance, setStaffPerformance] = useState([]);
  const [locationData, setLocationData] = useState([]);

  const getDateRange = useCallback(() => {
    if (dateFilter === 'all') {
      return { from: undefined, to: undefined };
    }

    const now = new Date();
    const days = dateFilter === '7d' ? 7 : dateFilter === '30d' ? 30 : 90;
    const fromDate = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);

    return {
      from: fromDate.toISOString().split('T')[0],
      to: now.toISOString().split('T')[0],
    };
  }, [dateFilter]);

  const loadAnalytics = useCallback(async () => {
    setLoading(true);
    setError(null);
    const { from, to } = getDateRange();

    try {
      const [trendsRes, catRes, slaRes, staffRes, locRes] = await Promise.all([
        adminService.getMonthlyTrends(from, to),
        adminService.getCategoriesAndPriorities(from, to),
        adminService.getResolutionPerformance(from, to),
        adminService.getStaffPerformance({ from, to }),
        adminService.getLocationAnalytics({ from, to }),
      ]);

      setMonthlyTrends(trendsRes.dataPoints || []);
      setCategoriesData(catRes.categoryCounts || []);
      setPrioritiesData(catRes.priorityCounts || []);
      setSlaMetrics(slaRes);
      setStaffPerformance(staffRes.items || []);
      setLocationData(locRes.buildingCounts || []);
    } catch (err) {
      setError(err.message || 'Unable to fetch analytics data');
    } finally {
      setLoading(false);
    }
  }, [getDateRange]);

  useEffect(() => {
    loadAnalytics();
  }, [loadAnalytics]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Page Header & Filter Controls */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: 'var(--font-size-2xl)', fontWeight: 800, color: 'var(--color-text-primary)' }}>
            Maintenance Intelligence & SLA Analytics
          </h2>
          <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
            Live performance trends, category distributions, technician productivity, and turnaround times.
          </p>
        </div>

        {/* Date Filter Segmented Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', backgroundColor: 'var(--color-bg-card)', padding: '4px', borderRadius: 'var(--radius-lg)', border: '1px solid var(--color-border)' }}>
          {['7d', '30d', '90d', 'all'].map((range) => (
            <button
              key={range}
              type="button"
              onClick={() => setDateFilter(range)}
              style={{
                padding: '6px 12px',
                borderRadius: 'var(--radius-md)',
                fontSize: 'var(--font-size-xs)',
                fontWeight: 600,
                color: dateFilter === range ? '#FFFFFF' : 'var(--color-text-secondary)',
                backgroundColor: dateFilter === range ? 'var(--color-primary)' : 'transparent',
                cursor: 'pointer',
                transition: 'all var(--transition-fast)',
              }}
            >
              {range === '7d' ? 'Last 7 Days' : range === '30d' ? 'Last 30 Days' : range === '90d' ? 'Last 90 Days' : 'All Time'}
            </button>
          ))}
          <Button variant="ghost" size="sm" icon={<RotateCcw size={14} />} onClick={loadAnalytics} title="Reload Data" />
        </div>
      </div>

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

      {/* SLA Metric Cards */}
      <div className="stats-grid">
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <Clock size={18} color="var(--color-primary)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Avg. Resolution Time
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800 }}>
            {slaMetrics?.averageSubmissionToResolutionHours != null
              ? `${slaMetrics.averageSubmissionToResolutionHours.toFixed(1)} hrs`
              : '—'}
          </div>
          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', marginTop: '4px' }}>
            Median: {slaMetrics?.medianSubmissionToResolutionHours != null ? `${slaMetrics.medianSubmissionToResolutionHours.toFixed(1)}h` : '—'}
          </div>
        </Card>

        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <CheckCircle size={18} color="var(--color-success)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Verification Rate
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-success)' }}>
            {slaMetrics?.verifiedAndClosedRate != null
              ? `${(slaMetrics.verifiedAndClosedRate * 100).toFixed(0)}%`
              : '—'}
          </div>
          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', marginTop: '4px' }}>
            Confirmed & Closed by reporters
          </div>
        </Card>

        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <RotateCcw size={18} color="var(--color-warning)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              Reopen Rate
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-warning)' }}>
            {slaMetrics?.reopenedRate != null
              ? `${(slaMetrics.reopenedRate * 100).toFixed(1)}%`
              : '0.0%'}
          </div>
          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', marginTop: '4px' }}>
            Total Reopened: {slaMetrics?.totalReopenedInScope || 0}
          </div>
        </Card>

        <Card>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <AlertCircle size={18} color="var(--color-danger)" />
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase' }}>
              SLA Overdue Rate
            </span>
          </div>
          <div style={{ fontSize: 'var(--font-size-3xl)', fontWeight: 800, color: 'var(--color-danger)' }}>
            {slaMetrics?.overdueRate != null
              ? `${(slaMetrics.overdueRate * 100).toFixed(1)}%`
              : '0.0%'}
          </div>
          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', marginTop: '4px' }}>
            Breached Resolution SLA
          </div>
        </Card>
      </div>

      {loading ? (
        <CardSkeleton />
      ) : (
        <>
          {/* Chart Section 1: Monthly Trends (Line Chart) */}
          <Card title="Monthly Complaint Lifecycle Trends">
            {monthlyTrends.length === 0 ? (
              <EmptyState title="No trend data" description="No complaints logged in the selected window." />
            ) : (
              <div style={{ width: '100%', height: 320 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={monthlyTrends} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                    <XAxis dataKey="month" stroke="var(--color-text-secondary)" fontSize={12} />
                    <YAxis stroke="var(--color-text-secondary)" fontSize={12} allowDecimals={false} />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: 'var(--color-bg-card)',
                        borderColor: 'var(--color-border)',
                        borderRadius: 'var(--radius-md)',
                      }}
                    />
                    <Legend />
                    <Line type="monotone" dataKey="submittedCount" name="Submitted" stroke="#7C3AED" strokeWidth={2.5} dot={{ r: 4 }} activeDot={{ r: 6 }} />
                    <Line type="monotone" dataKey="resolvedCount" name="Resolved" stroke="#059669" strokeWidth={2.5} dot={{ r: 4 }} activeDot={{ r: 6 }} />
                    <Line type="monotone" dataKey="closedCount" name="Closed" stroke="#6B7280" strokeWidth={2} dot={{ r: 4 }} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            )}
          </Card>

          {/* Chart Section 2: Category Donut & Priority Bar Chart */}
          <div className="content-grid">
            <Card title="Complaint Category Breakdown">
              {categoriesData.length === 0 ? (
                <EmptyState title="No category data" description="No complaints available to categorize." />
              ) : (
                <div style={{ width: '100%', height: 280 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={categoriesData}
                        dataKey="count"
                        nameKey="category"
                        cx="50%"
                        cy="50%"
                        innerRadius={60}
                        outerRadius={90}
                        paddingAngle={4}
                      >
                        {categoriesData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={PALETTE[index % PALETTE.length]} />
                        ))}
                      </Pie>
                      <Tooltip
                        contentStyle={{
                          backgroundColor: 'var(--color-bg-card)',
                          borderColor: 'var(--color-border)',
                          borderRadius: 'var(--radius-md)',
                        }}
                      />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}
            </Card>

            <Card title="Priority Distribution">
              {prioritiesData.length === 0 ? (
                <EmptyState title="No priority data" description="No complaints available to prioritize." />
              ) : (
                <div style={{ width: '100%', height: 280 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={prioritiesData} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                      <XAxis dataKey="priority" stroke="var(--color-text-secondary)" fontSize={12} />
                      <YAxis stroke="var(--color-text-secondary)" fontSize={12} allowDecimals={false} />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: 'var(--color-bg-card)',
                          borderColor: 'var(--color-border)',
                          borderRadius: 'var(--radius-md)',
                        }}
                      />
                      <Bar dataKey="count" name="Tickets" fill="#7C3AED" radius={[6, 6, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </Card>
          </div>

          {/* Chart Section 3: Campus Building & Facility Analysis */}
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <MapPin size={18} color="var(--color-primary)" />
                <span>Complaints by Campus Facility & Building</span>
              </div>
            }
          >
            {locationData.length === 0 ? (
              <EmptyState title="No location data" description="No facility complaints recorded for this period." />
            ) : (
              <div style={{ width: '100%', height: 260 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={locationData} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                    <XAxis dataKey="building" stroke="var(--color-text-secondary)" fontSize={12} />
                    <YAxis stroke="var(--color-text-secondary)" fontSize={12} allowDecimals={false} />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: 'var(--color-bg-card)',
                        borderColor: 'var(--color-border)',
                        borderRadius: 'var(--radius-md)',
                      }}
                    />
                    <Bar dataKey="count" name="Complaints" fill="#A78BFA" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </Card>

          {/* Staff Performance Table */}
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Users size={18} color="var(--color-primary)" />
                <span>Technician Performance & Workload</span>
              </div>
            }
          >
            {staffPerformance.length === 0 ? (
              <EmptyState title="No staff records" description="No maintenance staff members found or allocated." />
            ) : (
              <div className="table-container" style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: 'var(--font-size-sm)' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-xs)' }}>
                      <th style={{ padding: '12px 14px' }}>Staff Name</th>
                      <th style={{ padding: '12px 14px' }}>Active Tasks</th>
                      <th style={{ padding: '12px 14px' }}>Resolved</th>
                      <th style={{ padding: '12px 14px' }}>Closed</th>
                      <th style={{ padding: '12px 14px' }}>Overdue</th>
                      <th style={{ padding: '12px 14px' }}>Avg Resolution Time</th>
                    </tr>
                  </thead>
                  <tbody>
                    {staffPerformance.map((st) => (
                      <tr key={st.staffId} style={{ borderBottom: '1px solid var(--color-border-subtle)' }}>
                        <td style={{ padding: '12px 14px', fontWeight: 600 }}>
                          <div>{st.staffName}</div>
                          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>{st.staffEmail}</div>
                        </td>
                        <td style={{ padding: '12px 14px', fontWeight: 600, color: 'var(--color-warning)' }}>
                          {(st.assignedCount || 0) + (st.inProgressCount || 0)}
                        </td>
                        <td style={{ padding: '12px 14px', fontWeight: 600, color: 'var(--color-success)' }}>
                          {st.resolvedCount || 0}
                        </td>
                        <td style={{ padding: '12px 14px' }}>
                          {st.closedCount || 0}
                        </td>
                        <td style={{ padding: '12px 14px', color: (st.overdueCount || 0) > 0 ? 'var(--color-danger)' : 'inherit' }}>
                          {st.overdueCount || 0}
                        </td>
                        <td style={{ padding: '12px 14px', fontSize: 'var(--font-size-xs)' }}>
                          {st.averageResolutionTimeHours != null
                            ? `${st.averageResolutionTimeHours.toFixed(1)} hrs`
                            : 'N/A'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </>
      )}
    </div>
  );
};
