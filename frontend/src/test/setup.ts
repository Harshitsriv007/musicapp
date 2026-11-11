import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach, vi } from 'vitest';

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

// jsdom has no media playback.
Object.defineProperty(HTMLMediaElement.prototype, 'play', { configurable: true, value: vi.fn(() => Promise.resolve()) });
Object.defineProperty(HTMLMediaElement.prototype, 'pause', { configurable: true, value: vi.fn() });

// Media Session API (lock screen / notification controls)
class MediaMetadataMock {
  title: string; artist: string; album: string; artwork: MediaImage[];
  constructor(init: MediaMetadataInit = {}) {
    this.title = init.title ?? '';
    this.artist = init.artist ?? '';
    this.album = init.album ?? '';
    this.artwork = [...(init.artwork ?? [])];
  }
}
(globalThis as unknown as { MediaMetadata: unknown }).MediaMetadata = MediaMetadataMock;

export const mediaSessionHandlers = new Map<string, MediaSessionActionHandler | null>();
Object.defineProperty(navigator, 'mediaSession', {
  configurable: true,
  value: {
    metadata: null as MediaMetadata | null,
    playbackState: 'none' as MediaSessionPlaybackState,
    setActionHandler: (action: string, handler: MediaSessionActionHandler | null) => mediaSessionHandlers.set(action, handler),
    setPositionState: vi.fn(),
  },
});
