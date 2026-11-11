import { getUserId } from '../lib/userId';
import type { HistoryEntry, LikedEntry, Page, Song } from './types';

export const API_BASE: string = import.meta.env.VITE_API_BASE ?? '';

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(API_BASE + path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': getUserId(),
      ...(init.headers ?? {}),
    },
  });
  if (!response.ok) {
    throw new ApiError(response.status, `${init.method ?? 'GET'} ${path} failed with ${response.status}`);
  }
  if (response.status === 204 || response.status === 202) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  newSongs: (limit = 50) => request<Song[]>(`/api/songs/new?limit=${limit}`),
  songs: (q: string, page = 0, size = 30) =>
    request<Page<Song>>(`/api/songs?page=${page}&size=${size}${q ? `&q=${encodeURIComponent(q)}` : ''}`),
  liked: (page = 0, size = 200) => request<Page<LikedEntry>>(`/api/likes?page=${page}&size=${size}`),
  like: (songId: number) => request<void>(`/api/likes/${songId}`, { method: 'PUT' }),
  unlike: (songId: number) => request<void>(`/api/likes/${songId}`, { method: 'DELETE' }),
  history: (page = 0, size = 100) => request<Page<HistoryEntry>>(`/api/history?page=${page}&size=${size}`),
  recordPlay: (songId: number) =>
    request<HistoryEntry>('/api/history', { method: 'POST', body: JSON.stringify({ songId }) }),
  clearHistory: () => request<void>('/api/history', { method: 'DELETE' }),
  triggerIngestion: () => request<void>('/api/admin/ingestion/run', { method: 'POST' }),
  songStreamUrl: () => `${API_BASE}/api/songs/stream`,
};
