import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { api } from '../api/client';
import type { Song } from '../api/types';

interface LikesApi {
  isLiked: (song: Song) => boolean;
  toggleLike: (song: Song) => Promise<void>;
  /** Like or unlike many songs at once (a whole folder). */
  setLikedMany: (songs: Song[], liked: boolean) => Promise<void>;
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

  const setLikedMany = useCallback(
    async (songs: Song[], liked: boolean) => {
      const changed = songs.filter((s) => (overrides.get(s.id) ?? s.liked) !== liked);
      if (changed.length === 0) return;
      const previous = new Map(changed.map((s) => [s.id, overrides.get(s.id) ?? s.liked]));
      setOverrides((prev) => {
        const next = new Map(prev);
        changed.forEach((s) => next.set(s.id, liked));
        return next;
      });
      try {
        await api.likeBatch(changed.map((s) => s.id), liked);
        setVersion((v) => v + 1);
      } catch {
        setOverrides((prev) => {
          const next = new Map(prev);
          previous.forEach((was, id) => next.set(id, was));
          return next;
        });
      }
    },
    [overrides],
  );

  const value = useMemo(
    () => ({ isLiked, toggleLike, setLikedMany, version }),
    [isLiked, toggleLike, setLikedMany, version],
  );
  return <LikesContext.Provider value={value}>{children}</LikesContext.Provider>;
}

export function useLikes(): LikesApi {
  const ctx = useContext(LikesContext);
  if (!ctx) throw new Error('useLikes must be used inside <LikesProvider>');
  return ctx;
}
