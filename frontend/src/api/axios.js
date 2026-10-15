import axios from 'axios';

/**
 * Normalizes and cleans the API base URL from Vite environment variables.
 * - Strips any trailing slashes
 * - Strips redundant trailing '/api' to prevent duplicate '/api/api' paths
 * - Defaults to http://localhost:8080 in development mode
 * - Defaults to empty string (relative paths) in production mode if unset
 */
export function getCleanBaseUrl() {
  const raw = import.meta.env.VITE_API_BASE_URL;
  if (typeof raw === 'string' && raw.trim() !== '') {
    let cleaned = raw.trim().replace(/\/+$/, '');
    if (cleaned.endsWith('/api')) {
      cleaned = cleaned.slice(0, -4);
    }
    return cleaned;
  }
  if (import.meta.env.DEV) {
    return 'http://localhost:8080';
  }
  return '';
}

const baseURL = getCleanBaseUrl();

const api = axios.create({
  baseURL: baseURL || undefined,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
  withCredentials: true, // Send HTTP-only refresh cookies
});

// State for concurrent refresh token queueing
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Request Interceptor: Attach JWT Bearer token if present & strip manual Content-Type for FormData
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('campusfix_token');
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // When transmitting FormData, let the browser or axios compute the boundary automatically
    if (config.data instanceof FormData && config.headers && config.headers['Content-Type']) {
      delete config.headers['Content-Type'];
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Normalize errors & handle 401 with token refresh queue
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (!error.response) {
      // Network error or backend unreachable
      const base = getCleanBaseUrl();
      const networkError = new Error(
        `Unable to connect to CampusFix server${base ? ` at ${base}` : ''}. Please check your network or server status.`
      );
      networkError.status = 0;
      networkError.errorCode = 'NETWORK_ERROR';
      return Promise.reject(networkError);
    }

    const data = error.response.data;
    const status = error.response.status;

    // Extract client-safe error message
    let message = 'An unexpected error occurred. Please try again.';
    if (typeof data === 'string') {
      message = data;
    } else if (data && data.message) {
      message = data.message;
    } else if (data && data.errors && typeof data.errors === 'object') {
      message = Object.values(data.errors)[0] || message;
    }

    const requestUrl = originalRequest?.url || '';
    const isAuthEndpoint =
      requestUrl.includes('/api/auth/login') ||
      requestUrl.includes('/api/auth/register') ||
      requestUrl.includes('/api/auth/refresh');

    // Handle 401 Unauthorized with token refresh (for non-auth endpoints only)
    if (status === 401 && !isAuthEndpoint && originalRequest && !originalRequest._retry) {
      if (isRefreshing) {
        // Another refresh operation is already in flight: queue this request
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((newToken) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const storedRefreshToken = localStorage.getItem('campusfix_refresh_token');
        const refreshPayload = storedRefreshToken ? { refreshToken: storedRefreshToken } : {};

        // Use direct axios call (independent of instance interceptors) to prevent infinite loops
        const refreshBase = getCleanBaseUrl();
        const refreshUrl = refreshBase ? `${refreshBase}/api/auth/refresh` : '/api/auth/refresh';
        const refreshResponse = await axios.post(
          refreshUrl,
          refreshPayload,
          {
            withCredentials: true,
            headers: {
              'Content-Type': 'application/json',
              Accept: 'application/json',
            },
          }
        );

        const newAccessToken = refreshResponse.data?.accessToken;
        if (!newAccessToken) {
          throw new Error('No access token returned from refresh');
        }

        localStorage.setItem('campusfix_token', newAccessToken);
        if (refreshResponse.data?.refreshToken) {
          localStorage.setItem('campusfix_refresh_token', refreshResponse.data.refreshToken);
        }
        if (refreshResponse.data?.user) {
          localStorage.setItem('campusfix_user', JSON.stringify(refreshResponse.data.user));
          window.dispatchEvent(new CustomEvent('campusfix:user-updated', { detail: refreshResponse.data.user }));
        }

        api.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

        processQueue(null, newAccessToken);
        return api(originalRequest);
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        localStorage.removeItem('campusfix_token');
        localStorage.removeItem('campusfix_refresh_token');
        localStorage.removeItem('campusfix_user');
        window.dispatchEvent(new CustomEvent('campusfix:auth-expired'));

        const authError = new Error(message || 'Session expired. Please log in again.');
        authError.status = 401;
        authError.errorCode = 'AUTH_EXPIRED';
        return Promise.reject(authError);
      } finally {
        isRefreshing = false;
      }
    }

    // Standard enhanced error object for other statuses
    const enhancedError = new Error(message);
    enhancedError.status = status;
    enhancedError.errorCode = data?.errorCode || 'API_ERROR';
    enhancedError.data = data;
    return Promise.reject(enhancedError);
  }
);

export default api;
