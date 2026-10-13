import React from 'react';
import { Navigate, useLocation, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { FullScreenLoader } from '../components/common/Loader';

/**
 * Route guard component protecting private routes.
 * 
 * @param {Array<string>} [allowedRoles] - Optional list of authorized roles. If omitted, any authenticated user can access.
 * @param {React.ReactNode} [children] - Optional child elements (defaults to <Outlet />).
 */
export const ProtectedRoute = ({ allowedRoles, children }) => {
  const { isAuthenticated, isLoading, role } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <FullScreenLoader message="Verifying session and authorization..." />;
  }

  if (!isAuthenticated) {
    // Preserve intended destination so user can be redirected after successful login
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(role)) {
    // User is authenticated but lacks required role for this view
    return <Navigate to="/unauthorized" replace />;
  }

  return children ? children : <Outlet />;
};

/**
 * Route guard for public-only pages like Login and Register.
 * Redirects already-authenticated users to their dashboard.
 */
export const PublicOnlyRoute = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <FullScreenLoader message="Loading..." />;
  }

  if (isAuthenticated) {
    const intended = location.state?.from?.pathname || '/dashboard';
    return <Navigate to={intended} replace />;
  }

  return children ? children : <Outlet />;
};
