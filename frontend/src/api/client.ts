import { getSession, setSession } from '../lib/session';
import { getUserId } from '../lib/userId';
import type {
  AuthResponse,
  DownloadStatusEntry,
  Folder,
  FolderType,
  HistoryEntry,
  LikedEntry,
  Page,
  SearchResult,
  Song,
  SongQuery,
} from './types';

export const API_BASE: string = import.meta.env.VITE_API_BASE ?? '';

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const session = getSession();
  const response = await fetch(API_BASE + path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      // Sent even when logged in: sign-up / login move this device's likes and history into the account.
      'X-User-Id': getUserId(),
      ...(session ? { Authorization: `Bearer ${session.token}` } : {}),
      ...(init.headers ?? {}),
    },
  });
  // The session expired or was logged out elsewhere: fall back to this device's own (anonymous) data.
  if (response.status === 401 && session && getSession()?.token === session.token) {
    setSession(null);
  }
  if (!response.ok) {
    const detail = await errorDetail(response);
    if (detail) throw new ApiError(response.status, detail);
    throw new ApiError(response.status, `${init.method ?? 'GET'} ${path} failed with ${response.status}`);
  }
  // 202/204 may or may not carry a body (e.g. POST /api/downloads returns the queued statuses).
  const text = response.status === 204 ? '' : await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

/** The human-readable "detail" of a Spring ProblemDetail error body, if any. */
async function errorDetail(response: Response): Promise<string | null> {
  try {
    const body = (await response.json()) as { detail?: unknown };
    return typeof body.detail === 'string' ? body.detail : null;
  } catch {
    return null;
  }
}

export const api = {
  signUp: (username: string, password: string) =>
    request<AuthResponse>('/api/auth/signup', { method: 'POST', body: JSON.stringify({ username, password }) }),
  logIn: (username: string, password: string) =>
    request<AuthResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
  logOut: () => request<void>('/api/auth/logout', { method: 'POST' }),
  newSongs: (limit = 50) => request<Song[]>(`/api/songs/new?limit=${limit}`),
  songs: (q: string, page = 0, size = 30, filter: SongQuery = {}) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (q) params.set('q', q);
    if (filter.downloaded) params.set('downloaded', 'true');
    if (filter.decade) params.set('decade', filter.decade);
    if (filter.artist) params.set('artist', filter.artist);
    if (filter.movie) params.set('movie', filter.movie);
    return request<Page<Song>>(`/api/songs?${params.toString().replace(/\+/g, '%20')}`);
  },
  /** Songs by name, artist or movie, plus matching artist and movie folders. */
  search: (q: string, init: RequestInit = {}) =>
    request<SearchResult>(`/api/search?q=${encodeURIComponent(q)}`, init),
  browse: (type: FolderType) => request<Folder[]>(`/api/browse/${type}`),
  likeBatch: (songIds: number[], liked: boolean) =>
    request<void>('/api/likes/batch', { method: 'POST', body: JSON.stringify({ songIds, liked }) }),
  liked: (page = 0, size = 200) => request<Page<LikedEntry>>(`/api/likes?page=${page}&size=${size}`),
  like: (songId: number) => request<void>(`/api/likes/${songId}`, { method: 'PUT' }),
  unlike: (songId: number) => request<void>(`/api/likes/${songId}`, { method: 'DELETE' }),
  history: (page = 0, size = 100) => request<Page<HistoryEntry>>(`/api/history?page=${page}&size=${size}`),
  recordPlay: (songId: number) =>
    request<HistoryEntry>('/api/history', { method: 'POST', body: JSON.stringify({ songId }) }),
  clearHistory: () => request<void>('/api/history', { method: 'DELETE' }),
  triggerIngestion: () => request<void>('/api/admin/ingestion/run', { method: 'POST' }),
  download: (songIds: number[]) =>
    request<DownloadStatusEntry[]>('/api/downloads', { method: 'POST', body: JSON.stringify({ songIds }) }),
  removeDownload: (songId: number) => request<void>(`/api/songs/${songId}/download`, { method: 'DELETE' }),
  songStreamUrl: () => `${API_BASE}/api/songs/stream`,
};

/** Downloaded songs play from our own database; others stream from the external music API. */
export function audioUrl(song: Song): string {
  if (song.downloadStatus === 'DOWNLOADED') return `${API_BASE}/api/songs/${song.id}/audio`;
  return song.streamUrl.startsWith('/') ? API_BASE + song.streamUrl : song.streamUrl;
}
