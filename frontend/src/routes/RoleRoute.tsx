import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Role } from '../types/auth';

export function RoleRoute({ role }: { role: Role }) {
  const { user } = useAuth();
  if (user?.role !== role)
    return <Navigate to={user?.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'} replace />;
  return <Outlet />;
}
