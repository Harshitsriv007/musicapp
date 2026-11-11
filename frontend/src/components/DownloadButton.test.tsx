import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Song } from '../api/types';
import { DownloadsProvider } from '../hooks/DownloadsContext';
import { mockFetch, song } from '../test/fakes';
import { DownloadAllButton, DownloadButton } from './DownloadButton';

class FakeEventSource {
  static last: FakeEventSource | null = null;
  listeners = new Map<string, (e: MessageEvent) => void>();
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  constructor(public url: string) {
    FakeEventSource.last = this;
  }
  addEventListener(type: string, fn: (e: MessageEvent) => void) {
    this.listeners.set(type, fn);
  }
  close() {}
  emit(type: string, data: unknown) {
    this.listeners.get(type)?.(new MessageEvent(type, { data: JSON.stringify(data) }));
  }
}

beforeEach(() => vi.stubGlobal('EventSource', FakeEventSource));
afterEach(() => vi.unstubAllGlobals());

const renderWith = (ui: React.ReactNode) => render(<DownloadsProvider>{ui}</DownloadsProvider>);

describe('DownloadButton', () => {
  it('queues a download and follows live progress until downloaded', async () => {
    const fetchMock = mockFetch({ 'POST /api/downloads': [{ songId: 1, downloadStatus: 'PENDING' }] });
    const kesariya = song(1, 'Kesariya');
    renderWith(<DownloadButton song={kesariya} />);

    await userEvent.click(screen.getByRole('button', { name: 'Download Kesariya' }));

    expect(fetchMock).toHaveBeenCalledWith('/api/downloads', expect.objectContaining({ body: '{"songIds":[1]}' }));
    expect(screen.getByRole('status', { name: 'Downloading Kesariya' })).toBeInTheDocument();

    act(() => FakeEventSource.last!.emit('song-updated', { ...kesariya, downloadStatus: 'DOWNLOADED' }));

    expect(screen.getByRole('button', { name: 'Remove download of Kesariya' })).toBeInTheDocument();
  });

  it('removes a stored download', async () => {
    const fetchMock = mockFetch();
    renderWith(<DownloadButton song={song(2, 'Chaleya', { downloadStatus: 'DOWNLOADED' })} />);

    await userEvent.click(screen.getByRole('button', { name: 'Remove download of Chaleya' }));

    expect(fetchMock).toHaveBeenCalledWith('/api/songs/2/download', expect.objectContaining({ method: 'DELETE' }));
    expect(screen.getByRole('button', { name: 'Download Chaleya' })).toBeInTheDocument();
  });

  it('offers a retry after a failed download and rolls back on API errors', async () => {
    mockFetch({ 'POST /api/downloads': new Response(null, { status: 500 }) });
    renderWith(<DownloadButton song={song(3, 'Heeriye', { downloadStatus: 'FAILED' })} />);

    await userEvent.click(screen.getByRole('button', { name: 'Retry download of Heeriye' }));

    await waitFor(() => expect(screen.getByRole('button', { name: 'Retry download of Heeriye' })).toBeInTheDocument());
  });
});

describe('DownloadAllButton', () => {
  it('downloads only songs that are not stored yet', async () => {
    const fetchMock = mockFetch({
      'POST /api/downloads': [
        { songId: 1, downloadStatus: 'PENDING' },
        { songId: 3, downloadStatus: 'PENDING' },
      ],
    });
    const songs: Song[] = [song(1, 'A'), song(2, 'B', { downloadStatus: 'DOWNLOADED' }), song(3, 'C', { downloadStatus: 'FAILED' })];
    renderWith(<DownloadAllButton songs={songs} />);

    await userEvent.click(screen.getByRole('button', { name: 'Download all (2)' }));

    expect(fetchMock.mock.calls[0][1]!.body).toBe('{"songIds":[1,3]}');
    expect(screen.getByRole('button', { name: 'Downloading…' })).toBeDisabled();
  });

  it('says when everything is already downloaded', () => {
    renderWith(<DownloadAllButton songs={[song(2, 'B', { downloadStatus: 'DOWNLOADED' })]} />);
    expect(screen.getByRole('button', { name: 'All downloaded' })).toBeDisabled();
  });
});
