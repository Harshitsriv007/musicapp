const KEY = 'dhun.userId';

function randomId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID().replace(/-/g, '');
  }
  return Math.random().toString(36).slice(2) + Date.now().toString(36);
}

/**
 * There is no login: each device gets a stable random id so likes and history are
 * kept per listener. Falls back to an in-memory id if storage is unavailable.
 */
let memoryId: string | null = null;

export function getUserId(): string {
  try {
    const existing = localStorage.getItem(KEY);
    if (existing) return existing;
    const id = randomId();
    localStorage.setItem(KEY, id);
    return id;
  } catch {
    memoryId ??= randomId();
    return memoryId;
  }
}
