"use client";

import { useRouter } from "next/navigation";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { api, clearToken, getToken, setToken } from "./api";
import type { User } from "./types";

interface AuthState {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  signup: (fullName: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  // On first load, a token in storage may still be valid — verify it against
  // /api/users/me rather than trusting its presence. The state updates are
  // deliberately kept out of the synchronous effect body, and guarded by
  // `cancelled` so an unmount mid-request doesn't update a dead component.
  useEffect(() => {
    let cancelled = false;

    async function restoreSession() {
      try {
        if (getToken()) {
          const currentUser = await api.me();
          if (!cancelled) setUser(currentUser);
        }
      } catch {
        clearToken();
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void restoreSession();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const { token } = await api.login(email, password);
    setToken(token);
    setUser(await api.me());
  }, []);

  const signup = useCallback(
    async (fullName: string, email: string, password: string) => {
      await api.signup(fullName, email, password);
      const { token } = await api.login(email, password);
      setToken(token);
      setUser(await api.me());
    },
    [],
  );

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
    router.push("/login");
  }, [router]);

  const value = useMemo(
    () => ({ user, loading, login, signup, logout }),
    [user, loading, login, signup, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}

/** Redirects to /login once we know there is no authenticated user. */
export function useRequireAuth() {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  return { user, loading };
}
