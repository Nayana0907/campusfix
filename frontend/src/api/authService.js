import api from './axios';

export const authService = {
  /**
   * Authenticate user with email and password
   * @param {Object} credentials - { email, password }
   * @returns {Promise<Object>} { accessToken, expiresIn, refreshToken, user }
   */
  async login(credentials) {
    const response = await api.post('/api/auth/login', {
      email: credentials.email.trim(),
      password: credentials.password,
    });
    return response.data;
  },

  /**
   * Register a new user with default STUDENT role
   * @param {Object} userData - { name, email, password, phone }
   * @returns {Promise<Object>} UserResponse
   */
  async register(userData) {
    const payload = {
      name: userData.name.trim(),
      email: userData.email.trim(),
      password: userData.password,
    };
    if (userData.phone && userData.phone.trim()) {
      payload.phone = userData.phone.trim();
    }
    const response = await api.post('/api/auth/register', payload);
    return response.data;
  },

  /**
   * Fetch profile of the currently authenticated user
   * @returns {Promise<Object>} UserResponse
   */
  async getCurrentUser() {
    const response = await api.get('/api/auth/me');
    return response.data;
  },

  /**
   * Refresh expired access token
   * @param {string} [refreshToken] - Optional if supplied via HTTP-only cookie
   * @returns {Promise<Object>} AuthResponse
   */
  async refreshToken(refreshToken) {
    const payload = refreshToken ? { refreshToken } : {};
    const response = await api.post('/api/auth/refresh', payload);
    return response.data;
  },

  /**
   * Logout user and invalidate refresh session
   * @returns {Promise<Object>} { success, message }
   */
  async logout() {
    try {
      const response = await api.post('/api/auth/logout');
      return response.data;
    } catch {
      // Even if network or backend fails, client cleanup will proceed
      return { success: true, message: 'Logged out' };
    }
  },

  /**
   * Health check
   */
  async checkHealth() {
    const response = await api.get('/api/health');
    return response.data;
  }
};
