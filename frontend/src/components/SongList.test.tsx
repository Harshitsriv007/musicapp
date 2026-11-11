import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DownloadsProvider } from '../hooks/DownloadsContext';
import { LikesProvider } from '../hooks/LikesContext';
import { PlayerProvider } from '../player/PlayerContext';
import { FakeAudio, mockFetch, song } from '../test/fakes';
import { SongList } from './SongList';

afterEach(() => vi.unstubAllGlobals());

function renderList(songs = [song(1, 'Tum Hi Ho'), song(2, 'Kesariya', { liked: true })]) {
  const audio = new FakeAudio();
  render(
    <LikesProvider>
      <DownloadsProvider>
      <PlayerProvider audioFactory={() => audio.asElement()}>
        <SongList songs={songs} />
      </PlayerProvider>
      </DownloadsProvider>
    </LikesProvider>,
  );
  return audio;
}

describe('SongList', () => {
  it('plays the clicked song', async () => {
    mockFetch({ 'POST /api/history': { id: 1 } });
    const audio = renderList();

    await userEvent.click(screen.getByRole('button', { name: 'Play Kesariya by Arijit Singh' }));

    expect(audio.src).toBe('https://stream.test/2');
    expect(screen.getByRole('button', { name: 'Play Kesariya by Arijit Singh' })).toHaveAttribute('aria-current', 'true');
  });

  it('likes and unlikes with the heart button', async () => {
    const fetchMock = mockFetch();
    renderList();

    await userEvent.click(screen.getByRole('button', { name: 'Like Tum Hi Ho' }));
    expect(screen.getByRole('button', { name: 'Unlike Tum Hi Ho' })).toHaveAttribute('aria-pressed', 'true');

    await userEvent.click(screen.getByRole('button', { name: 'Unlike Kesariya' }));
    expect(screen.getByRole('button', { name: 'Like Kesariya' })).toBeInTheDocument();

    expect(fetchMock.mock.calls.map(([u, i]) => `${i!.method} ${u}`)).toEqual(['PUT /api/likes/1', 'DELETE /api/likes/2']);
  });

  it('rolls back the heart if the server rejects the like', async () => {
    mockFetch({ 'PUT /api/likes/1': new Response(null, { status: 500 }) });
    renderList();

    await userEvent.click(screen.getByRole('button', { name: 'Like Tum Hi Ho' }));

    await waitFor(() => expect(screen.getByRole('button', { name: 'Like Tum Hi Ho' })).toHaveAttribute('aria-pressed', 'false'));
  });

  it('shows an empty state', () => {
    renderList([]);
    expect(screen.getByText('No songs yet.')).toBeInTheDocument();
  });
});
