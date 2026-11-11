import type { Song } from '../api/types';

export interface MediaSessionHandlers {
  play: () => void;
  pause: () => void;
  stop: () => void;
  next: () => void;
  previous: () => void;
  seekTo: (seconds: number) => void;
}

const supported = () => typeof navigator !== 'undefined' && 'mediaSession' in navigator;

/**
 * The Media Session API is what makes the app usable while minimised: it powers the
 * lock-screen / notification player on Android & iOS, the media hub on desktop Chrome/Edge,
 * macOS Now Playing, and hardware media keys – showing title, artist and artwork with
 * play / pause / next / previous / stop buttons.
 */
export function updateMetadata(song: Song | null): void {
  if (!supported()) return;
  if (!song) {
    navigator.mediaSession.metadata = null;
    return;
  }
  const artwork = song.imageUrl
    ? [
        { src: song.imageUrl, sizes: '480x480', type: 'image/jpeg' },
        { src: song.imageUrl, sizes: '512x512', type: 'image/jpeg' },
      ]
    : [{ src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' }];
  navigator.mediaSession.metadata = new MediaMetadata({
    title: song.title,
    artist: song.artist ?? '',
    album: song.album ?? 'Dhun',
    artwork,
  });
}

export function updatePlaybackState(state: MediaSessionPlaybackState): void {
  if (supported()) navigator.mediaSession.playbackState = state;
}

export function updatePosition(duration: number, position: number): void {
  if (!supported() || !navigator.mediaSession.setPositionState) return;
  if (!Number.isFinite(duration) || duration <= 0) return;
  try {
    navigator.mediaSession.setPositionState({ duration, position: Math.min(position, duration), playbackRate: 1 });
  } catch {
    // some browsers throw on transient invalid states – safe to ignore
  }
}

export function registerHandlers(h: MediaSessionHandlers): () => void {
  if (!supported()) return () => {};
  const entries: [MediaSessionAction, MediaSessionActionHandler][] = [
    ['play', () => h.play()],
    ['pause', () => h.pause()],
    ['stop', () => h.stop()],
    ['nexttrack', () => h.next()],
    ['previoustrack', () => h.previous()],
    ['seekto', (d) => d.seekTime != null && h.seekTo(d.seekTime)],
  ];
  for (const [action, handler] of entries) {
    try {
      navigator.mediaSession.setActionHandler(action, handler);
    } catch {
      // action not supported on this platform
    }
  }
  return () => {
    for (const [action] of entries) {
      try {
        navigator.mediaSession.setActionHandler(action, null);
      } catch {
        /* ignore */
      }
    }
  };
}
