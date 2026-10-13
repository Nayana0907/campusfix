import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute, PublicOnlyRoute } from './ProtectedRoute';
import { AppLayout } from '../components/layout/AppLayout';
import { DashboardDispatcher, ComplaintsDispatcher } from './RoleDispatcher';

// Auth & Error Pages
import { LoginPage } from '../pages/auth/LoginPage';
import { RegisterPage } from '../pages/auth/RegisterPage';
import { UnauthorizedPage } from '../pages/errors/UnauthorizedPage';
import { NotFoundPage } from '../pages/errors/NotFoundPage';

// Workflows & Detail Pages
import { SubmitComplaintPage } from '../pages/student/SubmitComplaintPage';
import { ComplaintDetailPage } from '../pages/student/ComplaintDetailPage';
import { StaffWorkOrdersPage } from '../pages/staff/StaffWorkOrdersPage';
import { AnalyticsDashboardPage } from '../pages/admin/AnalyticsDashboardPage';
import { ProfilePage } from '../pages/profile/ProfilePage';

export const AppRoutes = () => {
  return (
    <Routes>
      {/* Root redirect */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />

      {/* Public Auth Routes */}
      <Route
        path="/login"
        element={
          <PublicOnlyRoute>
            <LoginPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path="/register"
        element={
          <PublicOnlyRoute>
            <RegisterPage />
          </PublicOnlyRoute>
        }
      />

      {/* Error & Fallback Routes */}
      <Route path="/unauthorized" element={<UnauthorizedPage />} />

      {/* Authenticated Application Shell Routes */}
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        {/* Dynamic Role-Based Landing Views */}
        <Route path="/dashboard" element={<DashboardDispatcher />} />
        <Route path="/complaints" element={<ComplaintsDispatcher />} />

        {/* Complaint Submission (Student & Admin) */}
        <Route
          path="/complaints/new"
          element={
            <ProtectedRoute allowedRoles={['STUDENT', 'ADMIN']}>
              <SubmitComplaintPage />
            </ProtectedRoute>
          }
        />

        {/* Complaint Details & Action Workflow */}
        <Route path="/complaints/:id" element={<ComplaintDetailPage />} />

        {/* Maintenance Staff Tasks */}
        <Route
          path="/staff/assigned"
          element={
            <ProtectedRoute allowedRoles={['MAINTENANCE_STAFF', 'ADMIN']}>
              <StaffWorkOrdersPage />
            </ProtectedRoute>
          }
        />

        {/* Admin Intelligence & SLA Analytics */}
        <Route
          path="/admin/analytics"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AnalyticsDashboardPage />
            </ProtectedRoute>
          }
        />

        {/* Account Profile */}
        <Route path="/profile" element={<ProfilePage />} />
      </Route>

      {/* 404 Fallback */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
};
