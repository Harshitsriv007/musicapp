/**
 * The logged-in account on this device. Likes, history and downloads follow the account to every
 * device. A login lasts a fixed time (4 hours by default, set by the server); after `expiresAt`
 * the listener has to log in again.
 */
export interface StoredSession {
  token: string;
  username: string;
  /** ISO timestamp when the server stops accepting the token. */
  expiresAt: string;
}

const KEY = 'dhun.session';
export const SESSION_EVENT = 'dhun:session';

let memory: StoredSession | null = null;
/** True after the session ran out (rather than the listener logging out), so the login screen can say why. */
let expired = false;

function parse(raw: string | null): StoredSession | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as Partial<StoredSession>;
    return typeof parsed.token === 'string' &&
      typeof parsed.username === 'string' &&
      typeof parsed.expiresAt === 'string' &&
      !Number.isNaN(Date.parse(parsed.expiresAt))
      ? { token: parsed.token, username: parsed.username, expiresAt: parsed.expiresAt }
      : null;
  } catch {
    return null;
  }
}

/** The current session, or null when there is none or it has expired. */
export function getSession(): StoredSession | null {
  let session: StoredSession | null;
  try {
    session = parse(localStorage.getItem(KEY));
  } catch {
    session = memory;
  }
  if (session && Date.parse(session.expiresAt) <= Date.now()) {
    expired = true;
    return null;
  }
  return session;
}

/** Stores a new session, or clears it (a manual logout, unless `ranOut` says the session expired). */
export function setSession(session: StoredSession | null, ranOut = false): void {
  memory = session;
  expired = !session && ranOut;
  try {
    if (session) localStorage.setItem(KEY, JSON.stringify(session));
    else localStorage.removeItem(KEY);
  } catch {
    /* in-memory only */
  }
  window.dispatchEvent(new Event(SESSION_EVENT));
}

/** Ends the session because it ran out or the server rejected it; the login screen explains why. */
export function expireSession(): void {
  setSession(null, true);
}

export function sessionExpired(): boolean {
  return expired;
}

/** Milliseconds until the session expires (never negative). */
export function msUntilExpiry(session: StoredSession): number {
  return Math.max(0, Date.parse(session.expiresAt) - Date.now());
}
