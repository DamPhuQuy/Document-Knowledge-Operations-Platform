import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { authApi } from '@/api/auth';
import { setCookie, getCookie, deleteCookie } from '@/utils/cookie';
import type {
  UserProfileDto,
  LoginRequest,
  RegisterRequest,
  VerifyOtpRequest,
  LoginResponse,
  RegisterResponse,
  VerifyOtpResponse,
} from '@/types';
import { ApiError } from '@/api/client';

const USER_STORAGE_KEY = 'docops_auth_user';
const TOKEN_COOKIE_KEY = 'access_token';

export interface AuthContextValue {
  token: string | null;
  user: UserProfileDto | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isManager: boolean;
  isStaff: boolean;
  isAuditor: boolean;
  loading: boolean;
  actionLoading: boolean;
  error: string | null;
  login: (credentials: LoginRequest) => Promise<LoginResponse>;
  register: (payload: RegisterRequest) => Promise<RegisterResponse>;
  verifyOtp: (payload: VerifyOtpRequest) => Promise<VerifyOtpResponse>;
  logout: () => void;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => {
    return getCookie(TOKEN_COOKIE_KEY) || localStorage.getItem('access_token');
  });

  const [user, setUser] = useState<UserProfileDto | null>(() => {
    try {
      const saved = localStorage.getItem(USER_STORAGE_KEY);
      return saved ? (JSON.parse(saved) as UserProfileDto) : null;
    } catch {
      return null;
    }
  });

  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isAuthenticated = !!token && !!user;
  const roles = useMemo(() => user?.roles || [], [user]);

  const isAdmin = roles.includes('ROLE_ADMIN');
  const isManager = roles.includes('ROLE_MANAGER') || isAdmin;
  const isStaff = roles.includes('ROLE_STAFF');
  const isAuditor = roles.includes('LEGAL_AUDITOR') || isAdmin;

  const login = useCallback(async (credentials: LoginRequest): Promise<LoginResponse> => {
    setActionLoading(true);
    setError(null);
    try {
      const res = await authApi.login(credentials);
      setToken(res.accessToken);
      setUser(res.user);

      setCookie(TOKEN_COOKIE_KEY, res.accessToken, { days: 7 });
      localStorage.setItem('access_token', res.accessToken);
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(res.user));

      return res;
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : 'Failed to sign in. Please verify your credentials.';
      setError(msg);
      throw err;
    } finally {
      setActionLoading(false);
    }
  }, []);

  const register = useCallback(async (payload: RegisterRequest): Promise<RegisterResponse> => {
    setActionLoading(true);
    setError(null);
    try {
      const res = await authApi.register(payload);
      return res;
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : 'Registration failed. Please check your details.';
      setError(msg);
      throw err;
    } finally {
      setActionLoading(false);
    }
  }, []);

  const verifyOtp = useCallback(async (payload: VerifyOtpRequest): Promise<VerifyOtpResponse> => {
    setActionLoading(true);
    setError(null);
    try {
      const res = await authApi.verifyOtp(payload);
      return res;
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : 'Invalid or expired OTP code.';
      setError(msg);
      throw err;
    } finally {
      setActionLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    deleteCookie(TOKEN_COOKIE_KEY);
    localStorage.removeItem('access_token');
    localStorage.removeItem(USER_STORAGE_KEY);
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      isAuthenticated,
      isAdmin,
      isManager,
      isStaff,
      isAuditor,
      loading,
      actionLoading,
      error,
      login,
      register,
      verifyOtp,
      logout,
      clearError,
    }),
    [token, user, isAuthenticated, isAdmin, isManager, isStaff, isAuditor, loading, actionLoading, error, login, register, verifyOtp, logout, clearError]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
