import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '../api/client';
import { onLiveChange, subscribe } from '../lib/liveEvents';
import type { Song } from '../api/types';

export interface NewSongsState {
  songs: Song[];
  loading: boolean;
  error: string | null;
  /** Songs that arrived live since the page was opened. */
  freshIds: Set<number>;
  /** True while the live (SSE) connection is open. */
  live: boolean;
  refresh: () => Promise<void>;
}

/**
 * New songs list that updates itself:
 *  1. REST: GET /api/songs/new on load and every `pollMs` (fallback, works everywhere)
 *  2. Live: Server-Sent Events from /api/songs/stream push each song the moment Kafka delivers it
 */
export function useNewSongs(pollMs = 60_000, limit = 50): NewSongsState {
  const [songs, setSongs] = useState<Song[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [freshIds, setFreshIds] = useState<Set<number>>(new Set());
  const [live, setLive] = useState(false);
  const mounted = useRef(true);

  const refresh = useCallback(async () => {
    try {
      const list = await api.newSongs(limit);
      if (!mounted.current) return;
      setSongs(list);
      setError(null);
    } catch {
      if (mounted.current) setError('Could not load new songs');
    } finally {
      if (mounted.current) setLoading(false);
    }
  }, [limit]);

  useEffect(() => {
    mounted.current = true;
    void refresh();
    const timer = setInterval(() => void refresh(), pollMs);
    const onVisible = () => document.visibilityState === 'visible' && void refresh();
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      mounted.current = false;
      clearInterval(timer);
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, [refresh, pollMs]);

  useEffect(() => onLiveChange(setLive), []);

  useEffect(
    () =>
      subscribe<Song>('song-added', (song) => {
        setSongs((prev) => (prev.some((s) => s.id === song.id) ? prev : [song, ...prev].slice(0, limit)));
        setFreshIds((prev) => new Set(prev).add(song.id));
      }),
    [limit],
  );

  return { songs, loading, error, freshIds, live, refresh };
}
