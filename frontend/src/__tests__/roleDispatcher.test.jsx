import { describe, it, expect, vi } from 'vitest';
import React from 'react';
import { render } from '@testing-library/react';
import { DashboardDispatcher, ComplaintsDispatcher } from '../routes/RoleDispatcher';
import * as AuthContextModule from '../context/AuthContext';

vi.mock('../pages/student/StudentDashboardPage', () => ({
  StudentDashboardPage: () => <div>Student Dashboard View</div>,
}));
vi.mock('../pages/admin/AdminDashboardPage', () => ({
  AdminDashboardPage: () => <div>Admin Dashboard View</div>,
}));
vi.mock('../pages/staff/StaffDashboardPage', () => ({
  StaffDashboardPage: () => <div>Staff Dashboard View</div>,
}));
vi.mock('../pages/student/MyComplaintsPage', () => ({
  MyComplaintsPage: () => <div>Student Complaints View</div>,
}));
vi.mock('../pages/admin/AdminComplaintsPage', () => ({
  AdminComplaintsPage: () => <div>Admin Complaints View</div>,
}));
vi.mock('../pages/staff/StaffWorkOrdersPage', () => ({
  StaffWorkOrdersPage: () => <div>Staff Work Orders View</div>,
}));

describe('Role Dispatchers', () => {
  describe('DashboardDispatcher', () => {
    it('renders StudentDashboardPage when role is STUDENT', () => {
      vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({ role: 'STUDENT' });
      const { container } = render(<DashboardDispatcher />);
      expect(container.textContent).toContain('Student Dashboard View');
    });

    it('renders AdminDashboardPage when role is ADMIN', () => {
      vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({ role: 'ADMIN' });
      const { container } = render(<DashboardDispatcher />);
      expect(container.textContent).toContain('Admin Dashboard View');
    });

    it('renders StaffDashboardPage when role is MAINTENANCE_STAFF', () => {
      vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({ role: 'MAINTENANCE_STAFF' });
      const { container } = render(<DashboardDispatcher />);
      expect(container.textContent).toContain('Staff Dashboard View');
    });
  });

  describe('ComplaintsDispatcher', () => {
    it('renders MyComplaintsPage for STUDENT', () => {
      vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({ role: 'STUDENT' });
      const { container } = render(<ComplaintsDispatcher />);
      expect(container.textContent).toContain('Student Complaints View');
    });

    it('renders AdminComplaintsPage for ADMIN', () => {
      vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({ role: 'ADMIN' });
      const { container } = render(<ComplaintsDispatcher />);
      expect(container.textContent).toContain('Admin Complaints View');
    });

    it('renders StaffWorkOrdersPage for MAINTENANCE_STAFF', () => {
      vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({ role: 'MAINTENANCE_STAFF' });
      const { container } = render(<ComplaintsDispatcher />);
      expect(container.textContent).toContain('Staff Work Orders View');
    });
  });
});
