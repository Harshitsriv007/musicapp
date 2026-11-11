import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api } from '../api/client';
import { getSession, SESSION_EVENT, setSession } from '../lib/session';

interface AccountApi {
  /** Logged-in username, or null when this device is anonymous. */
  username: string | null;
  logIn: (username: string, password: string) => Promise<void>;
  signUp: (username: string, password: string) => Promise<void>;
  /** "Forgot password": sets a new password and logs in. */
  resetPassword: (username: string, newPassword: string) => Promise<void>;
  logOut: () => Promise<void>;
}

const AccountContext = createContext<AccountApi | null>(null);

export function AccountProvider({ children }: { children: ReactNode }) {
  const [username, setUsername] = useState<string | null>(() => getSession()?.username ?? null);

  // Also changes when a request finds the session expired (see api/client).
  useEffect(() => {
    const sync = () => setUsername(getSession()?.username ?? null);
    window.addEventListener(SESSION_EVENT, sync);
    window.addEventListener('storage', sync); // logged in / out in another tab
    return () => {
      window.removeEventListener(SESSION_EVENT, sync);
      window.removeEventListener('storage', sync);
    };
  }, []);

  const logIn = useCallback(async (name: string, password: string) => {
    setSession(await api.logIn(name, password));
  }, []);

  const signUp = useCallback(async (name: string, password: string) => {
    setSession(await api.signUp(name, password));
  }, []);

  const resetPassword = useCallback(async (name: string, newPassword: string) => {
    setSession(await api.resetPassword(name, newPassword));
  }, []);

  const logOut = useCallback(async () => {
    try {
      await api.logOut();
    } finally {
      setSession(null);
    }
  }, []);

  const value = useMemo(
    () => ({ username, logIn, signUp, resetPassword, logOut }),
    [username, logIn, signUp, resetPassword, logOut],
  );
  return <AccountContext.Provider value={value}>{children}</AccountContext.Provider>;
}

export function useAccount(): AccountApi {
  const ctx = useContext(AccountContext);
  if (!ctx) throw new Error('useAccount must be used inside <AccountProvider>');
  return ctx;
}
