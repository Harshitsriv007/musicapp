import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { api } from '../api/client';
import type { DownloadStatus, Song } from '../api/types';
import { subscribe } from '../lib/liveEvents';

interface DownloadsApi {
  statusOf: (song: Song) => DownloadStatus;
  /** The song with its latest known download status (so playback uses our DB when possible). */
  withStatus: (song: Song) => Song;
  download: (songs: Song[]) => Promise<void>;
  remove: (song: Song) => Promise<void>;
  /** Bumps when a download finishes or is removed, so the Downloads tab reloads. */
  version: number;
}

const DownloadsContext = createContext<DownloadsApi | null>(null);
const BATCH = 200;

export function DownloadsProvider({ children }: { children: ReactNode }) {
  const [overrides, setOverrides] = useState<Map<number, DownloadStatus>>(new Map());
  const [version, setVersion] = useState(0);
  /** Songs in this listener's Downloads. Live events about other listeners' downloads are ignored. */
  const mine = useRef<Set<number>>(new Set());

  const setStatuses = (entries: [number, DownloadStatus][]) =>
    setOverrides((prev) => {
      const next = new Map(prev);
      entries.forEach(([id, status]) => next.set(id, status));
      return next;
    });

  // Live progress pushed by the server (PENDING → DOWNLOADING → DOWNLOADED / FAILED).
  useEffect(
    () =>
      subscribe<Song>('song-updated', (song) => {
        if (!mine.current.has(song.id)) return;
        setStatuses([[song.id, song.downloadStatus]]);
        if (song.downloadStatus === 'DOWNLOADED' || song.downloadStatus === 'NONE') setVersion((v) => v + 1);
      }),
    [],
  );

  const statusOf = useCallback(
    (song: Song): DownloadStatus => {
      // The server reports a status other than NONE only for songs in this listener's Downloads.
      if (!overrides.has(song.id) && song.downloadStatus && song.downloadStatus !== 'NONE') mine.current.add(song.id);
      return overrides.get(song.id) ?? song.downloadStatus ?? 'NONE';
    },
    [overrides],
  );

  const withStatus = useCallback((song: Song): Song => ({ ...song, downloadStatus: statusOf(song) }), [statusOf]);

  const download = useCallback(
    async (songs: Song[]) => {
      const todo = songs.filter((s) => {
        const st = statusOf(s);
        return st === 'NONE' || st === 'FAILED';
      });
      if (todo.length === 0) return;
      const previous = todo.map((s): [number, DownloadStatus] => [s.id, statusOf(s)]);
      todo.forEach((s) => mine.current.add(s.id));
      setStatuses(todo.map((s) => [s.id, 'PENDING']));
      try {
        for (let i = 0; i < todo.length; i += BATCH) {
          const result = await api.download(todo.slice(i, i + BATCH).map((s) => s.id));
          setStatuses(result.map((r) => [r.songId, r.downloadStatus]));
        }
        setVersion((v) => v + 1);
      } catch {
        previous.forEach(([id, status]) => {
          if (status === 'NONE') mine.current.delete(id);
        });
        setStatuses(previous);
      }
    },
    [statusOf],
  );

  const remove = useCallback(
    async (song: Song) => {
      const before = statusOf(song);
      mine.current.delete(song.id);
      setStatuses([[song.id, 'NONE']]);
      try {
        await api.removeDownload(song.id);
        setVersion((v) => v + 1);
      } catch {
        mine.current.add(song.id);
        setStatuses([[song.id, before]]);
      }
    },
    [statusOf],
  );

  const value = useMemo(
    () => ({ statusOf, withStatus, download, remove, version }),
    [statusOf, withStatus, download, remove, version],
  );
  return <DownloadsContext.Provider value={value}>{children}</DownloadsContext.Provider>;
}

export function useDownloads(): DownloadsApi {
  const ctx = useContext(DownloadsContext);
  if (!ctx) throw new Error('useDownloads must be used inside <DownloadsProvider>');
  return ctx;
}
