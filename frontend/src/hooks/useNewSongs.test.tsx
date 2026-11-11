import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mockFetch, song } from '../test/fakes';
import { useNewSongs } from './useNewSongs';

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  listeners = new Map<string, (e: MessageEvent) => void>();
  closed = false;
  constructor(public url: string) {
    FakeEventSource.instances.push(this);
  }
  addEventListener(type: string, fn: (e: MessageEvent) => void) {
    this.listeners.set(type, fn);
  }
  close() {
    this.closed = true;
  }
  emit(type: string, data: unknown) {
    this.listeners.get(type)?.(new MessageEvent(type, { data: JSON.stringify(data) }));
  }
}

beforeEach(() => {
  FakeEventSource.instances = [];
  vi.stubGlobal('EventSource', FakeEventSource);
});
afterEach(() => vi.unstubAllGlobals());

describe('useNewSongs', () => {
  it('loads new songs over REST', async () => {
    mockFetch({ 'GET /api/songs/new': [song(1, 'Tum Hi Ho')] });

    const { result } = renderHook(() => useNewSongs());

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.songs.map((s) => s.title)).toEqual(['Tum Hi Ho']);
  });

  it('prepends songs pushed live from the server and marks them fresh', async () => {
    mockFetch({ 'GET /api/songs/new': [song(1, 'Tum Hi Ho')] });
    const { result, unmount } = renderHook(() => useNewSongs());
    await waitFor(() => expect(result.current.loading).toBe(false));
    const es = FakeEventSource.instances[0];
    expect(es.url).toBe('/api/songs/stream');

    act(() => es.onopen?.());
    act(() => es.emit('song-added', song(7, 'Chaleya')));
    act(() => es.emit('song-added', song(7, 'Chaleya'))); // duplicate ignored

    expect(result.current.live).toBe(true);
    expect(result.current.songs.map((s) => s.title)).toEqual(['Chaleya', 'Tum Hi Ho']);
    expect(result.current.freshIds.has(7)).toBe(true);

    unmount();
    expect(es.closed).toBe(true);
  });

  it('polls the REST API periodically', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const fetchMock = mockFetch({ 'GET /api/songs/new': [] });
    renderHook(() => useNewSongs(1000));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));

    await act(async () => {
      vi.advanceTimersByTime(1000);
    });

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    vi.useRealTimers();
  });

  it('exposes an error when the API is unreachable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('offline')));

    const { result } = renderHook(() => useNewSongs());

    await waitFor(() => expect(result.current.error).toBe('Could not load new songs'));
  });
});
