import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DownloadsProvider } from '../hooks/DownloadsContext';
import { LikesProvider } from '../hooks/LikesContext';
import { PlayerProvider } from '../player/PlayerContext';
import { FakeAudio, mockFetch, song } from '../test/fakes';
import { FolderView } from './FolderView';

afterEach(() => vi.unstubAllGlobals());

const brahmastra = [
  song(1, 'Kesariya', { movie: 'Brahmastra' }),
  song(2, 'Deva Deva', { movie: 'Brahmastra', liked: true }),
];

function renderFolder() {
  const audio = new FakeAudio();
  render(
    <LikesProvider>
      <DownloadsProvider>
        <PlayerProvider audioFactory={() => audio.asElement()}>
          <FolderView filter="movie" folderKey="Brahmastra" />
        </PlayerProvider>
      </DownloadsProvider>
    </LikesProvider>,
  );
  return audio;
}

describe('FolderView', () => {
  it('lists the folder songs with like and download buttons on each', async () => {
    const fetchMock = mockFetch({
      'GET /api/songs': { content: brahmastra, page: 0, size: 100, totalElements: 2, totalPages: 1 },
    });
    renderFolder();

    expect(await screen.findByRole('heading', { name: 'Brahmastra' })).toBeInTheDocument();
    await screen.findByText('2 songs');
    expect(String(fetchMock.mock.calls[0][0])).toBe('/api/songs?page=0&size=100&movie=Brahmastra');
    const row = screen.getByRole('button', { name: 'Play Kesariya by Arijit Singh' });
    expect(within(row).getByRole('button', { name: 'Like Kesariya' })).toBeInTheDocument();
    expect(within(row).getByRole('button', { name: 'Download Kesariya' })).toBeInTheDocument();
  });

  it('likes the whole folder, then unlikes it', async () => {
    const fetchMock = mockFetch({
      'GET /api/songs': { content: brahmastra, page: 0, size: 100, totalElements: 2, totalPages: 1 },
    });
    renderFolder();

    await userEvent.click(await screen.findByRole('button', { name: 'Like all (1)' }));
    expect(screen.getByRole('button', { name: 'Unlike all' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Unlike Kesariya' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Unlike all' }));
    expect(screen.getByRole('button', { name: 'Like all (2)' })).toBeInTheDocument();

    const batches = fetchMock.mock.calls.filter(([url]) => url === '/api/likes/batch').map(([, i]) => i!.body);
    expect(batches).toEqual(['{"songIds":[1],"liked":true}', '{"songIds":[1,2],"liked":false}']);
  });

  it('downloads the whole folder', async () => {
    const fetchMock = mockFetch({
      'GET /api/songs': { content: brahmastra, page: 0, size: 100, totalElements: 2, totalPages: 1 },
      'POST /api/downloads': [
        { songId: 1, downloadStatus: 'DOWNLOADED' },
        { songId: 2, downloadStatus: 'DOWNLOADED' },
      ],
    });
    renderFolder();

    await userEvent.click(await screen.findByRole('button', { name: 'Download all (2)' }));

    expect(fetchMock).toHaveBeenCalledWith('/api/downloads', expect.objectContaining({ body: '{"songIds":[1,2]}' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'All downloaded' })).toBeDisabled());
  });

  it('plays the whole folder in order', async () => {
    mockFetch({
      'GET /api/songs': { content: brahmastra, page: 0, size: 100, totalElements: 2, totalPages: 1 },
      'POST /api/history': { id: 1 },
    });
    const audio = renderFolder();

    await userEvent.click(await screen.findByRole('button', { name: 'Play all' }));

    expect(audio.src).toBe('https://stream.test/1');
  });
});
