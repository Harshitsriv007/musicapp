import { getUserId } from '../lib/userId';
import type { DownloadStatusEntry, HistoryEntry, LikedEntry, Page, Song } from './types';

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
  // 202/204 may or may not carry a body (e.g. POST /api/downloads returns the queued statuses).
  const text = response.status === 204 ? '' : await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  newSongs: (limit = 50) => request<Song[]>(`/api/songs/new?limit=${limit}`),
  songs: (q: string, page = 0, size = 30, downloaded = false) =>
    request<Page<Song>>(
      `/api/songs?page=${page}&size=${size}${q ? `&q=${encodeURIComponent(q)}` : ''}${downloaded ? '&downloaded=true' : ''}`,
    ),
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
