import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { api } from '../api/client';
import type { Song } from '../api/types';

interface LikesApi {
  isLiked: (song: Song) => boolean;
  toggleLike: (song: Song) => Promise<void>;
  /** Bumps whenever a like changes, so the Liked tab knows to reload. */
  version: number;
}

const LikesContext = createContext<LikesApi | null>(null);

/**
 * The server already returns `liked` on every song; this context only tracks the changes
 * made in this session so every list (New, Library, History, mini player) stays in sync.
 */
export function LikesProvider({ children }: { children: ReactNode }) {
  const [overrides, setOverrides] = useState<Map<number, boolean>>(new Map());
  const [version, setVersion] = useState(0);

  const isLiked = useCallback(
    (song: Song) => overrides.get(song.id) ?? song.liked,
    [overrides],
  );

  const setLiked = (id: number, liked: boolean) =>
    setOverrides((prev) => new Map(prev).set(id, liked));

  const toggleLike = useCallback(
    async (song: Song) => {
      const wasLiked = overrides.get(song.id) ?? song.liked;
      setLiked(song.id, !wasLiked); // optimistic
      try {
        await (wasLiked ? api.unlike(song.id) : api.like(song.id));
        setVersion((v) => v + 1);
      } catch {
        setLiked(song.id, wasLiked); // roll back
      }
    },
    [overrides],
  );

  const value = useMemo(() => ({ isLiked, toggleLike, version }), [isLiked, toggleLike, version]);
  return <LikesContext.Provider value={value}>{children}</LikesContext.Provider>;
}

export function useLikes(): LikesApi {
  const ctx = useContext(LikesContext);
  if (!ctx) throw new Error('useLikes must be used inside <LikesProvider>');
  return ctx;
}
