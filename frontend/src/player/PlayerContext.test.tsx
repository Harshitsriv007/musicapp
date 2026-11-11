import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { FakeAudio, mockFetch, song } from '../test/fakes';
import { mediaSessionHandlers } from '../test/setup';
import { PlayerProvider, usePlayer } from './PlayerContext';

const queue = [song(1, 'Tum Hi Ho'), song(2, 'Kesariya'), song(3, 'Chaleya')];

function setup(onPlayRecorded = vi.fn()) {
  const audio = new FakeAudio();
  const wrapper = ({ children }: { children: ReactNode }) => (
    <PlayerProvider audioFactory={() => audio.asElement()} onPlayRecorded={onPlayRecorded}>
      {children}
    </PlayerProvider>
  );
  const hook = renderHook(() => usePlayer(), { wrapper });
  return { audio, ...hook };
}

let fetchMock: ReturnType<typeof mockFetch>;
beforeEach(() => {
  fetchMock = mockFetch({ 'POST /api/history': { id: 1 } });
});
afterEach(() => vi.unstubAllGlobals());

describe('PlayerProvider', () => {
  it('plays a song, records it in history and exposes it as current', async () => {
    const onRecorded = vi.fn();
    const { result, audio } = setup(onRecorded);

    act(() => result.current.playSong(queue[1], queue));

    expect(result.current.current?.title).toBe('Kesariya');
    expect(result.current.isPlaying).toBe(true);
    expect(audio.src).toBe('https://stream.test/2');
    expect(result.current.hasNext).toBe(true);
    expect(result.current.hasPrevious).toBe(true);
    await waitFor(() => expect(onRecorded).toHaveBeenCalled());
    expect(fetchMock).toHaveBeenCalledWith('/api/history', expect.objectContaining({ method: 'POST', body: '{"songId":2}' }));
  });

  it('toggles pause / play', () => {
    const { result, audio } = setup();
    act(() => result.current.playSong(queue[0], queue));

    act(() => result.current.togglePlay());
    expect(audio.pause).toHaveBeenCalled();
    expect(result.current.isPlaying).toBe(false);

    act(() => result.current.togglePlay());
    expect(result.current.isPlaying).toBe(true);
  });

  it('next and previous move through the queue', () => {
    const { result } = setup();
    act(() => result.current.playSong(queue[0], queue));

    act(() => result.current.next());
    expect(result.current.current?.title).toBe('Kesariya');

    act(() => result.current.previous());
    expect(result.current.current?.title).toBe('Tum Hi Ho');
  });

  it('previous restarts the song when more than 3 seconds in', () => {
    const { result, audio } = setup();
    act(() => result.current.playSong(queue[1], queue));
    audio.currentTime = 42;

    act(() => result.current.previous());

    expect(result.current.current?.title).toBe('Kesariya');
    expect(audio.currentTime).toBe(0);
  });

  it('stop pauses and rewinds but keeps the song loaded', () => {
    const { result, audio } = setup();
    act(() => result.current.playSong(queue[0], queue));
    audio.currentTime = 30;

    act(() => result.current.stop());

    expect(result.current.isPlaying).toBe(false);
    expect(audio.currentTime).toBe(0);
    expect(result.current.current?.title).toBe('Tum Hi Ho');
    expect(navigator.mediaSession.playbackState).toBe('none');
  });

  it('auto-advances when a song ends and stops at the end of the queue', () => {
    const { result, audio } = setup();
    act(() => result.current.playSong(queue[1], queue));

    act(() => audio.finish());
    expect(result.current.current?.title).toBe('Chaleya');

    act(() => audio.finish());
    expect(result.current.current?.title).toBe('Chaleya');
    expect(result.current.isPlaying).toBe(false);
  });

  it('publishes metadata to the OS media controls (lock screen / notification)', () => {
    const { result } = setup();

    act(() => result.current.playSong(queue[2], queue));

    const meta = navigator.mediaSession.metadata!;
    expect(meta.title).toBe('Chaleya');
    expect(meta.artist).toBe('Arijit Singh');
    expect(meta.artwork[0].src).toBe('https://img.test/3.jpg');
    expect(navigator.mediaSession.playbackState).toBe('playing');
  });

  it('responds to OS media buttons: next, previous, pause, stop', () => {
    const { result, audio } = setup();
    act(() => result.current.playSong(queue[0], queue));

    act(() => mediaSessionHandlers.get('nexttrack')!({ action: 'nexttrack' }));
    expect(result.current.current?.title).toBe('Kesariya');

    act(() => mediaSessionHandlers.get('previoustrack')!({ action: 'previoustrack' }));
    expect(result.current.current?.title).toBe('Tum Hi Ho');

    act(() => mediaSessionHandlers.get('pause')!({ action: 'pause' }));
    expect(audio.pause).toHaveBeenCalled();
    expect(result.current.isPlaying).toBe(false);

    act(() => mediaSessionHandlers.get('play')!({ action: 'play' }));
    expect(result.current.isPlaying).toBe(true);

    act(() => mediaSessionHandlers.get('stop')!({ action: 'stop' }));
    expect(result.current.isPlaying).toBe(false);
  });

  it('reports playback errors', () => {
    const { result, audio } = setup();
    act(() => result.current.playSong(queue[0], queue));

    act(() => {
      audio.dispatchEvent(new Event('error'));
    });

    expect(result.current.error).toBe('Could not play this song');
    expect(result.current.isPlaying).toBe(false);
  });
});
