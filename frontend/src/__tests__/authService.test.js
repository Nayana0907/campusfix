import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../api/axios';
import { authService } from '../api/authService';

vi.mock('../api/axios', () => {
  return {
    default: {
      post: vi.fn(),
      get: vi.fn(),
    },
  };
});

describe('authService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('login sends trimmed email and credentials to /api/auth/login', async () => {
    const mockAuthResponse = {
      accessToken: 'jwt-test-token-123',
      expiresIn: 86400,
      refreshToken: 'refresh-test-456',
      user: {
        id: 1,
        name: 'Alex Student',
        email: 'alex@campus.edu',
        role: 'STUDENT',
      },
    };
    api.post.mockResolvedValueOnce({ data: mockAuthResponse });

    const result = await authService.login({
      email: '  alex@campus.edu  ',
      password: 'secretPassword123',
    });

    expect(api.post).toHaveBeenCalledWith('/api/auth/login', {
      email: 'alex@campus.edu',
      password: 'secretPassword123',
    });
    expect(result.accessToken).toBe('jwt-test-token-123');
    expect(result.user.role).toBe('STUDENT');
  });

  it('register trims inputs and posts to /api/auth/register', async () => {
    const mockUserResponse = {
      id: 2,
      name: 'Priya Sharma',
      email: 'priya@campus.edu',
      role: 'STUDENT',
    };
    api.post.mockResolvedValueOnce({ data: mockUserResponse });

    const result = await authService.register({
      name: '  Priya Sharma  ',
      email: '  priya@campus.edu ',
      password: 'securePass123',
      phone: ' 9876543210 ',
    });

    expect(api.post).toHaveBeenCalledWith('/api/auth/register', {
      name: 'Priya Sharma',
      email: 'priya@campus.edu',
      password: 'securePass123',
      phone: '9876543210',
    });
    expect(result.id).toBe(2);
  });

  it('getCurrentUser fetches profile from /api/auth/me', async () => {
    const mockProfile = { id: 1, name: 'Admin User', role: 'ADMIN' };
    api.get.mockResolvedValueOnce({ data: mockProfile });

    const result = await authService.getCurrentUser();
    expect(api.get).toHaveBeenCalledWith('/api/auth/me');
    expect(result.role).toBe('ADMIN');
  });

  it('logout calls /api/auth/logout and gracefully catches errors', async () => {
    api.post.mockRejectedValueOnce(new Error('Network error'));
    const result = await authService.logout();
    expect(api.post).toHaveBeenCalledWith('/api/auth/logout');
    expect(result.success).toBe(true);
  });
});
