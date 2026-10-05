import { createContext, useContext, useEffect, useState, ReactNode, useCallback } from 'react';
import { UserSummary } from '../types/auth';
import apiClient, { clearTokens, getStoredRefreshToken, setTokens } from '../lib/api/client';
import { API } from '../lib/api/endpoints';

interface AuthContextValue {
  user: UserSummary | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (accessToken: string, refreshToken: string, user: UserSummary) => void;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    try {
      const res = await apiClient.get(API.auth.me);
      setUser(res.data.data);
    } catch {
      setUser(null);
    }
  }, []);

  useEffect(() => {
    const rt = getStoredRefreshToken();
    if (!rt) { setIsLoading(false); return; }
    apiClient.post(API.auth.refresh, { refreshToken: rt })
      .then((res) => {
        const { accessToken, refreshToken: newRt, user: u } = res.data.data;
        setTokens(accessToken, newRt);
        setUser(u);
      })
      .catch(() => { clearTokens(); setUser(null); })
      .finally(() => setIsLoading(false));
  }, []);

  const login = useCallback((at: string, rt: string, userData: UserSummary) => {
    setTokens(at, rt);
    setUser(userData);
  }, []);

  const logout = useCallback(async () => {
    const rt = getStoredRefreshToken();
    try { await apiClient.post(API.auth.logout, { refreshToken: rt }); } catch { /* ignore */ }
    clearTokens();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, isLoading, isAuthenticated: !!user, login, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
