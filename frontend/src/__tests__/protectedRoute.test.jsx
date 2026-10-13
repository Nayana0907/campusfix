import { describe, it, expect, vi } from 'vitest';
import React from 'react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { render } from '@testing-library/react';
import { ProtectedRoute, PublicOnlyRoute } from '../routes/ProtectedRoute';
import * as AuthContextModule from '../context/AuthContext';

describe('Route Protection & Guards', () => {
  it('redirects unauthenticated users to /login preserving destination', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      isAuthenticated: false,
      isLoading: false,
      role: null,
    });

    const { container } = render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <div>Secret Dashboard Content</div>
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<div>Login Page Display</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(container.textContent).toContain('Login Page Display');
    expect(container.textContent).not.toContain('Secret Dashboard Content');
  });

  it('redirects user to /unauthorized when role is forbidden', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      role: 'STUDENT',
    });

    const { container } = render(
      <MemoryRouter initialEntries={['/admin/analytics']}>
        <Routes>
          <Route
            path="/admin/analytics"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <div>Admin Secret Analytics Content</div>
              </ProtectedRoute>
            }
          />
          <Route path="/unauthorized" element={<div>Access Denied Page Display</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(container.textContent).toContain('Access Denied Page Display');
    expect(container.textContent).not.toContain('Admin Secret Analytics Content');
  });

  it('allows access when user has matching role', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      role: 'ADMIN',
    });

    const { container } = render(
      <MemoryRouter initialEntries={['/admin/analytics']}>
        <Routes>
          <Route
            path="/admin/analytics"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <div>Admin Secret Analytics Content</div>
              </ProtectedRoute>
            }
          />
          <Route path="/unauthorized" element={<div>Access Denied Page Display</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(container.textContent).toContain('Admin Secret Analytics Content');
  });

  it('PublicOnlyRoute redirects authenticated user away from login to dashboard', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      role: 'STUDENT',
    });

    const { container } = render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route
            path="/login"
            element={
              <PublicOnlyRoute>
                <div>Login Form Display</div>
              </PublicOnlyRoute>
            }
          />
          <Route path="/dashboard" element={<div>Student Dashboard Content</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(container.textContent).toContain('Student Dashboard Content');
    expect(container.textContent).not.toContain('Login Form Display');
  });
});
