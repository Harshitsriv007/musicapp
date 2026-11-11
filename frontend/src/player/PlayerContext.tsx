import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { api } from '../api/client';
import type { Song } from '../api/types';
import { registerHandlers, updateMetadata, updatePlaybackState, updatePosition } from './mediaSession';

export interface PlayerApi {
  current: Song | null;
  queue: Song[];
  index: number;
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  error: string | null;
  playSong: (song: Song, queue?: Song[]) => void;
  togglePlay: () => void;
  play: () => void;
  pause: () => void;
  next: () => void;
  previous: () => void;
  stop: () => void;
  seek: (seconds: number) => void;
  hasNext: boolean;
  hasPrevious: boolean;
}

const PlayerContext = createContext<PlayerApi | null>(null);

/** Restart the current song instead of going back if we are past this many seconds. */
const RESTART_THRESHOLD = 3;

interface Props {
  children: ReactNode;
  /** Called after a play has been recorded in the history (used to refresh the History tab). */
  onPlayRecorded?: () => void;
  /** Injected in tests. */
  audioFactory?: () => HTMLAudioElement;
}

/**
 * A single <audio> element lives for the whole app lifetime, outside React's render tree,
 * so playback continues when the user switches tabs, minimises the window, locks the phone
 * or pops the mini player out into its own window.
 */
export function PlayerProvider({ children, onPlayRecorded, audioFactory }: Props) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  if (audioRef.current === null) {
    const audio = audioFactory ? audioFactory() : new Audio();
    audio.preload = 'auto';
    audioRef.current = audio;
  }
  const audio = audioRef.current;

  const [queue, setQueue] = useState<Song[]>([]);
  const [index, setIndex] = useState(-1);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const current = index >= 0 && index < queue.length ? queue[index] : null;

  const onPlayRecordedRef = useRef(onPlayRecorded);
  onPlayRecordedRef.current = onPlayRecorded;

  const load = useCallback(
    (newQueue: Song[], newIndex: number) => {
      const song = newQueue[newIndex];
      if (!song) return;
      setQueue(newQueue);
      setIndex(newIndex);
      setError(null);
      setCurrentTime(0);
      setDuration(song.durationSeconds ?? 0);
      audio.src = song.streamUrl;
      audio.currentTime = 0;
      const started = audio.play();
      if (started && typeof started.catch === 'function') {
        started.catch((e: unknown) => {
          if (e instanceof DOMException && e.name === 'AbortError') return; // superseded by another song
          setError('Could not play this song');
          setIsPlaying(false);
        });
      }
      api
        .recordPlay(song.id)
        .then(() => onPlayRecordedRef.current?.())
        .catch(() => {
          /* history is best effort */
        });
    },
    [audio],
  );

  const playSong = useCallback(
    (song: Song, newQueue?: Song[]) => {
      const q = newQueue && newQueue.length ? newQueue : [song];
      const i = Math.max(0, q.findIndex((s) => s.id === song.id));
      load(q, i);
    },
    [load],
  );

  const play = useCallback(() => {
    if (!current) return;
    void audio.play()?.catch?.(() => setIsPlaying(false));
  }, [audio, current]);

  const pause = useCallback(() => audio.pause(), [audio]);

  const togglePlay = useCallback(() => (audio.paused ? play() : pause()), [audio, play, pause]);

  const next = useCallback(() => {
    if (index + 1 < queue.length) load(queue, index + 1);
  }, [index, queue, load]);

  const previous = useCallback(() => {
    if (audio.currentTime > RESTART_THRESHOLD || index <= 0) {
      audio.currentTime = 0;
      setCurrentTime(0);
      return;
    }
    load(queue, index - 1);
  }, [audio, index, queue, load]);

  const stop = useCallback(() => {
    audio.pause();
    audio.currentTime = 0;
    setCurrentTime(0);
    setIsPlaying(false);
    updatePlaybackState('none');
  }, [audio]);

  const seek = useCallback(
    (seconds: number) => {
      audio.currentTime = seconds;
      setCurrentTime(seconds);
    },
    [audio],
  );

  // Keep media-session handlers pointing at the latest callbacks.
  const handlersRef = useRef({ play, pause, stop, next, previous, seekTo: seek });
  handlersRef.current = { play, pause, stop, next, previous, seekTo: seek };

  useEffect(
    () =>
      registerHandlers({
        play: () => handlersRef.current.play(),
        pause: () => handlersRef.current.pause(),
        stop: () => handlersRef.current.stop(),
        next: () => handlersRef.current.next(),
        previous: () => handlersRef.current.previous(),
        seekTo: (s) => handlersRef.current.seekTo(s),
      }),
    [],
  );

  useEffect(() => updateMetadata(current), [current]);

  // Wire audio element events once.
  const nextRef = useRef(next);
  nextRef.current = next;
  const hasNextRef = useRef(false);
  hasNextRef.current = index + 1 < queue.length;

  useEffect(() => {
    const onPlay = () => {
      setIsPlaying(true);
      updatePlaybackState('playing');
    };
    const onPause = () => {
      setIsPlaying(false);
      updatePlaybackState('paused');
    };
    const onTime = () => {
      setCurrentTime(audio.currentTime);
      updatePosition(audio.duration, audio.currentTime);
    };
    const onMeta = () => {
      if (Number.isFinite(audio.duration)) setDuration(audio.duration);
    };
    const onEnded = () => {
      if (hasNextRef.current) nextRef.current();
      else {
        setIsPlaying(false);
        updatePlaybackState('none');
      }
    };
    const onError = () => {
      setError('Could not play this song');
      setIsPlaying(false);
    };
    audio.addEventListener('play', onPlay);
    audio.addEventListener('pause', onPause);
    audio.addEventListener('timeupdate', onTime);
    audio.addEventListener('loadedmetadata', onMeta);
    audio.addEventListener('ended', onEnded);
    audio.addEventListener('error', onError);
    return () => {
      audio.removeEventListener('play', onPlay);
      audio.removeEventListener('pause', onPause);
      audio.removeEventListener('timeupdate', onTime);
      audio.removeEventListener('loadedmetadata', onMeta);
      audio.removeEventListener('ended', onEnded);
      audio.removeEventListener('error', onError);
    };
  }, [audio]);

  const value = useMemo<PlayerApi>(
    () => ({
      current,
      queue,
      index,
      isPlaying,
      currentTime,
      duration,
      error,
      playSong,
      togglePlay,
      play,
      pause,
      next,
      previous,
      stop,
      seek,
      hasNext: index + 1 < queue.length,
      hasPrevious: index > 0,
    }),
    [current, queue, index, isPlaying, currentTime, duration, error, playSong, togglePlay, play, pause, next, previous, stop, seek],
  );

  return <PlayerContext.Provider value={value}>{children}</PlayerContext.Provider>;
}

export function usePlayer(): PlayerApi {
  const ctx = useContext(PlayerContext);
  if (!ctx) throw new Error('usePlayer must be used inside <PlayerProvider>');
  return ctx;
}
