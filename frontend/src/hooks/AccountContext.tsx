import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api } from '../api/client';
import { expireSession, getSession, msUntilExpiry, SESSION_EVENT, sessionExpired, setSession } from '../lib/session';

interface AccountApi {
  /** Logged-in username, or null when nobody is logged in on this device. */
  username: string | null;
  /** True when the last session ran out (or was ended elsewhere), so the login screen can say so. */
  sessionExpired: boolean;
  logIn: (username: string, password: string) => Promise<void>;
  signUp: (username: string, password: string) => Promise<void>;
  /** "Forgot password": sets a new password and logs in. */
  resetPassword: (username: string, newPassword: string) => Promise<void>;
  logOut: () => Promise<void>;
}

const AccountContext = createContext<AccountApi | null>(null);

interface State {
  username: string | null;
  expiresAt: string | null;
  sessionExpired: boolean;
}

function readState(): State {
  const session = getSession();
  return { username: session?.username ?? null, expiresAt: session?.expiresAt ?? null, sessionExpired: sessionExpired() };
}

export function AccountProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<State>(readState);

  // Also changes when a request finds the session expired (see api/client).
  useEffect(() => {
    const sync = () =>
      setState((prev) => {
        const next = readState();
        const same =
          prev.username === next.username &&
          prev.expiresAt === next.expiresAt &&
          prev.sessionExpired === next.sessionExpired;
        return same ? prev : next;
      });
    window.addEventListener(SESSION_EVENT, sync);
    window.addEventListener('storage', sync); // logged in / out in another tab
    return () => {
      window.removeEventListener(SESSION_EVENT, sync);
      window.removeEventListener('storage', sync);
    };
  }, []);

  // Log out the moment the session runs out, even if no request is made (e.g. music just playing).
  useEffect(() => {
    if (!state.expiresAt) return;
    const check = () => {
      const session = getSession();
      if (!session || msUntilExpiry(session) === 0) expireSession();
    };
    const session = getSession();
    if (!session) {
      expireSession();
      return;
    }
    const timer = window.setTimeout(check, msUntilExpiry(session) + 50);
    // Timers are paused or throttled while the device sleeps or the tab is hidden: re-check on return.
    const onVisible = () => {
      if (document.visibilityState === 'visible') check();
    };
    document.addEventListener('visibilitychange', onVisible);
    window.addEventListener('focus', check);
    return () => {
      window.clearTimeout(timer);
      document.removeEventListener('visibilitychange', onVisible);
      window.removeEventListener('focus', check);
    };
  }, [state.expiresAt]);

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

  const { username, sessionExpired: expired } = state;
  const value = useMemo(
    () => ({ username, sessionExpired: expired, logIn, signUp, resetPassword, logOut }),
    [username, expired, logIn, signUp, resetPassword, logOut],
  );
  return <AccountContext.Provider value={value}>{children}</AccountContext.Provider>;
}

export function useAccount(): AccountApi {
  const ctx = useContext(AccountContext);
  if (!ctx) throw new Error('useAccount must be used inside <AccountProvider>');
  return ctx;
}
