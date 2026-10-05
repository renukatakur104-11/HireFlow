import { Link, useNavigate } from 'react-router-dom';
import { Briefcase, Bell, LogOut, User, LayoutDashboard, Search, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../../lib/api/client';
import { API } from '../../lib/api/endpoints';
import { ApiResponse } from '../../types/api';
import toast from 'react-hot-toast';

export function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const { data: unreadData } = useQuery<ApiResponse<number>>({
    queryKey: ['notif-count'],
    queryFn: () => apiClient.get(API.notifications.unreadCount).then(r => r.data),
    refetchInterval: 30000,
    enabled: !!user,
  });

  const unreadCount = unreadData?.data ?? 0;

  const handleLogout = async () => {
    await logout();
    toast.success('Signed out successfully');
    navigate('/login');
  };

  const isAdmin = user?.role === 'ADMIN';

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200/80 bg-white/95 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link to={isAdmin ? '/admin/dashboard' : '/dashboard'} className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-md shadow-indigo-500/20">
            <Briefcase className="h-5 w-5" />
          </div>
          <span className="text-lg font-bold tracking-tight text-slate-900">HireFlow</span>
          {isAdmin && (
            <span className="ml-1 inline-flex items-center gap-1 rounded-full bg-violet-100 px-2 py-0.5 text-xs font-semibold text-violet-700">
              <ShieldCheck className="w-3 h-3" />Admin
            </span>
          )}
        </Link>

        <nav className="hidden md:flex items-center gap-1">
          {isAdmin ? (
            <>
              <NavLink to="/admin/dashboard"><LayoutDashboard className="w-4 h-4" />Dashboard</NavLink>
              <NavLink to="/admin/drives"><Briefcase className="w-4 h-4" />Drives</NavLink>
              <NavLink to="/admin/students"><User className="w-4 h-4" />Students</NavLink>
              <NavLink to="/admin/notifications"><Bell className="w-4 h-4" />Notifications</NavLink>
            </>
          ) : (
            <>
              <NavLink to="/dashboard"><LayoutDashboard className="w-4 h-4" />Dashboard</NavLink>
              <NavLink to="/drives"><Search className="w-4 h-4" />Drives</NavLink>
              <NavLink to="/applications"><Briefcase className="w-4 h-4" />Applications</NavLink>
              <NavLink to="/profile"><User className="w-4 h-4" />Profile</NavLink>
            </>
          )}
        </nav>

        <div className="flex items-center gap-2">
          {!isAdmin && (
            <Link to="/notifications" className="relative p-2 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors">
              <Bell className="w-5 h-5" />
              {unreadCount > 0 && (
                <span className="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-rose-500 text-[10px] font-bold text-white">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </Link>
          )}
          <button
            onClick={handleLogout}
            className="flex items-center gap-1.5 px-3 py-1.5 text-sm text-slate-600 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
          >
            <LogOut className="w-4 h-4" />
            <span className="hidden sm:block">Sign out</span>
          </button>
        </div>
      </div>
    </header>
  );
}

function NavLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <Link
      to={to}
      className="flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-slate-600 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
    >
      {children}
    </Link>
  );
}
