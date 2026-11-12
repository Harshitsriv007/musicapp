import { vi } from 'vitest';
import type { Song } from '../api/types';

/** Deterministic stand-in for HTMLAudioElement. */
export class FakeAudio extends EventTarget {
  src = '';
  preload = '';
  currentTime = 0;
  duration = NaN;
  paused = true;
  play = vi.fn(() => {
    this.paused = false;
    this.dispatchEvent(new Event('play'));
    return Promise.resolve();
  });
  pause = vi.fn(() => {
    if (this.paused) return;
    this.paused = true;
    this.dispatchEvent(new Event('pause'));
  });
  finish() {
    this.paused = true;
    this.dispatchEvent(new Event('ended'));
  }
  asElement() {
    return this as unknown as HTMLAudioElement;
  }
}

export function song(id: number, title: string, extra: Partial<Song> = {}): Song {
  return {
    id,
    title,
    artist: 'Arijit Singh',
    album: null,
    movie: null,
    genre: 'Bollywood',
    imageUrl: `https://img.test/${id}.jpg`,
    streamUrl: `https://stream.test/${id}`,
    durationSeconds: 200,
    releaseDate: '2024-01-01',
    originalReleaseDate: null,
    source: 'audius',
    addedAt: null,
    liked: false,
    downloadStatus: 'NONE',
    ...extra,
  };
}

/** Routes fetch calls by "METHOD path" → response body. */
export function mockFetch(routes: Record<string, unknown> = {}) {
  const fn = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input), 'http://localhost');
    const key = `${init?.method ?? 'GET'} ${url.pathname}`;
    const body = key in routes ? routes[key] : undefined;
    if (body instanceof Response) return body;
    const status =
      init?.method === 'POST' && url.pathname === '/api/history' ? 201
      : init?.method === 'POST' && url.pathname === '/api/downloads' ? 202
      : body === undefined ? 204 : 200;
    return new Response(body === undefined ? null : JSON.stringify(body), { status });
  });
  vi.stubGlobal('fetch', fn);
  return fn;
}
