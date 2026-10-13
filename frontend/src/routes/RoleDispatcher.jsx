import React from 'react';
import { useAuth } from '../context/AuthContext';
import { StudentDashboardPage } from '../pages/student/StudentDashboardPage';
import { AdminDashboardPage } from '../pages/admin/AdminDashboardPage';
import { StaffDashboardPage } from '../pages/staff/StaffDashboardPage';
import { MyComplaintsPage } from '../pages/student/MyComplaintsPage';
import { AdminComplaintsPage } from '../pages/admin/AdminComplaintsPage';
import { StaffWorkOrdersPage } from '../pages/staff/StaffWorkOrdersPage';

/**
 * Routes /dashboard to the tailored dashboard based on the user's confirmed role.
 */
export const DashboardDispatcher = () => {
  const { role } = useAuth();
  if (role === 'ADMIN') return <AdminDashboardPage />;
  if (role === 'MAINTENANCE_STAFF') return <StaffDashboardPage />;
  return <StudentDashboardPage />;
};

/**
 * Routes /complaints to the tailored complaints registry based on role.
 */
export const ComplaintsDispatcher = () => {
  const { role } = useAuth();
  if (role === 'ADMIN') return <AdminComplaintsPage />;
  if (role === 'MAINTENANCE_STAFF') return <StaffWorkOrdersPage />;
  return <MyComplaintsPage />;
};
