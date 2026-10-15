import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../api/axios';

describe('Axios Client & Interceptors', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('attaches Bearer token to request headers if token is in localStorage', async () => {
    localStorage.setItem('campusfix_token', 'test-bearer-token-abc');

    // Retrieve request interceptor handler
    const requestInterceptor = api.interceptors.request.handlers[0];
    const config = { headers: {} };
    const modifiedConfig = requestInterceptor.fulfilled(config);

    expect(modifiedConfig.headers.Authorization).toBe('Bearer test-bearer-token-abc');
  });

  it('does not attach Authorization header if no token is present', async () => {
    const requestInterceptor = api.interceptors.request.handlers[0];
    const config = { headers: {} };
    const modifiedConfig = requestInterceptor.fulfilled(config);

    expect(modifiedConfig.headers.Authorization).toBeUndefined();
  });

  it('normalizes backend error message from response.data.message', async () => {
    const responseInterceptor = api.interceptors.response.handlers[0];
    const mockError = {
      response: {
        status: 400,
        data: {
          success: false,
          message: 'Invalid email or password format',
          errorCode: 'INVALID_CREDENTIALS',
        },
      },
      config: { url: '/api/auth/login' },
    };

    await expect(responseInterceptor.rejected(mockError)).rejects.toThrow(
      'Invalid email or password format'
    );
  });

  it('clears localStorage and dispatches auth-expired on 401 for protected endpoints', async () => {
    localStorage.setItem('campusfix_token', 'expired-token');
    localStorage.setItem('campusfix_user', JSON.stringify({ name: 'User' }));

    const dispatchSpy = vi.spyOn(window, 'dispatchEvent');

    const responseInterceptor = api.interceptors.response.handlers[0];
    const mockError = {
      response: {
        status: 401,
        data: { message: 'JWT token has expired' },
      },
      config: { url: '/api/complaints' },
    };

    await expect(responseInterceptor.rejected(mockError)).rejects.toThrow('JWT token has expired');
    expect(localStorage.getItem('campusfix_token')).toBeNull();
    expect(localStorage.getItem('campusfix_user')).toBeNull();
    expect(dispatchSpy).toHaveBeenCalled();
  });

  it('handles network failure when backend is unreachable', async () => {
    const responseInterceptor = api.interceptors.response.handlers[0];
    const networkError = {
      request: {},
      config: { url: '/api/auth/login' },
    };

    await expect(responseInterceptor.rejected(networkError)).rejects.toThrow(
      'Unable to connect to CampusFix server'
    );
  });

  it('removes Content-Type header when request data is FormData so browser sets multipart boundary', () => {
    const requestInterceptor = api.interceptors.request.handlers[0];
    const formData = new FormData();
    formData.append('key', 'value');

    const config = {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      data: formData,
    };

    const modifiedConfig = requestInterceptor.fulfilled(config);
    expect(modifiedConfig.headers['Content-Type']).toBeUndefined();
  });
});
