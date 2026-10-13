import { describe, it, expect, vi } from 'vitest';
import React from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { UnauthorizedPage } from '../pages/errors/UnauthorizedPage';
import { NotFoundPage } from '../pages/errors/NotFoundPage';
import { ProfilePage } from '../pages/profile/ProfilePage';
import * as AuthContextModule from '../context/AuthContext';
import * as ToastContextModule from '../context/ToastContext';

describe('Error Pages and Profile Page', () => {
  it('UnauthorizedPage renders 403 status and role badge', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: { name: 'Student Alex', email: 'alex@campus.edu' },
      role: 'STUDENT',
    });

    const { container } = render(
      <MemoryRouter>
        <UnauthorizedPage />
      </MemoryRouter>
    );

    expect(container.textContent).toContain('403 Forbidden');
    expect(container.textContent).toContain('Access Denied');
    expect(container.textContent).toContain('Current Role:');
    expect(container.textContent).toContain('Student');
  });

  it('NotFoundPage renders 404 status and navigation buttons', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      isAuthenticated: true,
    });

    const { container } = render(
      <MemoryRouter>
        <NotFoundPage />
      </MemoryRouter>
    );

    expect(container.textContent).toContain('404 Not Found');
    expect(container.textContent).toContain('Page Not Located');
    expect(container.textContent).toContain('Return to Dashboard');
  });

  it('ProfilePage renders user details and refresh button', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: { id: 10, name: 'Taylor Admin', email: 'admin@campus.edu', phone: '9876543210' },
      role: 'ADMIN',
      refreshProfile: vi.fn(),
    });
    vi.spyOn(ToastContextModule, 'useToast').mockReturnValue({
      addToast: vi.fn(),
    });

    const { container } = render(
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    );

    expect(container.textContent).toContain('User Profile & Account Information');
    expect(container.textContent).toContain('Taylor Admin');
    expect(container.textContent).toContain('admin@campus.edu');
    expect(container.textContent).toContain('Administrator');
  });
});
