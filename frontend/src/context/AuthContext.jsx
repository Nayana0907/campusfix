import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authService } from '../api/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('campusfix_token'));
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('campusfix_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } catch {
      // Ignore network errors during logout
    } finally {
      localStorage.removeItem('campusfix_token');
      localStorage.removeItem('campusfix_refresh_token');
      localStorage.removeItem('campusfix_user');
      setToken(null);
      setUser(null);
      setError(null);
    }
  }, []);

  // Verify session on mount if token exists
  useEffect(() => {
    let isMounted = true;

    const verifySession = async () => {
      const storedToken = localStorage.getItem('campusfix_token');
      if (!storedToken) {
        if (isMounted) setIsLoading(false);
        return;
      }

      try {
        const profile = await authService.getCurrentUser();
        if (isMounted) {
          setUser(profile);
          localStorage.setItem('campusfix_user', JSON.stringify(profile));
        }
      } catch {
        if (isMounted) {
          // Token is invalid/expired
          localStorage.removeItem('campusfix_token');
          localStorage.removeItem('campusfix_refresh_token');
          localStorage.removeItem('campusfix_user');
          setToken(null);
          setUser(null);
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    verifySession();

    const handleAuthExpired = () => {
      logout();
    };

    const handleUserUpdated = (e) => {
      if (e.detail && isMounted) {
        setUser(e.detail);
      }
    };

    window.addEventListener('campusfix:auth-expired', handleAuthExpired);
    window.addEventListener('campusfix:user-updated', handleUserUpdated);
    return () => {
      isMounted = false;
      window.removeEventListener('campusfix:auth-expired', handleAuthExpired);
      window.removeEventListener('campusfix:user-updated', handleUserUpdated);
    };
  }, [logout]);

  const login = async (credentials) => {
    setIsLoading(true);
    setError(null);
    try {
      const authData = await authService.login(credentials);
      const accessToken = authData.accessToken;
      const currentUser = authData.user;

      localStorage.setItem('campusfix_token', accessToken);
      if (authData.refreshToken) {
        localStorage.setItem('campusfix_refresh_token', authData.refreshToken);
      }
      localStorage.setItem('campusfix_user', JSON.stringify(currentUser));

      setToken(accessToken);
      setUser(currentUser);
      return currentUser;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (userData) => {
    setIsLoading(true);
    setError(null);
    try {
      const createdUser = await authService.register(userData);
      return createdUser;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const refreshProfile = async () => {
    try {
      const updatedUser = await authService.getCurrentUser();
      setUser(updatedUser);
      localStorage.setItem('campusfix_user', JSON.stringify(updatedUser));
      return updatedUser;
    } catch {
      // If fetching fails, leave existing state
      return user;
    }
  };

  const value = {
    token,
    user,
    role: user?.role || null,
    isAuthenticated: Boolean(token && user),
    isLoading,
    error,
    login,
    register,
    logout,
    refreshProfile,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
