/** The logged-in account on this device. Likes and history follow the account to every device. */
export interface StoredSession {
  token: string;
  username: string;
}

const KEY = 'dhun.session';
export const SESSION_EVENT = 'dhun:session';

let memory: StoredSession | null = null;

export function getSession(): StoredSession | null {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<StoredSession>;
    return typeof parsed.token === 'string' && typeof parsed.username === 'string'
      ? { token: parsed.token, username: parsed.username }
      : null;
  } catch {
    return memory;
  }
}

export function setSession(session: StoredSession | null): void {
  memory = session;
  try {
    if (session) localStorage.setItem(KEY, JSON.stringify(session));
    else localStorage.removeItem(KEY);
  } catch {
    /* in-memory only */
  }
  window.dispatchEvent(new Event(SESSION_EVENT));
}
